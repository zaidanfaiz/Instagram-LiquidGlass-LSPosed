package dev.zaidan.igglass;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.WeakHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Instagram Liquid Glass 0.1-dev: clean implementation for IG 452 / Android 16.
 *
 * 1) Moves native clickable tab children without replacing Instagram listeners.
 * 2) Reclaims the confirmed 180px reserved tab height (v0.8 device-tested).
 * 3) Renders a low-overhead translucent glass with a separate Search circle.
 * 4) Hides overlays for Story/profile detail screens or hidden original tabs.
 * 5) Writes lightweight diagnostics to Download/ via MediaStore asynchronously.
 *
 * No frame-by-frame Canvas captures, bitmap screenshot loops or access to
 * private account data. Unknown view trees are never removed indiscriminately.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String TARGET = "com.instagram.android";
    private static final WeakHashMap<Activity, Controller> CONTROLLERS =
            new WeakHashMap<>();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!TARGET.equals(load.packageName)) return;

        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        Activity activity = (Activity) p.thisObject;
                        if (!isMain(activity)) return;
                        DevFileLog.start(activity);
                        Controller c = CONTROLLERS.get(activity);
                        if (c == null) {
                            c = new Controller(activity);
                            CONTROLLERS.put(activity, c);
                        }
                        c.onResume();
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onPause",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        Controller c = CONTROLLERS.get((Activity) p.thisObject);
                        if (c != null) c.onPause();
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        Controller c = CONTROLLERS.remove((Activity) p.thisObject);
                        if (c != null) c.stop();
                    }
                });

        // Observe UP only; do not override/consume dispatch, native gesture
        // processing or original Instagram click listeners.
        XposedHelpers.findAndHookMethod(Activity.class, "dispatchTouchEvent",
                MotionEvent.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        MotionEvent ev = (MotionEvent) p.args[0];
                        if (ev == null || ev.getActionMasked() != MotionEvent.ACTION_UP) {
                            return;
                        }
                        Controller c = CONTROLLERS.get((Activity) p.thisObject);
                        if (c != null) c.onTouch(ev);
                    }
                });
    }

    private static boolean isMain(Activity activity) {
        return TARGET.equals(activity.getPackageName())
                && activity.getClass().getName().endsWith(".InstagramMainActivity");
    }

    private static final class Controller {
        private final Activity activity;
        private final View decor;
        private DevGlassUi.Scene scene;
        private DevEdgeSpaceFix.Session edgeSpace;
        private DevGlassBackdrop frost;
        private boolean running;
        private boolean resumed;
        private long lastMissingLog;
        private long lastFragmentLog;
        private long lastSafeViewportCheck;
        private String lastReason = "initial";

        private final Runnable poll = new Runnable() {
            @Override public void run() {
                if (!running || activity.isDestroyed() || activity.isFinishing()) {
                    stop();
                    return;
                }
                try {
                    if (resumed) {
                        if (scene == null) install();
                        else if (!scene.attached()) {
                            DevFileLog.note("native nav tree changed; attempting safe rebind");
                            if (frost != null) frost.stop();
                            frost = null;
                            if (edgeSpace != null) edgeSpace.restore();
                            edgeSpace = null;
                            scene.removeAndRestore();
                            scene = null;
                            install();
                        }
                        sync();
                    }
                } catch (Throwable e) {
                    DevFileLog.note("poll error " + e.getClass().getSimpleName());
                }
                // Reels video should not be traversed/reclassified 2x per
                // second; touch-up also queues a faster route refresh (240ms)
                // for opening or dismissing the comment sheet.
                if (running) decor.postDelayed(this,
                        scene != null && scene.isReelsActive() ? 850 : 700);
            }
        };

        Controller(Activity activity) {
            this.activity = activity;
            this.decor = activity.getWindow().getDecorView();
        }

        void onResume() {
            resumed = true;
            if (!running) {
                running = true;
                DevFileLog.note("main activity resumed; searching for verified tab_bar");
                decor.postDelayed(poll, 350);
            }
            decor.postDelayed(this::installIfNeeded, 200);
            decor.postDelayed(this::installIfNeeded, 1000);
            decor.postDelayed(this::installIfNeeded, 2200);
            decor.postDelayed(this::sync, 280);
            if (frost != null) frost.request(680);
        }

        void onPause() {
            resumed = false;
            if (frost != null) frost.setEnabled(false);
            if (scene != null) scene.show(false);
            // Do NOT restore the 60px root margin here. Opening a modal
            // comment sheet, another Window or an input UI can briefly pause
            // the main Activity while the same Reel Surface remains active.
            // Root relayout in that moment can orphan/freeze SurfaceView
            // visuals even when the audio decoder keeps playing.
            DevFileLog.note("main activity paused: glass hidden; video root geometry preserved");
        }

        void onTouch(MotionEvent ev) {
            if (!resumed || scene == null || !scene.attached()) return;
            // dispatchTouchEvent sees ALL Activity touches, including taps
            // inside a foreground comment sheet. Never interpret a modal
            // comment tap as a click on the visually-overlapped native nav.
            if (DevScreenDetector.hiddenBecause(activity, scene.nav,
                    scene.sourceParent) != null) {
                scene.show(false);
                if (frost != null) frost.setEnabled(false);
                decor.postDelayed(this::sync, 240);
                return;
            }
            scene.onTouch(ev);
            if (frost != null) {
                frost.setEnabled(!scene.isReelsActive()
                        && scene.mainPill.getVisibility()==View.VISIBLE);
                frost.request(1000);
            }
            decor.postDelayed(this::sync, 240);
        }

        void stop() {
            if (!running) return;
            running = false;
            resumed = false;
            decor.removeCallbacks(poll);
            if (frost != null) {
                frost.stop();
                frost = null;
            }
            if (edgeSpace != null) {
                edgeSpace.restore();
                edgeSpace = null;
            }
            if (scene != null) {
                scene.removeAndRestore();
                scene = null;
            }
            DevFileLog.note("main activity released");
        }

        private void installIfNeeded() {
            if (resumed && running && scene == null) install();
        }

        private void install() {
            if (!resumed || activity.isDestroyed() || activity.isFinishing()
                    || !(decor instanceof ViewGroup)) return;
            View container = activity.findViewById(android.R.id.content);
            if (!(container instanceof FrameLayout)) return;
            FrameLayout host = (FrameLayout) container;
            ViewGroup nav = findVerifiedTabBar((ViewGroup) decor, 0, new int[] {500});
            if (nav == null) {
                long now = System.currentTimeMillis();
                if (now - lastMissingLog > 10_000) {
                    lastMissingLog = now;
                    DevFileLog.note("verified Instagram 452 tab_bar not visible yet");
                }
                return;
            }
            ViewGroup parent = (ViewGroup) nav.getParent();
            if (!isDescendant(parent, host)) return;
            scene = DevGlassUi.create(activity, host, parent, nav);
            if (scene == null) return;
            edgeSpace = DevEdgeSpaceFix.inspect(activity, parent);
            // Apply the verified root correction ONCE at installation.
            // Never toggle it when a Reel comment overlay gains/loses focus.
            if (edgeSpace != null) edgeSpace.setActive(true);
            reclaimOriginalNavSpace();
            DevFileLog.note("r13 viewport: reserve 180px removed; 60px root guarded; no window/immersive flag overrides");
            DevViewportGuard.note(activity, scene, "install");
            // Diagnostic optical baseline: no captured feed/video bitmap may
            // be painted onto the shell. The r7/r8 screenshots stayed white
            // EVEN when native tab rendering was alpha=0. This decisively
            // isolates the high-risk cached AGSL backdrop from the clear
            // material. Re-enable only after actual on-device clear pass.
            // SAFE non-video frosting only: 16%-opacity cached bitmap,
            // never PixelCopy from Reels, never draw if a Surface/TextureView
            // overlaps the glass. Automatically suspend on modal comments.
            frost = new DevGlassBackdrop(activity, scene);
            frost.start();
            DevFileLog.note("r12: non-video GPU frosted alpha=16%; Reels kept rim-only");
            sync();
        }

        private void sync() {
            if (!running || !resumed || scene == null || !scene.attached()) return;
            String reason = DevScreenDetector.hiddenBecause(activity, scene.nav, scene.sourceParent);
            String state = reason == null ? "visible" : "hidden:" + reason;
            boolean show = reason == null;
            scene.show(show);
            // The blur never samples Reels nor any overlaid comment modal.
            if (frost != null) frost.setEnabled(show && !scene.isReelsActive());
            // Neither setActive(show) nor margin reclamation may run in
            // periodic sync: it forces repeated setLayoutParams() on the
            // live video coordinator, which can freeze SurfaceView pictures
            // after dismissing the comments sheet. Installer owns insets.
            if (show) scene.syncSelection();
            // A new native selection can change to Reels during syncSelection.
            if (frost != null) frost.setEnabled(show && !scene.isReelsActive());

            // IG452 sometimes reinstates its 60px root / 180px native tab
            // reservation after a main-tab transition, leaving a black gap
            // instead of filling the display. r12 stopped *all* reconciliation
            // to fix frozen Reels. r13 repairs only when no live video surface
            // is visible, the user is on a non-Reels main tab and window
            // focus is stable. No layout manipulation while video is playing.
            long uptime = android.os.SystemClock.uptimeMillis();
            if (show && !scene.isReelsActive() && activity.hasWindowFocus()
                    && uptime - lastSafeViewportCheck >= 6500) {
                lastSafeViewportCheck = uptime;
                if (!DevViewportGuard.hasLiveVideo(scene.host)) {
                    if (edgeSpace != null) edgeSpace.setActive(true);
                    reclaimOriginalNavSpace();
                    DevViewportGuard.note(activity, scene, "safe-static-tab");
                } else {
                    DevFileLog.note("r13 viewport: deferred, visible video surface");
                }
            }
            if (!state.equals(lastReason)) {
                if (show && frost != null) frost.request(550);
                lastReason = state;
                scene.auditMaterial(state);
                DevFileLog.note("screen=" + state
                        + "; fragments=" + DevScreenDetector.fragmentNames(activity));
            }
            long now = System.currentTimeMillis();
            if (now - lastFragmentLog > 8000) {
                lastFragmentLog = now;
                DevFileLog.note("screen diagnostic " + state
                        + "; fragments=" + DevScreenDetector.fragmentNames(activity));
            }
        }

        /**
         * Verified real Instagram 452 hierarchy (180px at 1440x3200):
         * layout_container_main_wrapper
         *  layout_container_main           bottomMargin=180
         *  swipeable_tab_view_pager         bottomMargin=180
         *  tab_bar_shadow                   1px
         *  tab_bar                          180px (already moved)
         *
         * Instagram can restore the original margins after navigation, so
         * reconcile only these two known views, and only if their margins
         * equal 0 or the exact native bar height. Unknown layouts are left
         * untouched. This preserves the stable v0.8 fix.
         */
        private void reclaimOriginalNavSpace() {
            if (scene == null || !"layout_container_main_wrapper".equals(
                    DevGlassUi.resourceName(scene.sourceParent))) return;
            ViewGroup parent = scene.sourceParent;
            View main = null, pager = null, shadow = null, bottomSearch = null;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                String id = DevGlassUi.resourceName(child);
                if ("layout_container_main".equals(id)) main = child;
                else if ("swipeable_tab_view_pager".equals(id)) pager = child;
                else if ("tab_bar_shadow".equals(id)) shadow = child;
                else if ("layout_bottom_searchbar".equals(id)) bottomSearch = child;
            }
            if (!validMargin(main, scene.barHeight)
                    || !validMargin(pager, scene.barHeight)) {
                return;
            }
            boolean adjusted = removeReservedMargin(main, scene.barHeight);
            adjusted |= removeReservedMargin(pager, scene.barHeight);
            if (validMargin(bottomSearch, scene.barHeight)) {
                adjusted |= removeReservedMargin(bottomSearch, scene.barHeight);
            }
            if (shadow != null && shadow.getHeight() <= dp(shadow, 3)
                    && shadow.getVisibility() == View.VISIBLE) {
                shadow.setVisibility(View.GONE);
                adjusted = true;
                DevFileLog.note("old tab_bar_shadow hidden");
            }
            if (adjusted) {
                parent.requestLayout();
                DevFileLog.note("fixed reserved bottom margin " + scene.barHeight
                        + "px -> 0 on native feed/pager");
            }
        }

        private static boolean validMargin(View view, int original) {
            if (view == null) return false;
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (!(lp instanceof FrameLayout.LayoutParams)) return false;
            int margin = ((FrameLayout.LayoutParams) lp).bottomMargin;
            return margin == 0 || margin == original;
        }

        private static boolean removeReservedMargin(View view, int original) {
            if (view == null) return false;
            FrameLayout.LayoutParams lp =
                    (FrameLayout.LayoutParams) view.getLayoutParams();
            if (lp.bottomMargin != original) return false;
            lp.bottomMargin = 0;
            view.setLayoutParams(lp);
            return true;
        }

        private static ViewGroup findVerifiedTabBar(ViewGroup root,
                                                     int depth, int[] budget) {
            if (depth > 14 || budget[0] <= 0) return null;
            for (int i = 0; i < root.getChildCount() && budget[0] > 0; i++) {
                View child = root.getChildAt(i);
                budget[0]--;
                if (!(child instanceof ViewGroup)
                        || child.getVisibility() != View.VISIBLE) continue;
                ViewGroup group = (ViewGroup) child;
                if ("tab_bar".equals(DevGlassUi.resourceName(group))
                        && group.getChildCount() == 5
                        && group.getParent() instanceof ViewGroup
                        && "layout_container_main_wrapper".equals(
                                DevGlassUi.resourceName((View) group.getParent()))) {
                    return group;
                }
                ViewGroup nested = findVerifiedTabBar(group, depth + 1, budget);
                if (nested != null) return nested;
            }
            return null;
        }

        private static boolean isDescendant(View child, ViewGroup ancestor) {
            View current = child;
            while (current != null) {
                if (current == ancestor) return true;
                current = current.getParent() instanceof View
                        ? (View) current.getParent() : null;
            }
            return false;
        }

        private static int dp(View v, float value) {
            return Math.round(v.getResources().getDisplayMetrics().density * value);
        }
    }
}
