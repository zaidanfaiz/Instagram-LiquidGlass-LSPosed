package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    static String hiddenBecause(Activity activity, View nav, View formerParent) {
        if (activity.isFinishing() || activity.isDestroyed()) return "closing";
        if (!activity.hasWindowFocus()) return "unfocused";
        // Because the real tab_bar was relocated into a new top-level shell,
        // it no longer inherits visibility from its original IG parent.
        // Respect Instagram hiding that original navigation container.
        if (formerParent != null && (!formerParent.isShown()
                || formerParent.getAlpha() < .1f)) {
            return "instagram-parent-hidden";
        }
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
                    new int[] {700});
            if (viewName != null) return viewName;
            String page = screenSignature((ViewGroup) decor);
            if (page != null) return page;
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

    /**
     * Instagram 452 does not expose a standard support fragment manager.
     * Detect the visible page from non-sensitive resource entry names only.
     * Require multiple independent markers to avoid hiding feed/Reels when
     * an embedded recommendation tile also mentions "profile".
     */
    private static String screenSignature(ViewGroup decor) {
        int[] found = new int[12]; // 5 resource markers + fixed Instagram UI label signatures
        StringBuilder report = new StringBuilder();
        collectSignature(decor, 0, new int[] {1250}, found, report);
        boolean profile = (found[0] > 0 && (found[1] > 0 || found[2] > 0))
                || (found[1] > 0 && found[2] > 0)
                || ((found[5] + found[6] + found[7]) >= 2);
        boolean inbox = (found[3] > 0 && found[4] > 0)
                || ((found[8] + found[9] + found[10] + found[11]) >= 2);
        if (inbox) {
            DevFileLog.note("screen signature direct inbox: " + report);
            return "native-direct-inbox";
        }
        if (profile) {
            DevFileLog.note("screen signature profile detail: " + report);
            return "native-profile-detail";
        }
        return null;
    }

    private static void collectSignature(ViewGroup group, int depth,
                                          int[] budget, int[] found,
                                          StringBuilder report) {
        if (depth > 17 || budget[0] <= 0) return;
        for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) {
            View child = group.getChildAt(i);
            budget[0]--;
            if (!child.isShown() || child.getAlpha() < .1f
                    || child.getWidth() == 0 || child.getHeight() == 0) continue;
            String key = DevGlassUi.resourceName(child)
                    .toLowerCase(Locale.ROOT);
            int category = -1;
            if (key.contains("profile_header")
                    || key.contains("user_profile_header")) category = 0;
            else if (key.contains("profile_grid")
                    || key.contains("profile_tab_content")
                    || key.contains("profile_media_grid")) category = 1;
            else if (key.contains("edit_profile")
                    || key.contains("profile_edit_button")) category = 2;
            else if (key.contains("direct_inbox")
                    || key.contains("direct_thread_list")
                    || key.contains("direct_message_list")) category = 3;
            else if (key.contains("inbox_tab")
                    || key.contains("direct_inbox_header")
                    || key.contains("direct_inbox_list")
                    || key.contains("message_inbox")) category = 4;
            // Only compare against fixed, public Instagram UI labels; never
            // store message text, handles or profile descriptions in logs.
            // Require markers near the header to avoid feed caption matches.
            if (child instanceof TextView) {
                CharSequence displayed = ((TextView) child).getText();
                // Ignore long/private text; only tiny fixed UI labels.
                if (displayed != null && displayed.length() <= 44) {
                String label = displayed.toString().trim().toLowerCase(Locale.ROOT);
                int[] loc = new int[2];
                child.getLocationOnScreen(loc);
                if (loc[1] >= 0 && loc[1] < group.getRootView().getHeight() * .60f) {
                    if ("edit profil".equals(label)) found[5]++;
                    else if ("bagikan profil".equals(label)) found[6]++;
                    else if ("dasbor anda".equals(label)) found[7]++;
                    else if ("filter".equals(label)) found[8]++;
                    else if ("utama".equals(label)) found[9]++;
                    else if ("permintaan".equals(label)) found[10]++;
                    else if (label.contains("cari atau tanya meta ai")) found[11]++;
                }
                }
            }
            if (category >= 0) {
                found[category]++;
                if (report.length() < 170) {
                    if (report.length() != 0) report.append(",");
                    report.append(key);
                }
            }
            if (child instanceof ViewGroup) {
                collectSignature((ViewGroup) child, depth + 1,
                        budget, found, report);
            }
        }
    }

    private static boolean flag(Object object, String method) {
        try {
            return Boolean.TRUE.equals(XposedHelpers.callMethod(object, method));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
