package dev.zaidan.igglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
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
 * Lightweight glass: real original Instagram tab button Views,
 * translucent layered gradients, a moving specular lens and a separate Search
 * circle. No bitmap screenshots, Canvas capture or 12fps CPU blur loops.
 */
final class DevGlassUi {
    static final int BOTTOM_MARGIN_DP = 26;
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
        final View lens;
        final DevLiquidFx mainFx;
        final DevLiquidFx searchFx;
        final int barHeight;
        final boolean dark;
        private int activeIndex = 0;
        private boolean selectionSearch;
        private int previousLensWidth;

        Scene(FrameLayout host, ViewGroup sourceParent,
              ViewGroup nav, int oldIndex, ViewGroup.LayoutParams originalNavParams,
              FrameLayout mainPill, FrameLayout searchPill, View search,
              ViewGroup.LayoutParams originalSearchParams, int originalSearchIndex,
              View lens, int barHeight, boolean dark) {
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
                    || mainPill.getVisibility() != View.VISIBLE) return;
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
            if (!attached()) return;
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

        void removeAndRestore() {
            try {
                if (search != null) {
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
        boolean dark = (nav.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Single native navigation instance: detach once, preserve all listeners.
        FrameLayout main = createPill(activity, height, dark);
        View lens = createLens(activity, dark);
        main.addView(lens, new FrameLayout.LayoutParams(dp(nav, 48),
                height - dp(nav, 8), Gravity.TOP | Gravity.LEFT));

        View search = null;
        ViewGroup.LayoutParams searchOriginalParams = null;
        int searchIndex = -1;
        FrameLayout searchShell = null;
        boolean relocated = false;
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
                nav.removeViewAt(searchIndex);
                searchShell = createPill(activity, height, dark);
                // Move exactly one genuine Instagram View, never forge clicks.
                searchShell.addView(search, new FrameLayout.LayoutParams(
                        -1, -1, Gravity.CENTER));
                FrameLayout.LayoutParams searchLp = new FrameLayout.LayoutParams(
                        height, height, Gravity.BOTTOM | Gravity.RIGHT);
                searchLp.rightMargin = side;
                searchLp.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
                host.addView(searchShell, searchLp);
            }
            FrameLayout.LayoutParams mainLp = new FrameLayout.LayoutParams(
                    -1, height, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            mainLp.leftMargin = side;
            mainLp.rightMargin = searchShell == null
                    ? side : side + height + gap;
            mainLp.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
            host.addView(main, mainLp);
            relocated = true;
            Scene scene = new Scene(host, oldParent, nav, originalIndex,
                    oldParams, main, searchShell, search, searchOriginalParams,
                    searchIndex, lens, height, dark);
            main.post(() -> scene.positionLens(false));
            DevFileLog.note("glass installed buttons=" + nav.getChildCount()
                    + "; search separated=" + (searchShell != null)
                    + "; height=" + height + "px; raised=" + BOTTOM_MARGIN_DP + "dp");
            return scene;
        } catch (Throwable error) {
            DevFileLog.note("glass install error: " + error.getClass().getSimpleName());
            try {
                if (search != null && search.getParent() instanceof ViewGroup) {
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
        shell.setClipChildren(false);
        shell.setClipToPadding(false);
        shell.setElevation(dp(shell, 13));
        shell.setClickable(false);

        GradientDrawable body = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0xD048586B, 0xAD17212F, 0xB8344354}
                     : new int[] {0xD5FFFFFF, 0xA7DDECF8, 0xC9FFFFFF});
        body.setCornerRadius(height / 2f);
        body.setStroke(dp(shell, 1), dark ? 0xC4E8F5FF : 0xE8FFFFFF);
        shell.setBackground(body);
        shell.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        Math.min(view.getHeight(), view.getWidth()) / 2f);
            }
        });
        shell.setClipToOutline(true);

        View shine = new View(activity);
        GradientDrawable gloss = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x43FFFFFF, 0x12FFFFFF, 0x00FFFFFF});
        gloss.setCornerRadius(height / 2f);
        shine.setBackground(gloss);
        shine.setClickable(false);
        shell.addView(shine, new FrameLayout.LayoutParams(-1, -1));
        DevLiquidFx ripple = new DevLiquidFx(activity);
        shell.addView(ripple, new FrameLayout.LayoutParams(-1, -1));
        return shell;
    }

    private static View createLens(Activity activity, boolean dark) {
        View lens = new View(activity);
        GradientDrawable halo = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0xB3ACCADE, 0x725D7798, 0xA084A1BE}
                     : new int[] {0xD5FFFFFF, 0x8FA9D7F9, 0xBBFFFFFF});
        halo.setCornerRadius(dp(lens, 40));
        halo.setStroke(dp(lens, 1), dark ? 0xCFF8FDFF : 0xF0FFFFFF);
        lens.setBackground(halo);
        lens.setClickable(false);
        return lens;
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
