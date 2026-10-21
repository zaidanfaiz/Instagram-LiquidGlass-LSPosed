package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

/**
 * Profile/detail and Story/status detection. Instagram frequently navigates
 * fragments inside the same MainActivity; lifecycle hooks alone do not work.
 * Only examines fragment class names and screen geometry (no user information).
 */
final class DevScreenDetector {
    private static boolean fragmentErrorLogged;
    private DevScreenDetector() {}

    static String hiddenBecause(Activity activity, View nav) {
        if (activity.isFinishing() || activity.isDestroyed()) return "closing";
        if (!activity.hasWindowFocus()) return "unfocused";
        if (nav.getVisibility() != View.VISIBLE || nav.getAlpha() < .10f) {
            return "native-tab-hidden";
        }
        if (!activity.getClass().getName().contains("InstagramMainActivity")) {
            return "secondary-activity";
        }
        try {
            Object fm = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
            String match = scan(fm, activity.getWindow().getDecorView(), 0,
                    new int[] {72});
            if (match != null) return match;
        } catch (Throwable error) {
            if (!fragmentErrorLogged) {
                fragmentErrorLogged = true;
                DevFileLog.note("fragment API inaccessible: "
                        + error.getClass().getSimpleName());
            }
            // Instagram 452 may use a proprietary main page router.
        }
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            String viewName = scanViewIds((ViewGroup) decor, decor, 0,
                    new int[] {450});
            if (viewName != null) return viewName;
        }
        return null;
    }

    static String fragmentNames(Activity activity) {
        try {
            Object fm = XposedHelpers.callMethod(activity, "getSupportFragmentManager");
            Object fragments = XposedHelpers.callMethod(fm, "getFragments");
            if (!(fragments instanceof List)) return "(none)";
            StringBuilder s = new StringBuilder();
            for (Object f : (List<?>) fragments) {
                if (f == null || !flag(f, "isVisible")) continue;
                if (s.length() != 0) s.append(" | ");
                s.append(f.getClass().getSimpleName());
                if (s.length() > 240) break;
            }
            return s.length() == 0 ? "(none)" : s.toString();
        } catch (Throwable ignored) {
            return "(fragment API unavailable)";
        }
    }

    private static String scan(Object fm, View decor, int depth, int[] remaining) {
        if (fm == null || depth > 4 || remaining[0] <= 0) return null;
        Object list;
        try {
            list = XposedHelpers.callMethod(fm, "getFragments");
        } catch (Throwable ignored) {
            return null;
        }
        if (!(list instanceof List)) return null;
        for (Object f : (List<?>) list) {
            if (f == null || --remaining[0] <= 0) break;
            if (!flag(f, "isVisible") || flag(f, "isHidden")
                    || !flag(f, "isResumed")) continue;
            View view;
            try {
                Object v = XposedHelpers.callMethod(f, "getView");
                view = v instanceof View ? (View) v : null;
            } catch (Throwable ignored) {
                view = null;
            }
            if (view == null || !isLargeForeground(view, decor)) continue;
            String name = f.getClass().getName().toLowerCase(Locale.ROOT);
            if (isDetail(name)) return "fragment:" + f.getClass().getSimpleName();
            try {
                Object child = XposedHelpers.callMethod(f, "getChildFragmentManager");
                String result = scan(child, decor, depth + 1, remaining);
                if (result != null) return result;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static boolean isDetail(String name) {
        // Instagram Stories use ReelViewer; Reels tab uses ClipsViewer.
        return name.contains("reelviewer") || name.contains("storyviewer")
                || name.contains("storyplayback")
                || name.contains("userdetailfragment")
                || name.contains("userprofilefragment")
                || name.contains("profilefragment")
                || name.contains("profiletabfragment");
    }

    private static boolean isLargeForeground(View view, View decor) {
        if (!view.isShown() || view.getAlpha() < .1f) return false;
        if (view.getWidth() < decor.getWidth() * .58f
                || view.getHeight() < decor.getHeight() * .48f) return false;
        Rect bounds = new Rect();
        return view.getGlobalVisibleRect(bounds)
                && bounds.width() > decor.getWidth() * .50f
                && bounds.height() > decor.getHeight() * .36f;
    }

    /** Only consider nearly full-page visible navigation containers. */
    private static String scanViewIds(ViewGroup group, View decor,
                                      int depth, int[] budget) {
        if (depth > 17 || budget[0] <= 0) return null;
        for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) {
            View v = group.getChildAt(i);
            budget[0]--;
            if (v.getVisibility() != View.VISIBLE || v.getAlpha() < .10f
                    || v.getWidth() <= 0 || v.getHeight() <= 0) continue;
            String id = DevGlassUi.resourceName(v).toLowerCase(Locale.ROOT);
            boolean special = id.contains("story_viewer")
                    || id.contains("reel_viewer")
                    || id.contains("user_profile_fragment")
                    || id.contains("profile_fragment")
                    || id.contains("profile_tab_content")
                    || id.contains("user_profile_container");
            if (special && isLargeForeground(v, decor)) {
                return "view:" + id;
            }
            if (v instanceof ViewGroup) {
                String found = scanViewIds((ViewGroup) v, decor,
                        depth + 1, budget);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean flag(Object object, String method) {
        try {
            return Boolean.TRUE.equals(XposedHelpers.callMethod(object, method));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
