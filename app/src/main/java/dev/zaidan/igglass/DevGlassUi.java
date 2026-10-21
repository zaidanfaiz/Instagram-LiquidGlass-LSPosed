package dev.zaidan.igglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import java.util.Locale;

/**
 * iOS-style glass composition: original clickable Instagram tab Views,
 * refracted cached backdrop, optical specular rim, lens and separate Search
 * circle. No continuous per-frame screen capture or heavy render loops.
 */
final class DevGlassUi {
    static final int BOTTOM_MARGIN_DP = 52;
    private static final int SIDE_DP = 15;
    private static final int GAP_DP = 12;
    private static final int HIGHLIGHT_ANIM_MS = 230;
    private DevGlassUi() {}

    static final class Scene {
        final FrameLayout host;
        final ViewGroup sourceParent;
        final ViewGroup nav;
        final int oldIndex;
        final ViewGroup.LayoutParams originalNavParams;
        final FrameLayout mainPill;
        final FrameLayout searchPill;
        final View search;
        final ViewGroup.LayoutParams originalSearchParams;
        final int originalSearchIndex;
        final float originalSearchAlpha;
        final View lens;
        final DevLiquidFx mainFx;
        final DevLiquidFx searchFx;
        final int barHeight;
        final boolean dark;
        private int activeIndex = 0;
        private boolean selectionSearch;
        private int previousLensWidth;
        private boolean dragging;
        private long lastSlideAt;

        Scene(FrameLayout host, ViewGroup sourceParent,
              ViewGroup nav, int oldIndex, ViewGroup.LayoutParams originalNavParams,
              FrameLayout mainPill, FrameLayout searchPill, View search,
              ViewGroup.LayoutParams originalSearchParams, int originalSearchIndex,
              float originalSearchAlpha, View lens, int barHeight, boolean dark) {
            this.host = host;
            this.sourceParent = sourceParent;
            this.nav = nav;
            this.oldIndex = oldIndex;
            this.originalNavParams = originalNavParams;
            this.mainPill = mainPill;
            this.searchPill = searchPill;
            this.search = search;
            this.originalSearchParams = originalSearchParams;
            this.originalSearchIndex = originalSearchIndex;
            this.originalSearchAlpha = originalSearchAlpha;
            this.lens = lens;
            this.mainFx = effectInside(mainPill);
            this.searchFx = effectInside(searchPill);
            this.barHeight = barHeight;
            this.dark = dark;
        }

        boolean attached() {
            return mainPill.getParent() == host
                    && nav.getParent() == mainPill
                    && (searchPill == null || searchPill.getParent() == host);
        }

        void show(boolean show) {
            int visibility = show ? View.VISIBLE : View.GONE;
            if (mainPill.getVisibility() != visibility) {
                mainPill.setVisibility(visibility);
            }
            if (searchPill != null && searchPill.getVisibility() != visibility) {
                searchPill.setVisibility(visibility);
            }
        }

        void onTouch(MotionEvent ev) {
            if (ev == null || ev.getActionMasked() != MotionEvent.ACTION_UP
                    || mainPill.getVisibility() != View.VISIBLE
                    || System.currentTimeMillis() - lastSlideAt < 180) return;
            final float rawX = ev.getRawX();
            final float rawY = ev.getRawY();
            if (searchPill != null && inside(searchPill, rawX, rawY)) {
                if (searchFx != null) pulse(searchFx, searchPill, rawX, rawY);
                selectSearch();
                DevFileLog.note("touch native SEARCH control");
                return;
            }
            if (!inside(nav, rawX, rawY)) return;
            int[] p = new int[2];
            nav.getLocationOnScreen(p);
            float x = rawX - p[0];
            for (int i = 0; i < nav.getChildCount(); i++) {
                View child = nav.getChildAt(i);
                if (x >= child.getLeft() && x <= child.getRight()) {
                    if (mainFx != null) pulse(mainFx, mainPill, rawX, rawY);
                    selectMain(i, true);
                    DevFileLog.note("touch native main tab index=" + i);
                    break;
                }
            }
        }

