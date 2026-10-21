package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Instagram layout-only diagnostic module.
 * Does not move, hide, paint, mutate, or screenshot Instagram views.
 * Logs only class names, static resource IDs, coordinates, sizes and backgrounds.
 * Never logs usernames, captions, View text or content descriptions.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String PKG = "com.instagram.android";
    private static final int MAX_VISITED = 3000;
    private static final int MAX_LOGGED = 180;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!PKG.equals(load.packageName)) return;
        XposedBridge.log("IGNAV DIAG loaded for " + PKG);
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!PKG.equals(activity.getPackageName())) return;
                View decor = activity.getWindow().getDecorView();
                for (int delay : new int[]{1400, 4000}) {
                    decor.postDelayed(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        try { dump(activity); }
                        catch (Throwable error) {
                            XposedBridge.log("IGNAV DIAG failed: " + error.getClass().getName());
                        }
                    }, delay);
                }
            }
        });
    }

    private static void dump(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup) || root.getHeight() < 1) return;
        float density = root.getResources().getDisplayMetrics().density;
        int[] rootXY = new int[2];
        root.getLocationOnScreen(rootXY);
        int rootBottom = rootXY[1] + root.getHeight();
        int stripTop = rootBottom - Math.round(300 * density);
        int[] stats = new int[2]; // visited, logged

        XposedBridge.log("IGNAV BEGIN activity=" + activity.getClass().getName()
                + " root=" + root.getClass().getName()
                + " rootXY=" + rootXY[0] + "," + rootXY[1]
                + " size=" + root.getWidth() + "x" + root.getHeight()
                + " density=" + density
                + " bottomStripStart=" + stripTop);

        scan((ViewGroup) root, 0, stripTop, rootBottom, stats, density);
        XposedBridge.log("IGNAV END visited=" + stats[0] + " logged=" + stats[1]);
    }

    private static void scan(View view, int depth, int stripTop,
                             int rootBottom, int[] stats, float density) {
        if (stats[0]++ >= MAX_VISITED || stats[1] >= MAX_LOGGED || depth > 18) return;
        if (view.getVisibility() != View.VISIBLE) return;
        int[] p = new int[2];
        view.getLocationOnScreen(p);
        int w = view.getWidth(), h = view.getHeight();
        if (h <= 0 || w <= 0) return;
        boolean intersects = p[1] + h > stripTop && p[1] < rootBottom;
        if (!intersects) return;

        // Record groups, clickable bottom controls and bounded decorative views.
        boolean isGroup = view instanceof ViewGroup;
        boolean important = isGroup || view.isClickable()
                || (h < Math.round(110 * density) && p[1] > stripTop);
        if (important && stats[1] < MAX_LOGGED) {
            String id = "none";
            try {
                if (view.getId() != View.NO_ID)
                    id = view.getResources().getResourceEntryName(view.getId());
            } catch (Throwable ignored) { id = "unknown"; }
            Drawable drawable = view.getBackground();
            String bg = drawable == null ? "none" : drawable.getClass().getSimpleName();
            if (drawable instanceof ColorDrawable) {
                bg += "#" + String.format(Locale.US, "%08X",
                        ((ColorDrawable) drawable).getColor());
            }
            String parent = view.getParent() instanceof View
                    ? ((View) view.getParent()).getClass().getSimpleName() : "none";
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            String layout = lp == null ? "none"
                    : lp.width + "x" + lp.height;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
                layout += " margin=" + m.leftMargin + "," + m.topMargin
                        + "," + m.rightMargin + "," + m.bottomMargin;
            }
            if (lp instanceof FrameLayout.LayoutParams)
                layout += " gravity=" + ((FrameLayout.LayoutParams) lp).gravity;
            if (lp instanceof LinearLayout.LayoutParams)
                layout += " weight=" + ((LinearLayout.LayoutParams) lp).weight;

            XposedBridge.log("IGNAV d=" + depth
                    + " cls=" + view.getClass().getName()
                    + " parent=" + parent
                    + " id=" + id
                    + " xy=" + p[0] + "," + p[1]
                    + " wh=" + w + "x" + h
                    + " click=" + view.isClickable()
                    + " children=" + (isGroup ? ((ViewGroup) view).getChildCount() : 0)
                    + " bg=" + bg + " lp=" + layout);
            stats[1]++;
        }

        if (isGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (stats[0] >= MAX_VISITED || stats[1] >= MAX_LOGGED) break;
                scan(group.getChildAt(i), depth + 1, stripTop, rootBottom, stats, density);
            }
        }
    }
}
