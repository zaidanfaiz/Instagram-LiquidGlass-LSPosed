package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

/**
 * Profile/detail and Story/status detection. Instagram frequently navigates
 * fragments inside the same MainActivity; lifecycle hooks alone do not work.
 * Only examines fragment class names and screen geometry (no user information).
 */
final class DevScreenDetector {
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
        } catch (Throwable ignored) {
            // Some 452 flows use proprietary navigation containers.
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

    private static boolean flag(Object object, String method) {
        try {
            return Boolean.TRUE.equals(XposedHelpers.callMethod(object, method));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