        private static void pulse(DevLiquidFx effect, View parent,
                                  float rawX, float rawY) {
            int[] xy = new int[2];
            parent.getLocationOnScreen(xy);
            effect.pulse(rawX - xy[0], rawY - xy[1]);
        }

        private static DevLiquidFx effectInside(FrameLayout shell) {
            if (shell == null) return null;
            for (int i = 0; i < shell.getChildCount(); i++) {
                View child = shell.getChildAt(i);
                if (child instanceof DevLiquidFx) return (DevLiquidFx) child;
            }
            return null;
        }

        private static boolean inside(View v, float rawX, float rawY) {
            int[] xy = new int[2];
            v.getLocationOnScreen(xy);
            return rawX >= xy[0] && rawX <= xy[0] + v.getWidth()
                    && rawY >= xy[1] && rawY <= xy[1] + v.getHeight();
        }

        void syncSelection() {
            if (!attached() || dragging) return;
            if (search != null && isSelected(search, 0)) {
                if (!selectionSearch) selectSearch();
                return;
            }
            for (int i = 0; i < nav.getChildCount(); i++) {
                if (isSelected(nav.getChildAt(i), 0)) {
                    if (selectionSearch || activeIndex != i) selectMain(i, true);
                    break;
                }
            }
        }

        private static boolean isSelected(View v, int depth) {
            if (depth > 4) return false;
            if (v.isSelected() || v.isActivated()) return true;
            if (!(v instanceof ViewGroup)) return false;
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < Math.min(group.getChildCount(), 6); i++) {
                if (isSelected(group.getChildAt(i), depth + 1)) return true;
            }
            return false;
        }

        private void selectSearch() {
            selectionSearch = true;
            if (lens != null) lens.animate().alpha(0f)
                    .setDuration(HIGHLIGHT_ANIM_MS).start();
            if (searchPill != null) {
                searchPill.animate().scaleX(1.04f).scaleY(1.04f)
                        .setDuration(180).withEndAction(() ->
                                searchPill.animate().scaleX(1f).scaleY(1f)
                                        .setDuration(210).start()).start();
            }
        }

        void selectMain(int index, boolean animated) {
            selectionSearch = false;
            activeIndex = Math.max(0, Math.min(nav.getChildCount() - 1, index));
            if (lens != null) {
                lens.animate().alpha(1f).setDuration(140).start();
            }
            nav.post(() -> positionLens(animated));
        }

