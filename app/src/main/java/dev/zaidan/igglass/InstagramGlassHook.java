package dev.zaidan.igglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import java.util.Locale;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v0.2: Moves a detected Instagram bottom tab bar into a floating overlay.
 * The bar is genuinely translucent: feed pixels can show through if the feed
 * extends underneath it. This is NOT a backdrop-blur or iOS refraction shader.
 *
 * Experimental: Instagram 452's layout is proprietary and may change.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String TARGET = "com.instagram.android";
    private static final WeakHashMap<View, Boolean> STYLED = new WeakHashMap<>();
    private static final int EDGE_DP = 14;
    private static final int BOTTOM_DP = 15;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        XposedBridge.log("IGGlass v0.2: process " + lpparam.processName);
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!TARGET.equals(activity.getPackageName())) return;
                View decor = activity.getWindow().getDecorView();
                for (int delay : new int[] {350, 1200, 2500}) {
                    decor.postDelayed(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            try { locateAndStyle(activity); }
                            catch (Throwable error) {
                                XposedBridge.log("IGGlass: " + error);
                            }
                        }
                    }, delay);
                }
            }
        });
    }

    private static void locateAndStyle(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup) || decor.getHeight() == 0) return;
        ViewGroup bar = findCandidate((ViewGroup) decor, decor.getHeight(), 0);
        if (bar == null || STYLED.containsKey(bar)) return;

        boolean floating = placeAboveFeed(activity, bar);
        styleGlass(bar);
        STYLED.put(bar, true);
        XposedBridge.log("IGGlass v0.2: " + (floating ? "overlay" : "safe fallback")
                + " on " + bar.getClass().getName());
    }

    /**
     * Reparent into android.R.id.content instead of translating the View.
     * This keeps touch coordinates consistent with the visible floating bar.
     * If reparenting is unsuitable, fall back to an inset visual transformation.
     */
    private static boolean placeAboveFeed(Activity activity, ViewGroup bar) {
        View contentView = activity.findViewById(android.R.id.content);
        if (!(contentView instanceof FrameLayout)) {
            applyFallback(bar);
            return false;
        }
        FrameLayout host = (FrameLayout) contentView;
        if (!(bar.getParent() instanceof ViewGroup)) {
            applyFallback(bar);
            return false;
        }
        ViewGroup parent = (ViewGroup) bar.getParent();
        if (parent != host && !descendsFrom(parent, host)) {
            applyFallback(bar);
            return false;
        }
        // Never remove bars owned by RecyclerView/ViewPager cells.
        if (isManagedCollectionParent(parent)) {
            applyFallback(bar);
            return false;
        }

        int oldIndex = parent.indexOfChild(bar);
        ViewGroup.LayoutParams oldParams = bar.getLayoutParams();
        int measuredHeight = bar.getHeight();
        if (measuredHeight < dp(bar, 34) || measuredHeight > dp(bar, 105)) {
            applyFallback(bar);
            return false;
        }

        FrameLayout.LayoutParams floatParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, measuredHeight,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        floatParams.leftMargin = dp(bar, EDGE_DP);
        floatParams.rightMargin = dp(bar, EDGE_DP);
        floatParams.bottomMargin = dp(bar, BOTTOM_DP);

        if (parent == host) {
            bar.setLayoutParams(floatParams);
            host.bringChildToFront(bar);
            return true;
        }

        try {
            parent.removeView(bar);
            host.addView(bar, floatParams);
            // Collapse an empty fixed-height slot when safe. On a vertical
            // LinearLayout this lets the feed occupy the space behind the bar.
            if (parent.getChildCount() == 0 && parent.getHeight() < dp(bar, 130)
                    && parent.getHeight() > 0
                    && parent.getParent() instanceof ViewGroup) {
                parent.setVisibility(View.GONE);
            }
            host.bringChildToFront(bar);
            return true;
        } catch (Throwable error) {
            XposedBridge.log("IGGlass: reparent failed, restoring: " + error);
            try {
                if (bar.getParent() instanceof ViewGroup) {
                    ((ViewGroup) bar.getParent()).removeView(bar);
                }
                if (bar.getParent() == null) {
                    parent.addView(bar,
                            Math.min(oldIndex, parent.getChildCount()), oldParams);
                }
            } catch (Throwable restoreError) {
                XposedBridge.log("IGGlass: restoration failed: " + restoreError);
            }
            applyFallback(bar);
            return false;
        }
    }

    private static boolean descendsFrom(View ancestor, ViewGroup host) {
        View current = ancestor;
        while (current != null) {
            if (current == host) return true;
            if (!(current.getParent() instanceof View)) return false;
            current = (View) current.getParent();
        }
        return false;
    }

    private static boolean isManagedCollectionParent(ViewGroup group) {
        View current = group;
        for (int i = 0; i < 5 && current != null; i++) {
            String name = current.getClass().getName();
            if (name.contains("RecyclerView") || name.contains("ViewPager")
                    || name.contains("ViewPager2")) return true;
            current = current.getParent() instanceof View
                    ? (View) current.getParent() : null;
        }
        return false;
    }

    private static void applyFallback(ViewGroup bar) {
        // Transformation only: does not alter the parent layout or touch target.
        bar.setPivotX(bar.getWidth() / 2f);
        bar.setPivotY(bar.getHeight() / 2f);
        bar.setScaleX(0.93f);
        bar.setTranslationY(-dp(bar, 7));
    }

    private static int dp(View view, float size) {
        return Math.round(size * view.getResources().getDisplayMetrics().density);
    }

    private static void styleGlass(ViewGroup bar) {
        boolean dark = (bar.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        float radius = dp(bar, 35);
        // 25-42% alpha. The actual feed should be visible through this pill.
        int[] glassColors = dark
                ? new int[] {0x703F4655, 0x45161C2A}
                : new int[] {0x86FFFFFF, 0x58DCE5F2};
        GradientDrawable glass = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, glassColors);
        glass.setCornerRadius(radius);
        glass.setStroke(dp(bar, 1), dark ? 0x8DFFFFFF : 0xA6FFFFFF);

        // Subtle white specular highlight without an opaque backing.
        GradientDrawable reflection = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {dark ? 0x21FFFFFF : 0x40FFFFFF, 0x00FFFFFF});
        reflection.setCornerRadius(radius);
        bar.setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[] {
                glass, reflection
        }));
        bar.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        bar.setClipToOutline(true);
        bar.setElevation(dp(bar, 14));
        bar.invalidate();
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
        int[] position = new int[2];
        group.getLocationOnScreen(position);
        float dp = group.getResources().getDisplayMetrics().density;
        if (position[1] < screenHeight * 0.72f ||
                group.getHeight() > 105 * dp || group.getHeight() < 34 * dp) return false;
        int children = group.getChildCount();
        if (children < 3 || children > 8) return false;
        int matches = 0;
        for (int i = 0; i < children; i++) {
            View v = group.getChildAt(i);
            String id = "";
            try {
                if (v.getId() != View.NO_ID)
                    id = v.getResources().getResourceEntryName(v.getId());
            } catch (Throwable ignored) { }
            CharSequence desc = v.getContentDescription();
            String key = ((desc == null ? "" : desc.toString()) + " " + id)
                    .toLowerCase(Locale.ROOT);
            if (key.contains("home") || key.contains("search") || key.contains("reels") ||
                    key.contains("profile") || key.contains("explore") ||
                    key.contains("beranda") || key.contains("cari") ||
                    key.contains("profil")) matches++;
        }
        return matches >= 3;
    }
}
