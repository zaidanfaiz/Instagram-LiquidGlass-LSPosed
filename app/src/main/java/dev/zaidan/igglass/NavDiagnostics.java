package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedBridge;

/**
 * Read-only runtime inspection for Instagram 452's bottom navigation.
 * Logs class names, resource IDs, geometry, backgrounds and system-bar insets.
 * Never logs view text, image pixels, account data or accessibility labels.
 */
final class NavDiagnostics {
    private static final String PREFIX = "IGGlass-DIAG: ";

    private NavDiagnostics() {}

    static void dump(Activity activity, View tabRow, View oldParent, String stage) {
        try {
            final View decor = activity.getWindow().getDecorView();
            final int[] xy = new int[2];
            decor.getLocationOnScreen(xy);
            final int bottom = xy[1] + decor.getHeight();
            final int width = decor.getWidth();

            log("BEGIN " + stage + " activity=" + activity.getClass().getName()
                    + " decor=" + width + "x" + decor.getHeight()
                    + " bottom=" + bottom);
            log("system navColor=" + color(activity.getWindow().getNavigationBarColor())
                    + " decorFits/flags=" + Integer.toHexString(
                            activity.getWindow().getAttributes().flags)
                    + " systemUiVisibility=" + Integer.toHexString(
                            decor.getSystemUiVisibility()));
            if (Build.VERSION.SDK_INT >= 30) {
                WindowInsets insets = decor.getRootWindowInsets();
                if (insets != null) {
                    android.graphics.Insets nav = insets.getInsets(
                            WindowInsets.Type.navigationBars());
                    android.graphics.Insets gesture = insets.getInsets(
                            WindowInsets.Type.systemGestures());
                    log("system navigationInsets=" + nav.left + ","
                            + nav.top + "," + nav.right + "," + nav.bottom
                            + " gestureInsets=" + gesture.left + ","
                            + gesture.top + "," + gesture.right + ","
                            + gesture.bottom);
                }
            }
            log("NAV " + describe(tabRow));
            log("OLD PARENT " + describe(oldParent));
            log("former-parent chain (up to 6 levels):");
            View current = oldParent;
            for (int i = 0; i < 6 && current != null; i++) {
                log("  [" + i + "] " + describe(current));
                if (current instanceof ViewGroup) {
                    ViewGroup g = (ViewGroup) current;
                    log("      children=" + g.getChildCount());
                    int limit = Math.min(9, g.getChildCount());
                    for (int j = 0; j < limit; j++) {
                        log("      child[" + j + "] " + describe(g.getChildAt(j)));
                    }
                }
                current = current.getParent() instanceof View
                        ? (View) current.getParent() : null;
            }
            log("other bottom-wide layers (up to 45):");
            int[] budget = new int[] {360, 45};
            if (decor instanceof ViewGroup) {
                inspectBottom((ViewGroup) decor, bottom, width, 0, budget);
            }
            log("END " + stage);
        } catch (Throwable e) {
            log("dump failed: " + e.getClass().getSimpleName()
                    + " " + e.getMessage());
        }
    }

    private static void inspectBottom(ViewGroup root, int screenBottom,
                                      int screenWidth, int depth, int[] budget) {
        if (depth >= 13 || budget[0] <= 0 || budget[1] <= 0) return;
        for (int i = 0; i < root.getChildCount() && budget[0] > 0
                && budget[1] > 0; i++) {
            View child = root.getChildAt(i);
            budget[0]--;
            if (child.getVisibility() != View.VISIBLE || child.getWidth() <= 0
                    || child.getHeight() <= 0) continue;
            int[] pos = new int[2];
            child.getLocationOnScreen(pos);
            int childBottom = pos[1] + child.getHeight();
            float density = child.getResources().getDisplayMetrics().density;
            boolean nearBottom = childBottom >= screenBottom - (int) (150 * density);
            boolean wide = child.getWidth() >= screenWidth * .65f;
            boolean shallow = child.getHeight() <= 165 * density;
            if (nearBottom && wide && shallow) {
                log("  depth=" + (depth + 1) + " " + describe(child));
                budget[1]--;
            }
            if (child instanceof ViewGroup && nearBottom) {
                inspectBottom((ViewGroup) child, screenBottom, screenWidth,
                        depth + 1, budget);
            }
        }
    }

    private static String describe(View view) {
        if (view == null) return "(null)";
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        String name = "(no-id)";
        try {
            if (view.getId() != View.NO_ID) {
                name = view.getResources().getResourceEntryName(view.getId());
            }
        } catch (Throwable ignored) {
            name = "id=0x" + Integer.toHexString(view.getId());
        }
        Drawable bg = view.getBackground();
        String background = bg == null ? "none" : bg.getClass().getSimpleName();
        if (bg instanceof ColorDrawable) {
            background += "(" + color(((ColorDrawable) bg).getColor()) + ")";
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        String layout = lp == null ? "none" : lp.width + "x" + lp.height;
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            layout += " margins=" + m.leftMargin + "," + m.topMargin
                    + "," + m.rightMargin + "," + m.bottomMargin;
        }
        return view.getClass().getName() + " #" + name
                + " xy=" + pos[0] + "," + pos[1]
                + " wh=" + view.getWidth() + "x" + view.getHeight()
                + " measured=" + view.getMeasuredWidth() + "x"
                + view.getMeasuredHeight()
                + " visibility=" + view.getVisibility()
                + " alpha=" + view.getAlpha()
                + " clickable=" + view.isClickable()
                + " elevation=" + view.getElevation()
                + " background=" + background
                + " lp=" + layout;
    }

    private static String color(int value) {
        return String.format(java.util.Locale.ROOT, "#%08X", value);
    }

    private static void log(String line) {
        XposedBridge.log(PREFIX + line);
    }
}