        void positionLens(boolean animated) {
            if (lens == null || nav.getChildCount() == 0
                    || nav.getWidth() <= 0 || nav.getHeight() <= 0) return;
            int index = Math.max(0, Math.min(activeIndex, nav.getChildCount() - 1));
            View target = nav.getChildAt(index);
            int width = target.getWidth();
            if (width <= 0) return;
            int inset = dp(nav, 4);
            int desired = Math.max(dp(nav, 28), width - inset * 2);
            int h = Math.max(dp(nav, 28), nav.getHeight() - inset * 2);
            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) lens.getLayoutParams();
            if (params.width != desired || params.height != h) {
                params.width = desired;
                params.height = h;
                lens.setLayoutParams(params);
                previousLensWidth = desired;
                animated = false;
            }
            float x = nav.getLeft() + target.getLeft() + (width - desired) / 2f;
            float y = nav.getTop() + inset;
            lens.animate().cancel();
            if (animated) {
                lens.animate().x(x).y(y).setDuration(HIGHLIGHT_ANIM_MS)
                        .setInterpolator(new DecelerateInterpolator()).start();
            } else {
                lens.setX(x);
                lens.setY(y);
            }
        }

        void slideStart(float localX) {
            dragging = true;
            lens.animate().cancel();
            lens.setAlpha(1f);
            slideMove(localX);
            DevFileLog.note("gesture: glass selection drag START");
        }

        void slideMove(float localX) {
            if (!dragging || lens == null || nav.getWidth() == 0) return;
            float desiredX = localX - lens.getWidth() / 2f;
            float low = nav.getLeft() + dp(nav, 4);
            float high = nav.getLeft() + nav.getWidth() - lens.getWidth()
                    - dp(nav, 4);
            lens.setX(Math.max(low, Math.min(desiredX, high)));
        }

        void slideEnd(float localX) {
            if (!dragging) return;
            dragging = false;
            lastSlideAt = System.currentTimeMillis();
            int index = indexAt(localX);
            selectMain(index, true);
            View destination = nav.getChildAt(index);
            boolean activated = clickRealTab(destination, 0);
            String mechanism = activated ? "performClick" : "nativeTouchDispatch";
            if (!activated) activated = dispatchOriginalTouch(destination);
            if (mainFx != null) mainFx.pulse(localX, mainPill.getHeight() / 2f);
            DevFileLog.note("gesture: glass selection drag END index=" + index
                    + " nativeAction=" + mechanism + " handled=" + activated);
        }

        void slideCancel() {
            dragging = false;
            lastSlideAt = System.currentTimeMillis();
            positionLens(true);
            DevFileLog.note("gesture: glass selection drag CANCEL");
        }

        private int indexAt(float localX) {
            float x = localX - nav.getLeft();
            int nearest = 0;
            float smallest = Float.MAX_VALUE;
            for (int i = 0; i < nav.getChildCount(); i++) {
                View v = nav.getChildAt(i);
                float mid = (v.getLeft() + v.getRight()) / 2f;
                float diff = Math.abs(x - mid);
                if (diff < smallest) {
                    smallest = diff;
                    nearest = i;
                }
            }
            return nearest;
        }

        private static boolean clickRealTab(View view, int depth) {
            if (view == null || depth > 5) return false;
            if (view.hasOnClickListeners()) return view.performClick();
            if (view instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) view;
                for (int i = 0; i < g.getChildCount(); i++) {
                    if (clickRealTab(g.getChildAt(i), depth + 1)) return true;
                }
            }
            if (view.isClickable()) return view.performClick();
            return false;
        }

        private static boolean dispatchOriginalTouch(View tab) {
            if (tab.getWidth() <= 0 || tab.getHeight() <= 0) return false;
            long t = android.os.SystemClock.uptimeMillis();
            float centerX = tab.getWidth() / 2f;
            float centerY = tab.getHeight() / 2f;
            MotionEvent down = MotionEvent.obtain(t, t,
                    MotionEvent.ACTION_DOWN, centerX, centerY, 0);
            MotionEvent up = MotionEvent.obtain(t, t + 55,
                    MotionEvent.ACTION_UP, centerX, centerY, 0);
            try {
                boolean handledDown = tab.dispatchTouchEvent(down);
                boolean handledUp = tab.dispatchTouchEvent(up);
                return handledDown || handledUp;
            } catch (Throwable e) {
                DevFileLog.note("drag native dispatch error "
                        + e.getClass().getSimpleName());
                return false;
            } finally {
                down.recycle();
                up.recycle();
            }
        }

        void removeAndRestore() {
            try {
                if (search != null) {
                    search.setAlpha(originalSearchAlpha);
                    if (search.getParent() instanceof ViewGroup) {
                        ((ViewGroup) search.getParent()).removeView(search);
                    }
                    if (search.getParent() == null && originalSearchParams != null) {
                        nav.addView(search,
                                Math.min(originalSearchIndex, nav.getChildCount()),
                                originalSearchParams);
                    }
                }
                if (nav.getParent() instanceof ViewGroup) {
                    ((ViewGroup) nav.getParent()).removeView(nav);
                }
                if (mainPill.getParent() == host) host.removeView(mainPill);
                if (searchPill != null && searchPill.getParent() == host) {
                    host.removeView(searchPill);
                }
                if (nav.getParent() == null && sourceParent.getParent() != null) {
                    sourceParent.addView(nav, Math.min(oldIndex,
                            sourceParent.getChildCount()), originalNavParams);
                }
            } catch (Throwable ex) {
                DevFileLog.note("restore failed " + ex.getClass().getSimpleName());
            }
        }
    }

    static Scene create(Activity activity, FrameLayout host,
                        ViewGroup oldParent, ViewGroup nav) {
        if (!"tab_bar".equals(resourceName(nav))
                || !"layout_container_main_wrapper".equals(resourceName(oldParent))) {
            DevFileLog.note("glass install skipped: unrecognized navbar/parent");
            return null;
        }
        int height = nav.getHeight();
        if (height < dp(nav, 35) || height > dp(nav, 85)) return null;
        int originalIndex = oldParent.indexOfChild(nav);
        if (originalIndex < 0) return null;
        ViewGroup.LayoutParams oldParams = nav.getLayoutParams();
        // The optical material is ALWAYS neutral/clear regardless of both
        // Instagram's dark/light settings and the current post/video colors.
        // 'dark' remains for legacy backdrop capture fill only.
        boolean dark = true;
        DevFileLog.note("glass material=NEUTRAL_CLEAR; no theme switching");

        // Single native navigation instance: detach once, preserve all listeners.
        DevGesturePill main = new DevGesturePill(activity);
        stylePill(main, activity, height, dark);
        View lens = createLens(activity, dark);
        main.addView(lens, new FrameLayout.LayoutParams(dp(nav, 48),
                height - dp(nav, 8), Gravity.TOP | Gravity.LEFT));

        View search = null;
        ViewGroup.LayoutParams searchOriginalParams = null;
        int searchIndex = -1;
        float searchOriginalAlpha = 1f;
        FrameLayout searchShell = null;
        try {
            oldParent.removeView(nav);
            // Clearing only the original bar's full-coverage backgrounds keeps
            // native icons, badges, touch targets and child stateful drawables.
            clearOldRowBackground(nav, nav.getWidth(), height, 0);
            // The row remains the real, clickable Instagram tab container.
            main.addView(nav, new FrameLayout.LayoutParams(-1, -1));
            int side = dp(nav, SIDE_DP);
            int gap = dp(nav, GAP_DP);

            searchIndex = searchIndex(nav);
            if (searchIndex >= 0 && nav.getChildCount() == 5) {
                search = nav.getChildAt(searchIndex);
                searchOriginalParams = search.getLayoutParams();
                searchOriginalAlpha = search.getAlpha();
                nav.removeViewAt(searchIndex);
                // 452 gives this tab its own opaque selection surface.
                // Moving it to a circular overlay without removing that
                // full-size backing makes the Search pill solid white.
                removeOpaqueButtonSurfaces(search, height, 0, "search");
                searchShell = createPill(activity, height, dark);
                // Move exactly one genuine Instagram View, never forge clicks.
                searchShell.addView(search, new FrameLayout.LayoutParams(
                        -1, -1, Gravity.CENTER));
                // Keep the native original Search target fully interactive.
                // Suppress only its renderer: selected native icon layers
                // and backgrounds otherwise create a solid WHITE circle.
                search.setAlpha(0f);
                searchShell.addView(new DevSearchGlyph(activity),
                        new FrameLayout.LayoutParams(-1, -1));
                FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(
                        height, height, Gravity.BOTTOM | Gravity.RIGHT);
                searchLp.rightMargin = side;
                searchLp.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
                host.addView(searchShell, searchLp);
            }
            // Native tab items can each own a full-height dark backing,
            // even after the bar's parent background has been cleared.
            // Strip ONLY non-icon ViewGroup surfaces so the glass behind the
            // original clickable icons is actually visible.
            for (int n = 0; n < nav.getChildCount(); n++) {
                removeOpaqueButtonSurfaces(nav.getChildAt(n), height, 0,
                        "tab-" + n);
            }
            FrameLayout.LayoutParams mainLp = new FrameLayout.LayoutParams(
                    -1, height, Gravity.BOTTOM | Gravity.LEFT);
            mainLp.leftMargin = side;
            mainLp.rightMargin = searchShell == null
                    ? side : side + height + gap;
            mainLp.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
            host.addView(main, mainLp);
            Scene scene = new Scene(host, oldParent, nav, originalIndex,
                    oldParams, main, searchShell, search, searchOriginalParams,
                    searchIndex, searchOriginalAlpha, lens, height, dark);
            main.setSlideListener(new DevGesturePill.SlideListener() {
                @Override public void onSlideStart(float x) { scene.slideStart(x); }
                @Override public void onSlideMove(float x) { scene.slideMove(x); }
                @Override public void onSlideEnd(float x) { scene.slideEnd(x); }
                @Override public void onSlideCancel() { scene.slideCancel(); }
            });
            main.post(() -> scene.positionLens(false));
            DevFileLog.note("concept-a: translucent smoked surface + beveled active lens");
            main.post(() -> {
                DevFileLog.note("pill geometry mainX=" + main.getLeft()
                        + " mainW=" + main.getWidth()
                        + " searchX=" + (scene.searchPill == null
                              ? -1 : scene.searchPill.getLeft())
                        + " glassBottom=" + (main.getTop() + main.getHeight()));
            });
            DevFileLog.note("glass installed buttons=" + nav.getChildCount()
                    + "; search separated=" + (searchShell != null)
                    + "; height=" + height + "px; raised=" + BOTTOM_MARGIN_DP + "dp");
            return scene;
        } catch (Throwable error) {
            DevFileLog.note("glass install error: " + error.getClass().getSimpleName());
            try {
                if (search != null && search.getParent() instanceof ViewGroup) {
                    search.setAlpha(searchOriginalAlpha);
                    ((ViewGroup) search.getParent()).removeView(search);
                }
                if (search != null && search.getParent() == null
                        && searchOriginalParams != null) {
                    nav.addView(search, Math.min(searchIndex, nav.getChildCount()),
                            searchOriginalParams);
                }
                if (nav.getParent() instanceof ViewGroup) {
                    ((ViewGroup) nav.getParent()).removeView(nav);
                }
                if (main.getParent() == host) host.removeView(main);
                if (searchShell != null && searchShell.getParent() == host) {
                    host.removeView(searchShell);
                }
                if (nav.getParent() == null) {
                    oldParent.addView(nav, Math.min(originalIndex,
                            oldParent.getChildCount()), oldParams);
                }
            } catch (Throwable ignored) {}
            return null;
        }
    }

    private static FrameLayout createPill(Activity activity, int height,
                                          boolean dark) {
        FrameLayout shell = new FrameLayout(activity);
        stylePill(shell, activity, height, dark);
        return shell;
    }

    private static void stylePill(FrameLayout shell, Activity activity,
                                  int height, boolean dark) {
        // The composite stack (after DevGlassBackdrop attaches its ImageView):
        // 0 cached, darkened blur (only after idle)
        // 1 translucent smoked glass + multiple bevel highlights
        // 2 barely-visible specular sheen
        // 3 small AGSL touch ripple
        // 4 active glass lens (main only)
        // 5 original Instagram icons (never recolored or replaced)
        shell.setClipChildren(false);
        shell.setClipToPadding(false);
        shell.setElevation(dp(shell, 11));
        shell.setClickable(false);
        shell.setBackground(null);
        shell.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        Math.min(view.getHeight(), view.getWidth()) / 2f);
            }
        });
        shell.setClipToOutline(true);
        DevGlassSurface smoked = new DevGlassSurface(activity, false);
        shell.addView(smoked, new FrameLayout.LayoutParams(-1, -1));

        // The reflection is deliberately faint, so it never creates a
        // solid white overlay over the real Instagram icons.
        View shine = new View(activity);
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x21FFFFFF, 0x09FFFFFF, 0x00FFFFFF});
        gloss.setCornerRadius(height / 2f);
        shine.setBackground(gloss);
        shine.setClickable(false);
        shell.addView(shine, new FrameLayout.LayoutParams(-1, -1));

        DevLiquidFx ripple = new DevLiquidFx(activity);
        shell.addView(ripple, new FrameLayout.LayoutParams(-1, -1));
    }

    private static View createLens(Activity activity, boolean dark) {
        DevGlassSurface lens = new DevGlassSurface(activity, true);
        lens.setAlpha(.89f);
        return lens;
    }

    private static boolean nativeBackgroundIsDark(View nav) {
        Drawable drawable = nav.getBackground();
        if (drawable instanceof ColorDrawable) {
            int color = ((ColorDrawable) drawable).getColor();
            double luma = (0.2126 * Color.red(color)
                    + 0.7152 * Color.green(color)
                    + 0.0722 * Color.blue(color)) / 255.0;
            DevFileLog.note("native tab_bar background=#"
                    + Integer.toHexString(color) + " luma=" + luma);
            return luma < 0.50;
        }
        return (nav.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int searchIndex(ViewGroup nav) {
        if (nav.getChildCount() != 5) return -1;
        // The verified 452 UI order is Home, Reels, Share, Search, Profile.
        // Validate search from accessible metadata when possible. Only use
        // fallback index 3 when all five views are equal-sized native slots.
        for (int i = 0; i < nav.getChildCount(); i++) {
            if (hasSearchMarker(nav.getChildAt(i), 0)) return i;
        }
        if (nav.getWidth() <= 0) return -1;
        for (int i = 0; i < 5; i++) {
            int w = nav.getChildAt(i).getWidth();
            if (w < nav.getWidth() * .11f || w > nav.getWidth() * .30f) {
                DevFileLog.note("search split skipped: native slot widths changed");
                return -1;
            }
        }
        DevFileLog.note("search slot selected from verified index=3");
        return 3;
    }

    private static boolean hasSearchMarker(View v, int depth) {
        if (depth > 4) return false;
        CharSequence label = v.getContentDescription();
        String key = (resourceName(v) + " "
                + (label == null ? "" : label.toString()))
                .toLowerCase(Locale.ROOT);
        if (key.contains("search") || key.contains("explore")
                || key.contains("cari") || key.contains("pencarian")) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < Math.min(g.getChildCount(), 7); i++) {
                if (hasSearchMarker(g.getChildAt(i), depth + 1)) return true;
            }
        }
        return false;
    }

    private static void clearOldRowBackground(ViewGroup group, int w, int h,
                                               int depth) {
        if (depth > 3) return;
        group.setBackground(null);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup
                    && child.getWidth() >= w * .83f
                    && child.getHeight() >= h * .80f) {
                clearOldRowBackground((ViewGroup) child, w, h, depth + 1);
            }
        }
    }

    /**
     * Remove full-size backgrounds belonging to the native tab *surface*.
     * Don't strip ImageView drawables, profile avatars, icon badges, or
     * small drawable elements. Preserve the original clickable native views.
     */
    private static void removeOpaqueButtonSurfaces(View v, int fullHeight,
                                                    int depth, String name) {
        if (v == null || depth > 5) return;
        // A search tab may itself be an ImageView. Remove only the BACKGROUND
        // drawable of that full-size tab; the foreground image/icon and
        // original click handlers stay completely untouched.
        boolean fillsButton = v.getHeight() >= fullHeight * .78f
                && v.getWidth() >= dp(v, 35);
        Drawable background = v.getBackground();
        if (fillsButton && background != null && depth <= 3) {
            boolean safePanel = depth == 0
                    || v instanceof ViewGroup && v.getWidth() >= dp(v, 40);
            if (safePanel) {
                v.setBackground(null);
                DevFileLog.note("glass: removed native " + name
                        + " panel depth=" + depth + " drawable="
                        + background.getClass().getSimpleName()
                        + " view=" + v.getClass().getSimpleName());
            }
        }
        if (!(v instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) v;
        for (int i = 0; i < Math.min(group.getChildCount(), 12); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                removeOpaqueButtonSurfaces(child, fullHeight, depth + 1, name);
            }
        }
    }

    static String resourceName(View v) {
        if (v == null || v.getId() == View.NO_ID) return "";
        try {
            return v.getResources().getResourceEntryName(v.getId());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int dp(View v, float size) {
        return Math.round(v.getResources().getDisplayMetrics().density * size);
    }
}
