package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import java.util.Locale;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Experimental style-only hook. Instagram's internal View hierarchy changes frequently.
 * Not a real backdrop blur or iOS refractive Liquid Glass effect.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String TARGET = "com.instagram.android";
    private static final WeakHashMap<View, Boolean> STYLED = new WeakHashMap<>();

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;
        XposedBridge.log("IGGlass: process " + lpparam.processName);
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                if (!TARGET.equals(activity.getPackageName())) return;
                View decor = activity.getWindow().getDecorView();
                for (int delay : new int[]{350, 1200, 2500}) {
                    decor.postDelayed(() -> {
                        try { locateAndStyle(decor); }
                        catch (Throwable t) { XposedBridge.log("IGGlass: " + t); }
                    }, delay);
                }
            }
        });
    }

    private static void locateAndStyle(View decor) {
        if (!(decor instanceof ViewGroup) || decor.getHeight() <= 0) return;
        ViewGroup found = findCandidate((ViewGroup) decor, decor.getHeight(), 0);
        if (found == null || STYLED.containsKey(found)) return;
        float density = found.getResources().getDisplayMetrics().density;
        boolean dark = (found.getResources().getConfiguration().uiMode & 0x30) == 0x20;
        int edge = dark ? 0x60FFFFFF : 0x5AFFFFFF;
        int top = dark ? 0xBB34363F : 0xD9FFFFFF;
        int bottom = dark ? 0x9A17191F : 0xA9E0E6F2;
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        glass.setCornerRadius(28 * density);
        glass.setStroke(Math.max(1, (int)density), edge);
        found.setBackground(glass);
        found.setElevation(10 * density);
        found.setClipToOutline(true);
        STYLED.put(found, true);
        XposedBridge.log("IGGlass: styled " + found.getClass().getName());
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
            group.getHeight() > 100 * dp || group.getHeight() < 34 * dp) return false;
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
            String key = ((desc == null ? "" : desc.toString()) + " " + id).toLowerCase(Locale.ROOT);
            if (key.contains("home") || key.contains("search") || key.contains("reels") ||
                key.contains("profile") || key.contains("explore") || key.contains("beranda") ||
                key.contains("cari") || key.contains("profil")) matches++;
        }
        return matches >= 3;
    }
}
