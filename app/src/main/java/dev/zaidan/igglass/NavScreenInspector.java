package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

/**
 * Hides only the custom floating overlay; does not mutate Instagram's
 * fragments or screens. The host main Activity may remain resumed while
 * Instagram changes pages, so onResume/onPause alone are insufficient.
 */
final class NavScreenInspector {
    private NavScreenInspector() {}

    static String hideReason(Activity activity, ViewGroup originalTab) {
        if (activity.isDestroyed() || activity.isFinishing()) return "activity-closing";
        if (!activity.hasWindowFocus()) return "activity-not-foreground";
        if (originalTab.getVisibility() != View.VISIBLE
                || originalTab.getAlpha() < .15f) return "original-tab-hidden";

        String fragment = findForegroundScreen(activity);
        if (fragment != null) return fragment;

        View decor = activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            String overlay = findFullscreenNavigationOverlay((ViewGroup) decor,
                    decor.getWidth(), decor.getHeight(), 0, new int[] {300});
            if (overlay != null) return overlay;
        }
        return null;
    }

    static String foregroundSummary(Activity activity) {
        try {
            Object manager = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
            Object result = XposedHelpers.callMethod(manager, "getFragments");
            if (!(result instanceof List)) return "(none)";
            StringBuilder summary = new StringBuilder();
            for (Object fragment : (List<?>) result) {
                if (fragment == null || !bool(fragment, "isVisible")) continue;
                if (summary.length() > 0) summary.append(" | ");
                summary.append(fragment.getClass().getSimpleName());
                if (summary.length() > 280) break;
            }
            return summary.length() == 0 ? "(none)" : summary.toString();
        } catch (Throwable ignored) {
            return "(fragment manager unavailable)";
        }
    }

    private static String findForegroundScreen(Activity activity) {
        String name = activity.getClass().getName().toLowerCase(Locale.ROOT);
        if (!name.contains("mainactivity") && isSpecialScreenName(name)) {
            return "activity:" + activity.getClass().getSimpleName();
        }
        try {
            Object manager = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
            return checkFragments(manager, activity.getWindow().getDecorView(),
                    0, new int[] {80});
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String checkFragments(Object manager, View decor,
                                          int depth, int[] budget) {
        if (manager == null || depth > 4 || budget[0] <= 0) return null;
        Object result;
        try {
            result = XposedHelpers.callMethod(manager, "getFragments");
        } catch (Throwable ignored) {
            return null;
        }
        if (!(result instanceof List)) return null;
        for (Object fragment : (List<?>) result) {
            if (fragment == null || --budget[0] < 0) break;
            if (!bool(fragment, "isVisible") || !bool(fragment, "isResumed")
                    || bool(fragment, "isHidden")) continue;
            View view;
            try {
                Object value = XposedHelpers.callMethod(fragment, "getView");
                view = value instanceof View ? (View) value : null;
            } catch (Throwable ignored) {
                continue;
            }
            if (view == null || !isLargeVisibleView(view, decor)) continue;

            String type = fragment.getClass().getName().toLowerCase(Locale.ROOT);
            if (isSpecialScreenName(type)) {
                return "fragment:" + fragment.getClass().getSimpleName();
            }
            try {
                Object child = XposedHelpers.callMethod(fragment, "getChildFragmentManager");
                String nested = checkFragments(child, decor, depth + 1, budget);
                if (nested != null) return nested;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static boolean isSpecialScreenName(String name) {
        // "ReelViewer" = Instagram Stories viewer (not ClipsViewer / Reels tab).
        // Match actual profile screens, not profile-icon widgets in feed.
        return name.contains("reelviewer")
                || name.contains("storyviewer")
                || name.contains("storyplayback")
                || name.contains("userdetailfragment")
                || name.contains("profilefragment")
                || name.contains("profilescreen")
                || name.contains("profilepage")
                || name.contains("userprofile")
                || name.contains("profiletabfragment");
    }

    private static boolean isLargeVisibleView(View view, View decor) {
        if (!view.isShown() || view.getAlpha() < .15f) return false;
        if (view.getWidth() < decor.getWidth() * .65f
                || view.getHeight() < decor.getHeight() * .54f) return false;
        Rect rect = new Rect();
        return view.getGlobalVisibleRect(rect)
                && rect.width() >= decor.getWidth() * .60f
                && rect.height() >= decor.getHeight() * .40f;
    }

    private static String findFullscreenNavigationOverlay(ViewGroup group,
            int rootWidth, int rootHeight, int depth, int[] budget) {
        if (depth > 15 || budget[0] <= 0) return null;
        for (int i = group.getChildCount() - 1; i >= 0 && budget[0] > 0; i--) {
            View child = group.getChildAt(i);
            budget[0]--;
            if (child.getVisibility() != View.VISIBLE
                    || !child.isShown() || child.getAlpha() < .15f) continue;
            if (child.getWidth() < rootWidth * .65f
                    || child.getHeight() < rootHeight * .54f) {
                continue;
            }
            String id = resourceName(child).toLowerCase(Locale.ROOT);
            if (id.contains("story_viewer") || id.contains("reel_viewer")
                    || id.contains("profile_fragment")
                    || id.contains("profile_screen")) {
                return "view:" + id;
            }
            if (child instanceof ViewGroup) {
                String nested = findFullscreenNavigationOverlay((ViewGroup) child,
                        rootWidth, rootHeight, depth + 1, budget);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static boolean bool(Object target, String method) {
        try {
            return Boolean.TRUE.equals(XposedHelpers.callMethod(target, method));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String resourceName(View view) {
        if (view.getId() == View.NO_ID) return "";
        try {
            return view.getResources().getResourceEntryName(view.getId());
        } catch (Throwable ignored) {
            return "";
        }
    }
}
