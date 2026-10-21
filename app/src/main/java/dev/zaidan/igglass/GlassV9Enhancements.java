package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;

/**
 * Reuses Instagram's real tab children instead of synthesizing click intents.
 * A validated five-button row can be split into a four-item main pill and
 * a separate search glass circle. Unknown hierarchies stay in the single pill.
 */
final class GlassV9Enhancements {
    static final int BOTTOM_MARGIN_DP = 30;
    private static final int EDGE_DP = 16;
    private static final int GAP_DP = 12;

    private GlassV9Enhancements() {}

    static final class Setup {
        final FrameLayout mainShell;
        final ViewGroup nav;
        final View lens;
        final FrameLayout searchShell;
        final ImageView searchBackdrop;
        final View searchButton;
        private final ViewGroup.LayoutParams originalSearchParams;
        private final int originalSearchIndex;
        private int activeIndex;
        private int lastWidth;

        Setup(FrameLayout mainShell, ViewGroup nav, View lens,
              FrameLayout searchShell, ImageView searchBackdrop,
              View searchButton, ViewGroup.LayoutParams originalSearchParams,
              int originalSearchIndex) {
            this.mainShell = mainShell;
            this.nav = nav;
            this.lens = lens;
            this.searchShell = searchShell;
            this.searchBackdrop = searchBackdrop;
            this.searchButton = searchButton;
            this.originalSearchParams = originalSearchParams;
            this.originalSearchIndex = originalSearchIndex;
        }

        void restoreSearch() {
            if (searchButton == null) return;
            if (searchButton.getParent() instanceof ViewGroup) {
                ((ViewGroup) searchButton.getParent()).removeView(searchButton);
            }
            if (searchButton.getParent() == null && originalSearchParams != null) {
                nav.addView(searchButton,
                        Math.min(originalSearchIndex, nav.getChildCount()),
                        originalSearchParams);
            }
        }

