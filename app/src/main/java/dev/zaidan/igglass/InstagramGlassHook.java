package dev.zaidan.igglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Locale;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v0.9 split Liquid Glass + Story/Profile auto-hide (beta) for Instagram 452.
 *
 * It relocates the ORIGINAL navigation buttons and draws a sampled, blurred
 * approximation of content behind them. This is not the native iOS shader.
 * Instagram may change its private hierarchy in any release.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String PACKAGE = "com.instagram.android";
    private static final WeakHashMap<View, Boolean> STYLED = new WeakHashMap<>();
    private static final int HORIZONTAL_MARGIN_DP = 16;
    private static final int BOTTOM_MARGIN_DP = GlassV9Enhancements.BOTTOM_MARGIN_DP;
    private static final long MAX_FRAME_INTERVAL_NS = 83_000_000L; // up to ~12 fps while scrolling
    private static final long ACTIVE_TAIL_NS = 700_000_000L;
    private static final long IDLE_REFRESH_MS = 550L;
    private static final WeakHashMap<Activity, GlassUpdater> SCENES = new WeakHashMap<>();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!PACKAGE.equals(load.packageName)) return;
        XposedBridge.log("IGGlass v0.9-split-nav: " + load.processName);
        XposedHelpers.findAndHookMethod(Activity.class, "dispatchTouchEvent", MotionEvent.class,
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        GlassUpdater updater = SCENES.get(activity);
                        if (updater != null) {
                            updater.onTouch((MotionEvent) param.args[0]);
                        }
                    }
                });
        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                GlassUpdater updater = SCENES.remove(activity);
                if (updater != null) updater.stop();
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (!PACKAGE.equals(activity.getPackageName())) return;
                        View decor = activity.getWindow().getDecorView();
                        for (int delay : new int[] {300, 1100, 2500}) {
                            decor.postDelayed(() -> {
                                if (activity.isFinishing() || activity.isDestroyed()) return;
                                try { install(activity); }
                                catch (Throwable e) { XposedBridge.log("IGGlass: " + e); }
                            }, delay);
                        }
                    }
                });
    }

    private static void install(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup) || decor.getHeight() == 0) return;
        ViewGroup nav = findCandidate((ViewGroup) decor, decor.getHeight(), 0);
        if (nav == null || STYLED.containsKey(nav)) return;

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)
                || !(nav.getParent() instanceof ViewGroup)) {
            fallback(nav);
            STYLED.put(nav, true);
            return;
        }

        FrameLayout host = (FrameLayout) content;
        ViewGroup parent = (ViewGroup) nav.getParent();
        NavDiagnostics.dump(activity, nav, parent, "before-floating");
        if (!descendsFrom(parent, host) || isInsideCollection(parent)) {
            fallback(nav);
            STYLED.put(nav, true);
            return;
        }
        int height = nav.getHeight();
        if (height < dp(nav, 34) || height > dp(nav, 105)) return;

        int oldIndex = parent.indexOfChild(nav);
        ViewGroup.LayoutParams oldParams = nav.getLayoutParams();
        boolean dark = (nav.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        FrameLayout shell = new FrameLayout(activity);
        shell.setClipChildren(true);
        shell.setClipToPadding(true);
        shell.setElevation(dp(nav, 16));
        shell.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        dp(view, 33));
            }
        });
        shell.setClipToOutline(true);

        ImageView sampledBackdrop = new ImageView(activity);
        sampledBackdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        sampledBackdrop.setClickable(false);
        if (Build.VERSION.SDK_INT >= 31) {
            sampledBackdrop.setRenderEffect(RenderEffect.createBlurEffect(
                    dp(nav, 10), dp(nav, 10), Shader.TileMode.CLAMP));
        }
        shell.addView(sampledBackdrop, new FrameLayout.LayoutParams(-1, -1));

        View frost = new View(activity);
        GradientDrawable tint = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0x6031384A, 0x4A090C18}
                     : new int[] {0x8AFFFFFF, 0x50F6FAFF});
        tint.setCornerRadius(dp(nav, 33));
        frost.setBackground(tint);
        frost.setClickable(false);
        shell.addView(frost, new FrameLayout.LayoutParams(-1, -1));

        View outline = new View(activity);
        GradientDrawable rim = new GradientDrawable();
        rim.setColor(Color.TRANSPARENT);
        rim.setCornerRadius(dp(nav, 33));
        rim.setStroke(dp(nav, 1), dark ? 0xA0FFFFFF : 0xBFFFFFFF);
        outline.setBackground(rim);
        outline.setClickable(false);

        FrameLayout.LayoutParams floatParams = new FrameLayout.LayoutParams(
                -1, height, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        floatParams.leftMargin = dp(nav, HORIZONTAL_MARGIN_DP);
        floatParams.rightMargin = dp(nav, HORIZONTAL_MARGIN_DP);
        floatParams.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);

        GlassV9Enhancements.Setup glass = null;
        try {
            parent.removeView(nav);
            host.addView(shell, floatParams);
            shell.addView(nav, new FrameLayout.LayoutParams(-1, -1));
            // Do not clear icon/badge backgrounds; only remove full-width
            // opaque layers that otherwise obscure the frosted backdrop.
            clearOpaqueFullSizeWrappers(nav, nav.getWidth(), height, 0);
            shell.addView(outline, new FrameLayout.LayoutParams(-1, -1));
            glass = GlassV9Enhancements.attach(activity, host, shell, nav,
                    height, dark);

            // Remove the former bottom bar's *layout slot*, including small
            // non-clickable decorative wrappers. Let the feed resize normally.
            // Never remove a container holding feed, stories, or live controls.
            if (!releaseKnownNavSpace(parent, height)) {
                collapseLegacyNavSlot(parent, host, height);
            }
            host.bringChildToFront(shell);
            STYLED.put(nav, true);
            GlassUpdater old = SCENES.remove(activity);
            if (old != null) old.stop();
            GlassUpdater updater = new GlassUpdater(activity, host, shell,
                    sampledBackdrop, glass);
            SCENES.put(activity, updater);
            updater.start();
            host.postDelayed(() -> NavDiagnostics.dump(activity, nav, parent,
                    "after-floating"), 450);
            XposedBridge.log("IGGlass v0.9-split-nav: floating UI installed");
        } catch (Throwable e) {
            XposedBridge.log("IGGlass: overlay failed, restoring: " + e);
            try {
                if (glass != null) {
                    glass.restoreSearch();
                    if (glass.searchShell != null && glass.searchShell.getParent() == host) {
                        host.removeView(glass.searchShell);
                    }
                }
                if (nav.getParent() instanceof ViewGroup) {
                    ((ViewGroup) nav.getParent()).removeView(nav);
                }
                if (shell.getParent() == host) host.removeView(shell);
                if (nav.getParent() == null) {
                    parent.addView(nav, Math.max(0,
                            Math.min(oldIndex, parent.getChildCount())), oldParams);
                }
            } catch (Throwable restoreError) {
                XposedBridge.log("IGGlass: restore: " + restoreError);
            }
            fallback(nav);
            STYLED.put(nav, true);
        }
    }


    /**
     * Verified using the Instagram 452.0.0.48.64 runtime hierarchy:
     *   layout_container_main_wrapper (2979px high)
     *   - layout_container_main (bottomMargin = 180px)
     *   - swipeable_tab_view_pager (bottomMargin = 180px)
     *   - tab_bar_shadow (1px, still visible after nav relocation)
     *
     * Instagram reserves the tab row's 180px by shrinking the actual feed
     * and its pager, not by putting them in a tiny empty wrapper. Therefore
     * collapsing parent layouts cannot work. Only reclaim this space after
     * checking the exact parent/child resource IDs and margin values seen in
     * the user's diagnostic log; never alter unknown layouts.
     */
    private static boolean releaseKnownNavSpace(ViewGroup oldParent, int navHeight) {
        if (!"layout_container_main_wrapper".equals(resourceName(oldParent))) {
            XposedBridge.log("IGGlass: targeted margin fix skipped: parent id mismatch");
            return false;
        }
        View main = null;
        View pager = null;
        View shadow = null;
        View bottomSearchBar = null;
        for (int i = 0; i < oldParent.getChildCount(); i++) {
            View child = oldParent.getChildAt(i);
            String id = resourceName(child);
            if ("layout_container_main".equals(id)) main = child;
            else if ("swipeable_tab_view_pager".equals(id)) pager = child;
            else if ("tab_bar_shadow".equals(id)) shadow = child;
            else if ("layout_bottom_searchbar".equals(id)) bottomSearchBar = child;
        }
        if (!hasReservedBottomMargin(main, navHeight)
                || !hasReservedBottomMargin(pager, navHeight)) {
            XposedBridge.log("IGGlass: targeted margin fix skipped: "
                    + "feed/pager IDs or original margins did not match");
            return false;
        }
        try {
            clearBottomMargin(main, "layout_container_main");
            clearBottomMargin(pager, "swipeable_tab_view_pager");
            if (hasReservedBottomMargin(bottomSearchBar, navHeight)) {
                clearBottomMargin(bottomSearchBar, "layout_bottom_searchbar");
            }
            if (shadow != null && shadow.getHeight() <= dp(shadow, 3)) {
                shadow.setVisibility(View.GONE);
                XposedBridge.log("IGGlass: hidden obsolete tab_bar_shadow");
            }
            oldParent.requestLayout();
            XposedBridge.log("IGGlass: released " + navHeight
                    + "px reserved bottom space for feed and pager");
            return true;
        } catch (Throwable e) {
            XposedBridge.log("IGGlass: targeted margin fix failed: " + e);
            return false;
        }
    }

    private static boolean hasReservedBottomMargin(View view, int expected) {
        if (view == null) return false;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        return params instanceof FrameLayout.LayoutParams
                && ((FrameLayout.LayoutParams) params).bottomMargin == expected;
    }

    private static void clearBottomMargin(View view, String name) {
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) view.getLayoutParams();
        int previous = params.bottomMargin;
        params.bottomMargin = 0;
        view.setLayoutParams(params);
        XposedBridge.log("IGGlass: " + name
                + " bottomMargin " + previous + " -> 0");
    }

    private static String resourceName(View view) {
        if (view == null || view.getId() == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * The old Instagram bottom-navigation *slot* is not the glass overlay:
     * the actual tab row has already been moved to 'shell'. With the former
     * wrapper still measured in the app, the feed stops above an opaque band.
     *
     * Collapse only small, bottom-anchored containers with no interactive or
     * textual content remaining. Do not touch the system navigation bar.
     */
    private static void collapseLegacyNavSlot(ViewGroup formerParent,
                                               FrameLayout host, int navHeight) {
        View candidate = formerParent;
        int collapsed = 0;
        for (int depth = 0; depth < 5; depth++) {
            if (!(candidate instanceof ViewGroup) || candidate == host) break;
            ViewGroup group = (ViewGroup) candidate;
            View next = group.getParent() instanceof View
                    ? (View) group.getParent() : null;
            if (next == null || !descendsFrom(group, host)) break;

            int slotHeight = group.getHeight();
            if (slotHeight <= 0 || slotHeight > navHeight + dp(group, 45)
                    || slotHeight > dp(group, 135)) break;

            int[] slotXY = new int[2];
            int[] hostXY = new int[2];
            group.getLocationOnScreen(slotXY);
            host.getLocationOnScreen(hostXY);
            int relativeTop = slotXY[1] - hostXY[1];
            if (relativeTop < host.getHeight() * 0.66f) break;
            if (!isEmptyNavDecoration(group, 0)) {
                XposedBridge.log("IGGlass: preserve live slot "
                        + group.getClass().getSimpleName()
                        + " children=" + group.getChildCount());
                break;
            }

            group.setBackground(null);
            group.setMinimumHeight(0);
            group.setVisibility(View.GONE);
            group.requestLayout();
            collapsed++;
            XposedBridge.log("IGGlass: collapsed old navbar slot: "
                    + group.getClass().getSimpleName()
                    + " h=" + slotHeight + " children=" + group.getChildCount());
            candidate = next;
        }
        if (collapsed == 0) {
            // Visible in LSPosed logs for users whose Instagram version has a
            // proprietary bottom panel that cannot be safely collapsed.
            XposedBridge.log("IGGlass: old nav slot not safely collapsible; "
                    + "retain its original layout pending hierarchy analysis");
        }
        host.requestLayout();
    }

    private static boolean isEmptyNavDecoration(View view, int depth) {
        if (depth >= 5) return false;
        if (view.getVisibility() != View.VISIBLE) return true;
        if (view.hasOnClickListeners() || view.isClickable()
                || view.isLongClickable() || view.isFocusable()) return false;
        if (view instanceof android.widget.TextView
                && ((android.widget.TextView) view).length() > 0) return false;
        if (view.getContentDescription() != null
                && view.getContentDescription().length() > 0) return false;
        if (!(view instanceof ViewGroup)) return true;
        ViewGroup group = (ViewGroup) view;
        if (group.getChildCount() > 12) return false;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (!isEmptyNavDecoration(group.getChildAt(i), depth + 1))
                return false;
        }
        return true;
    }

    private static void clearOpaqueFullSizeWrappers(ViewGroup nav,
                                                     int barWidth, int barHeight,
                                                     int depth) {
        if (depth > 3) return;
        nav.setBackground(null);
        for (int i = 0; i < nav.getChildCount(); i++) {
            View child = nav.getChildAt(i);
            if (!(child instanceof ViewGroup)) continue;
            if (child.getWidth() >= barWidth * .82f
                    && child.getHeight() >= barHeight * .82f) {
                clearOpaqueFullSizeWrappers((ViewGroup) child,
                        barWidth, barHeight, depth + 1);
            }
        }
    }

    /**
     * Renders only a small region of the host beneath the floating bar into
     * a low-resolution bitmap. Hiding the shell *during the draw call* avoids
     * capturing the bar itself. It never requests device screenshot permission.
     *
     * A blank Instagram bottom spacer does not reveal actual feed pixels;
     * in that case sample the strip immediately above as a visual fallback.
     */
    private static void updateBackdrop(Activity activity, FrameLayout host,
                                       FrameLayout shell, ImageView background,
                                       boolean duringScroll) {
        if (activity.isFinishing() || activity.isDestroyed()
                || shell.getParent() != host || !shell.isAttachedToWindow()) return;
        if (shell.getWidth() <= 0 || shell.getHeight() <= 0) return;
        if (activity.hasWindowFocus()) {
            try {
                int[] navXY = new int[2];
                int[] hostXY = new int[2];
                shell.getLocationOnScreen(navXY);
                host.getLocationOnScreen(hostXY);
                int x = navXY[0] - hostXY[0];
                int y = navXY[1] - hostXY[1];
                int w = shell.getWidth(), h = shell.getHeight();

                Bitmap behind = sample(host, shell, x, y, w, h);
                if (!duringScroll && behind != null && textureScore(behind) < 11.0) {
                    Bitmap upper = sample(host, shell, x, y - h, w, h);
                    if (upper != null) {
                        if (textureScore(upper) > textureScore(behind) + 5.0) {
                            behind.recycle();
                            behind = upper;
                        } else {
                            upper.recycle();
                        }
                    }
                }
                if (behind != null) background.setImageBitmap(behind);
            } catch (Throwable error) {
                XposedBridge.log("IGGlass: sampling unsupported: " + error);
            }
        }
    }


    /**
     * Event-driven sampling instead of a fixed 900ms timer.
     * During touch/scroll capture up to 12fps, sync with display vsync;
     * at rest sample infrequently to notice asynchronous image loads.
     * Stop frame scheduling when the scroll tail ends.
     */
    private static final class GlassUpdater
            implements Choreographer.FrameCallback, ViewTreeObserver.OnScrollChangedListener {
        private final Activity activity;
        private final FrameLayout host;
        private final FrameLayout shell;
        private final ImageView background;
        private long lastFrameNs;
        private long activeUntilNs;
        private boolean frameQueued;
        private boolean stopped;
        private final Runnable idleRefresh = new Runnable() {
            @Override public void run() {
                if (!isAlive()) {
                    stop();
                    return;
                }
                if (System.nanoTime() >= activeUntilNs) {
                    updateBackdrop(activity, host, shell, background, false);
                }
                host.postDelayed(this, IDLE_REFRESH_MS);
            }
        };

        GlassUpdater(Activity activity, FrameLayout host, FrameLayout shell,
                     ImageView background) {
            this.activity = activity;
            this.host = host;
            this.shell = shell;
            this.background = background;
        }

        void start() {
            if (!isAlive()) return;
            host.getViewTreeObserver().addOnScrollChangedListener(this);
            activeUntilNs = System.nanoTime() + ACTIVE_TAIL_NS;
            queueFrame();
            host.postDelayed(idleRefresh, IDLE_REFRESH_MS);
        }

        void onTouch(MotionEvent event) {
            if (stopped || event == null) return;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeUntilNs = System.nanoTime() + ACTIVE_TAIL_NS;
                    queueFrame();
                    break;
                default: break;
            }
        }

        @Override public void onScrollChanged() {
            if (stopped) return;
            // Keeps updating after finger release while a fling is in motion.
            activeUntilNs = System.nanoTime() + ACTIVE_TAIL_NS;
            queueFrame();
        }

        private boolean isAlive() {
            return !stopped && !activity.isDestroyed() && !activity.isFinishing()
                    && shell.getParent() == host;
        }

        private void queueFrame() {
            if (frameQueued || !isAlive()) return;
            frameQueued = true;
            Choreographer.getInstance().postFrameCallback(this);
        }

        @Override public void doFrame(long frameTimeNanos) {
            frameQueued = false;
            if (!isAlive()) {
                stop();
                return;
            }
            if (frameTimeNanos - lastFrameNs >= MAX_FRAME_INTERVAL_NS) {
                lastFrameNs = frameTimeNanos;
                updateBackdrop(activity, host, shell, background, true);
            }
            if (System.nanoTime() < activeUntilNs) queueFrame();
        }

        void stop() {
            if (stopped) return;
            stopped = true;
            host.removeCallbacks(idleRefresh);
            ViewTreeObserver observer = host.getViewTreeObserver();
            if (observer.isAlive()) observer.removeOnScrollChangedListener(this);
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    private static Bitmap sample(FrameLayout host, FrameLayout shell,
                                 int x, int y, int w, int h) {
        // Low-resolution offscreen render; skip the floating glass layer itself
        // without calling setVisibility(), which can invalidate layout/scroll.
        final float scale = .30f;
        Bitmap bitmap = Bitmap.createBitmap(
                Math.max(1, (int) (w * scale)),
                Math.max(1, (int) (h * scale)),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF13151D);
        canvas.scale(scale, scale);
        canvas.translate(-x, -y);
        canvas.clipRect(x, y, x + w, y + h);
        try {
            if (host.getBackground() != null) host.getBackground().draw(canvas);
            for (int i = 0; i < host.getChildCount(); i++) {
                View child = host.getChildAt(i);
                if (child == shell || child.getVisibility() != View.VISIBLE) continue;
                int saved = canvas.save();
                canvas.translate(child.getLeft() - host.getScrollX(),
                        child.getTop() - host.getScrollY());
                canvas.concat(child.getMatrix());
                child.draw(canvas);
                canvas.restoreToCount(saved);
            }
            return bitmap;
        } catch (Throwable e) {
            bitmap.recycle();
            return null;
        }
    }

    private static double textureScore(Bitmap bitmap) {
        int count = 0;
        double sum = 0.0, sum2 = 0.0;
        for (int y = 0; y < bitmap.getHeight(); y += Math.max(1, bitmap.getHeight()/7)) {
            for (int x = 0; x < bitmap.getWidth(); x += Math.max(1, bitmap.getWidth()/11)) {
                int pixel = bitmap.getPixel(x, y);
                double luma = .2126 * Color.red(pixel)
                        + .7152 * Color.green(pixel) + .0722 * Color.blue(pixel);
                sum += luma;
                sum2 += luma * luma;
                count++;
            }
        }
        if (count == 0) return 0.0;
        return Math.sqrt(Math.max(0.0, sum2/count - (sum/count)*(sum/count)));
    }

    private static void fallback(ViewGroup nav) {
        nav.setPivotX(nav.getWidth() / 2f);
        nav.setPivotY(nav.getHeight() / 2f);
        nav.setScaleX(.93f);
        nav.setTranslationY(-dp(nav, 7));
    }

    private static boolean descendsFrom(View child, ViewGroup ancestor) {
        View current = child;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent() instanceof View
                    ? (View) current.getParent() : null;
        }
        return false;
    }

    private static boolean isInsideCollection(ViewGroup group) {
        View current = group;
        for (int i = 0; i < 5 && current != null; i++) {
            String name = current.getClass().getName();
            if (name.contains("RecyclerView") || name.contains("ViewPager")) return true;
            current = current.getParent() instanceof View
                    ? (View) current.getParent() : null;
        }
        return false;
    }

    private static int dp(View view, float size) {
        return Math.round(size * view.getResources().getDisplayMetrics().density);
    }

    private static ViewGroup findCandidate(ViewGroup root, int screenHeight, int depth) {
        if (depth > 13) return null;
        ViewGroup result = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || !(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            if (isNavCandidate(group, screenHeight)) result = group;
            ViewGroup nested = findCandidate(group, screenHeight, depth + 1);
            if (nested != null) result = nested;
        }
        return result;
    }

    private static boolean isNavCandidate(ViewGroup group, int screenHeight) {
        if (group.getWidth() < 160 || group.getHeight() <= 0) return false;
        int[] pos = new int[2];
        group.getLocationOnScreen(pos);
        float density = group.getResources().getDisplayMetrics().density;
        if (pos[1] < screenHeight * .72f
                || group.getHeight() > 105 * density
                || group.getHeight() < 34 * density) return false;
        int children = group.getChildCount();
        if (children < 3 || children > 8) return false;
        int match = 0;
        for (int i = 0; i < children; i++) {
            View child = group.getChildAt(i);
            String id = "";
            try {
                if (child.getId() != View.NO_ID)
                    id = child.getResources().getResourceEntryName(child.getId());
            } catch (Throwable ignored) { }
            CharSequence label = child.getContentDescription();
            String key = ((label == null ? "" : label.toString()) + " " + id)
                    .toLowerCase(Locale.ROOT);
            if (key.contains("home") || key.contains("search") || key.contains("reels")
                    || key.contains("profile") || key.contains("explore")
                    || key.contains("beranda") || key.contains("cari")
                    || key.contains("profil")) match++;
        }
        return match >= 3;
    }
}