        void positionLens(boolean animated) {
            if (lens == null || nav.getWidth() == 0 || nav.getChildCount() == 0) return;
            activeIndex = Math.min(activeIndex, nav.getChildCount() - 1);
            View target = nav.getChildAt(activeIndex);
            int width = target.getWidth();
            if (width < dp(nav, 30)) width = nav.getWidth() / nav.getChildCount();
            int inset = dp(nav, 4);
            int desired = Math.max(dp(nav, 30), width - inset * 2);
            if (lastWidth != desired) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) lens.getLayoutParams();
                lp.width = desired;
                lp.height = Math.max(dp(nav, 30), nav.getHeight() - inset * 2);
                lens.setLayoutParams(lp);
                lastWidth = desired;
                animated = false;
            }
            float x = nav.getLeft() + target.getLeft() + (width - desired) / 2f;
            float y = nav.getTop() + inset;
            lens.animate().cancel();
            if (animated) {
                lens.animate().x(x).y(y).setDuration(250)
                        .setInterpolator(new DecelerateInterpolator()).start();
            } else {
                lens.setX(x);
                lens.setY(y);
            }
        }

        void onTouch(MotionEvent event) {
            if (event == null || event.getActionMasked() != MotionEvent.ACTION_UP
                    || mainShell.getVisibility() != View.VISIBLE) return;
            int[] xy = new int[2];
            nav.getLocationOnScreen(xy);
            float touchX = event.getRawX() - xy[0];
            float touchY = event.getRawY() - xy[1];
            if (touchX < 0 || touchX >= nav.getWidth()
                    || touchY < 0 || touchY >= nav.getHeight()) return;
            for (int i = 0; i < nav.getChildCount(); i++) {
                View child = nav.getChildAt(i);
                if (touchX >= child.getLeft() && touchX <= child.getRight()) {
                    activeIndex = i;
                    nav.post(() -> positionLens(true));
                    break;
                }
            }
        }
    }

    static Setup attach(Activity activity, FrameLayout host,
                        FrameLayout mainShell, ViewGroup nav,
                        int navHeight, boolean dark) {
        int edge = dp(nav, EDGE_DP);
        int gap = dp(nav, GAP_DP);
        View lens = makeLens(activity, nav, dark);
        mainShell.addView(lens, Math.min(2, mainShell.getChildCount()),
                new FrameLayout.LayoutParams(dp(nav, 56), navHeight - dp(nav, 8),
                        Gravity.TOP | Gravity.LEFT));
        Setup fallback = new Setup(mainShell, nav, lens,
                null, null, null, null, -1);
        View search = findSearchItem(nav);
        if (search == null) {
            XposedBridge.log("IGGlass v0.9: cannot safely isolate search; retaining one pill");
            nav.post(() -> fallback.positionLens(false));
            return fallback;
        }

        int index = nav.indexOfChild(search);
        ViewGroup.LayoutParams originalParams = search.getLayoutParams();
        FrameLayout searchShell = null;
        try {
            nav.removeViewAt(index);
            SearchGlass glass = makeSearchGlass(activity, nav, navHeight, dark);
            searchShell = glass.shell;
            glass.shell.addView(search, glass.shell.getChildCount() - 1,
                    new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
            FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                    navHeight, navHeight, Gravity.BOTTOM | Gravity.RIGHT);
            buttonParams.rightMargin = edge;
            buttonParams.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
            host.addView(glass.shell, buttonParams);

            FrameLayout.LayoutParams mainLp =
                    (FrameLayout.LayoutParams) mainShell.getLayoutParams();
            mainLp.leftMargin = edge;
            mainLp.rightMargin = edge + navHeight + gap;
            mainLp.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);
            mainShell.setLayoutParams(mainLp);

            Setup split = new Setup(mainShell, nav, lens, glass.shell,
                    glass.backdrop, search, originalParams, index);
            nav.post(() -> split.positionLens(false));
            XposedBridge.log("IGGlass v0.9: split search tab at index=" + index
                    + " mainButtons=" + nav.getChildCount()
                    + " height=" + navHeight
                    + "px raised=" + BOTTOM_MARGIN_DP + "dp");
            return split;
        } catch (Throwable e) {
            XposedBridge.log("IGGlass v0.9: search split failed safely: " + e);
            if (search.getParent() instanceof ViewGroup) {
                ((ViewGroup) search.getParent()).removeView(search);
            }
            if (searchShell != null && searchShell.getParent() == host) {
                host.removeView(searchShell);
            }
            if (search.getParent() == null) {
                nav.addView(search, Math.min(index, nav.getChildCount()), originalParams);
            }
            nav.post(() -> fallback.positionLens(false));
            return fallback;
        }
    }

    private static View findSearchItem(ViewGroup nav) {
        String parentName = resourceName(nav);
        if (!"tab_bar".equals(parentName)) return null;
        for (int i = 0; i < nav.getChildCount(); i++) {
            if (isSearch(nav.getChildAt(i), 0)) return nav.getChildAt(i);
        }
        // Diagnostic screenshot confirms: Home / Reels / Share / Search / Profile.
        // Only rely on slot 4 for this verified five-column Instagram tab row.
        if (nav.getChildCount() != 5) return null;
        int totalWidth = nav.getWidth();
        if (totalWidth <= 0) return null;
        for (int i = 0; i < 5; i++) {
            View child = nav.getChildAt(i);
            if (child.getWidth() < totalWidth * .10f
                    || child.getWidth() > totalWidth * .35f) return null;
        }
        XposedBridge.log("IGGlass v0.9: search inferred from verified 4th slot");
        return nav.getChildAt(3);
    }

    private static boolean isSearch(View view, int depth) {
        if (depth > 4) return false;
        CharSequence desc = view.getContentDescription();
        String key = (resourceName(view) + " "
                + (desc == null ? "" : desc.toString()))
                .toLowerCase(Locale.ROOT);
        if (key.contains("search") || key.contains("explore")
                || key.contains("pencarian") || key.contains("cari")) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < Math.min(group.getChildCount(), 10); i++) {
                if (isSearch(group.getChildAt(i), depth + 1)) return true;
            }
        }
        return false;
    }

    private static View makeLens(Activity activity, View nav, boolean dark) {
        View lens = new View(activity);
        GradientDrawable liquid = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0x9B7891AE, 0x62374862, 0x92627D9A}
                     : new int[] {0xC9FFFFFF, 0x89DFEEFF, 0xAFFFFFFF});
        liquid.setCornerRadius(dp(nav, 35));
        liquid.setStroke(dp(nav, 1), dark ? 0xB6F7FBFF : 0xECFFFFFF);
        lens.setBackground(liquid);
        lens.setClickable(false);
        return lens;
    }

    private static final class SearchGlass {
        final FrameLayout shell;
        final ImageView backdrop;
        SearchGlass(FrameLayout shell, ImageView backdrop) {
            this.shell = shell;
            this.backdrop = backdrop;
        }
    }

    private static SearchGlass makeSearchGlass(Activity activity,
                                                View nav, int height,
                                                boolean dark) {
        FrameLayout shell = new FrameLayout(activity);
        shell.setClipChildren(true);
        shell.setClipToPadding(true);
        shell.setElevation(dp(nav, 16));
        shell.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        view.getHeight() / 2f);
            }
        });
        shell.setClipToOutline(true);

        ImageView backdrop = new ImageView(activity);
        backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        backdrop.setClickable(false);
        if (Build.VERSION.SDK_INT >= 31) {
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(
                    dp(nav, 10), dp(nav, 10), Shader.TileMode.CLAMP));
        }
        shell.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));

        View tint = new View(activity);
        GradientDrawable frost = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0x743B4A64, 0x4E141928}
                     : new int[] {0x9CFFFFFF, 0x5EE6F3FF});
        frost.setCornerRadius(height / 2f);
        tint.setBackground(frost);
        tint.setClickable(false);
        shell.addView(tint, new FrameLayout.LayoutParams(-1, -1));

        View gleam = new View(activity);
        GradientDrawable sheen = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x31FFFFFF, 0x08FFFFFF, 0x00FFFFFF});
        sheen.setCornerRadius(height / 2f);
        gleam.setBackground(sheen);
        gleam.setClickable(false);
        shell.addView(gleam, new FrameLayout.LayoutParams(-1, -1));

        View outline = new View(activity);
        GradientDrawable stroke = new GradientDrawable();
        stroke.setCornerRadius(height / 2f);
        stroke.setColor(0x00000000);
        stroke.setStroke(dp(nav, 1), dark ? 0xBAFFFFFF : 0xDCFFFFFF);
        outline.setBackground(stroke);
        outline.setClickable(false);
        shell.addView(outline, new FrameLayout.LayoutParams(-1, -1));
        return new SearchGlass(shell, backdrop);
    }

    private static String resourceName(View view) {
        if (view.getId() == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int dp(View v, float value) {
        return Math.round(v.getResources().getDisplayMetrics().density * value);
    }
}
