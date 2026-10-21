package dev.zaidan.igglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Locale;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * v0.3 frosted-glass beta for Instagram's 5-tab navigation.
 *
 * It relocates the ORIGINAL navigation buttons and draws a sampled, blurred
 * approximation of content behind them. This is not the native iOS shader.
 * Instagram may change its private hierarchy in any release.
 */
public final class InstagramGlassHook implements IXposedHookLoadPackage {
    private static final String PACKAGE = "com.instagram.android";
    private static final WeakHashMap<View, Boolean> STYLED = new WeakHashMap<>();
    private static final int HORIZONTAL_MARGIN_DP = 14;
    private static final int BOTTOM_MARGIN_DP = 14;
    private static final long SAMPLE_INTERVAL_MS = 900L;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) {
        if (!PACKAGE.equals(load.packageName)) return;
        XposedBridge.log("IGGlass v0.3: " + load.processName);
        XposedHelpers.findAndHookMethod(Activity.class, "onResume",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        if (!PACKAGE.equals(activity.getPackageName())) return;
                        View decor = activity.getWindow().getDecorView();
                        for (int delay : new int[] {300, 1100, 2500}) {
                            decor.postDelayed(() -> {
                                if (activity.isFinishing() || activity.isDestroyed()) return;
                                try { install(activity); }
                                catch (Throwable e) { XposedBridge.log("IGGlass: " + e); }
                            }, delay);
                        }
                    }
                });
    }

    private static void install(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup) || decor.getHeight() == 0) return;
        ViewGroup nav = findCandidate((ViewGroup) decor, decor.getHeight(), 0);
        if (nav == null || STYLED.containsKey(nav)) return;

        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)
                || !(nav.getParent() instanceof ViewGroup)) {
            fallback(nav);
            STYLED.put(nav, true);
            return;
        }

        FrameLayout host = (FrameLayout) content;
        ViewGroup parent = (ViewGroup) nav.getParent();
        if (!descendsFrom(parent, host) || isInsideCollection(parent)) {
            fallback(nav);
            STYLED.put(nav, true);
            return;
        }
        int height = nav.getHeight();
        if (height < dp(nav, 34) || height > dp(nav, 105)) return;

        int oldIndex = parent.indexOfChild(nav);
        ViewGroup.LayoutParams oldParams = nav.getLayoutParams();
        boolean dark = (nav.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        FrameLayout shell = new FrameLayout(activity);
        shell.setClipChildren(true);
        shell.setClipToPadding(true);
        shell.setElevation(dp(nav, 16));
        shell.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        dp(view, 33));
            }
        });
        shell.setClipToOutline(true);

        ImageView sampledBackdrop = new ImageView(activity);
        sampledBackdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        sampledBackdrop.setClickable(false);
        if (Build.VERSION.SDK_INT >= 31) {
            sampledBackdrop.setRenderEffect(RenderEffect.createBlurEffect(
                    dp(nav, 10), dp(nav, 10), Shader.TileMode.CLAMP));
        }
        shell.addView(sampledBackdrop, new FrameLayout.LayoutParams(-1, -1));

        View frost = new View(activity);
        GradientDrawable tint = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                dark ? new int[] {0x6031384A, 0x4A090C18}
                     : new int[] {0x8AFFFFFF, 0x50F6FAFF});
        tint.setCornerRadius(dp(nav, 33));
        frost.setBackground(tint);
        frost.setClickable(false);
        shell.addView(frost, new FrameLayout.LayoutParams(-1, -1));

        View outline = new View(activity);
        GradientDrawable rim = new GradientDrawable();
        rim.setColor(Color.TRANSPARENT);
        rim.setCornerRadius(dp(nav, 33));
        rim.setStroke(dp(nav, 1), dark ? 0xA0FFFFFF : 0xBFFFFFFF);
        outline.setBackground(rim);
        outline.setClickable(false);

        FrameLayout.LayoutParams floatParams = new FrameLayout.LayoutParams(
                -1, height, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        floatParams.leftMargin = dp(nav, HORIZONTAL_MARGIN_DP);
        floatParams.rightMargin = dp(nav, HORIZONTAL_MARGIN_DP);
        floatParams.bottomMargin = dp(nav, BOTTOM_MARGIN_DP);

        try {
            parent.removeView(nav);
            host.addView(shell, floatParams);
            shell.addView(nav, new FrameLayout.LayoutParams(-1, -1));
            // Do not clear icon/badge backgrounds; only remove full-width
            // opaque layers that otherwise obscure the frosted backdrop.
            clearOpaqueFullSizeWrappers(nav, nav.getWidth(), height, 0);
            shell.addView(outline, new FrameLayout.LayoutParams(-1, -1));

            // Collapse only vacant small placeholders. Never hide a container
            // with feed content, and never alter Instagram's click listeners.
            if (parent.getChildCount() == 0
                    && parent.getHeight() > 0
                    && parent.getHeight() <= dp(nav, 110)
                    && parent.getParent() instanceof ViewGroup) {
                parent.setVisibility(View.GONE);
            }
            host.bringChildToFront(shell);
            STYLED.put(nav, true);
            shell.post(() -> updateBackdrop(activity, host, shell, sampledBackdrop));
            XposedBridge.log("IGGlass v0.3: installed frosted floating tab bar");
        } catch (Throwable e) {
            XposedBridge.log("IGGlass: overlay failed, restoring: " + e);
            try {
                if (nav.getParent() instanceof ViewGroup) {
                    ((ViewGroup) nav.getParent()).removeView(nav);
                }
                if (shell.getParent() == host) host.removeView(shell);
                if (nav.getParent() == null) {
                    parent.addView(nav, Math.max(0,
                            Math.min(oldIndex, parent.getChildCount())), oldParams);
                }
            } catch (Throwable restoreError) {
                XposedBridge.log("IGGlass: restore: " + restoreError);
            }
            fallback(nav);
            STYLED.put(nav, true);
        }
    }

    private static void clearOpaqueFullSizeWrappers(ViewGroup nav,
                                                     int barWidth, int barHeight,
                                                     int depth) {
        if (depth > 3) return;
        nav.setBackground(null);
        for (int i = 0; i < nav.getChildCount(); i++) {
            View child = nav.getChildAt(i);
            if (!(child instanceof ViewGroup)) continue;
            if (child.getWidth() >= barWidth * .82f
                    && child.getHeight() >= barHeight * .82f) {
                clearOpaqueFullSizeWrappers((ViewGroup) child,
                        barWidth, barHeight, depth + 1);
            }
        }
    }

    /**
     * Renders only a small region of the host beneath the floating bar into
     * a low-resolution bitmap. Hiding the shell *during the draw call* avoids
     * capturing the bar itself. It never requests device screenshot permission.
     *
     * A blank Instagram bottom spacer does not reveal actual feed pixels;
     * in that case sample the strip immediately above as a visual fallback.
     */
    private static void updateBackdrop(Activity activity, FrameLayout host,
                                       FrameLayout shell, ImageView background) {
        if (activity.isFinishing() || activity.isDestroyed()
                || shell.getParent() != host || !shell.isAttachedToWindow()) return;
        if (shell.getWidth() <= 0 || shell.getHeight() <= 0) {
            shell.postDelayed(() -> updateBackdrop(activity, host, shell, background),
                    SAMPLE_INTERVAL_MS);
            return;
        }
        if (activity.hasWindowFocus()) {
            try {
                int[] navXY = new int[2];
                int[] hostXY = new int[2];
                shell.getLocationOnScreen(navXY);
                host.getLocationOnScreen(hostXY);
                int x = navXY[0] - hostXY[0];
                int y = navXY[1] - hostXY[1];
                int w = shell.getWidth(), h = shell.getHeight();

                Bitmap behind = sample(host, shell, x, y, w, h);
                if (behind != null && textureScore(behind) < 11.0) {
                    Bitmap upper = sample(host, shell, x, y - h, w, h);
                    if (upper != null) {
                        if (textureScore(upper) > textureScore(behind) + 5.0) {
                            behind.recycle();
                            behind = upper;
                        } else {
                            upper.recycle();
                        }
                    }
                }
                if (behind != null) background.setImageBitmap(behind);
            } catch (Throwable error) {
                XposedBridge.log("IGGlass: sampling unsupported: " + error);
            }
        }
        shell.postDelayed(() -> updateBackdrop(activity, host, shell, background),
                SAMPLE_INTERVAL_MS);
    }

    private static Bitmap sample(FrameLayout host, FrameLayout shell,
                                 int x, int y, int w, int h) {
        // Approx. quarter-resolution: low overhead and smooth blur edges.
        final float scale = .30f;
        Bitmap bitmap = Bitmap.createBitmap(
                Math.max(1, (int) (w * scale)),
                Math.max(1, (int) (h * scale)),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0xFF13151D);
        canvas.scale(scale, scale);
        canvas.translate(-x, -y);
        canvas.clipRect(x, y, x + w, y + h);
        int previous = shell.getVisibility();
        try {
            shell.setVisibility(View.INVISIBLE);
            host.draw(canvas);
            return bitmap;
        } catch (Throwable e) {
            bitmap.recycle();
            return null;
        } finally {
            shell.setVisibility(previous);
        }
    }

    private static double textureScore(Bitmap bitmap) {
        int count = 0;
        double sum = 0.0, sum2 = 0.0;
        for (int y = 0; y < bitmap.getHeight(); y += Math.max(1, bitmap.getHeight()/7)) {
            for (int x = 0; x < bitmap.getWidth(); x += Math.max(1, bitmap.getWidth()/11)) {
                int pixel = bitmap.getPixel(x, y);
                double luma = .2126 * Color.red(pixel)
                        + .7152 * Color.green(pixel) + .0722 * Color.blue(pixel);
                sum += luma;
                sum2 += luma * luma;
                count++;
            }
        }
        if (count == 0) return 0.0;
        return Math.sqrt(Math.max(0.0, sum2/count - (sum/count)*(sum/count)));
    }

    private static void fallback(ViewGroup nav) {
        nav.setPivotX(nav.getWidth() / 2f);
        nav.setPivotY(nav.getHeight() / 2f);
        nav.setScaleX(.93f);
        nav.setTranslationY(-dp(nav, 7));
    }

    private static boolean descendsFrom(View child, ViewGroup ancestor) {
        View current = child;
        while (current != null) {
            if (current == ancestor) return true;
            current = current.getParent() instanceof View
                    ? (View) current.getParent() : null;
        }
        return false;
    }

    private static boolean isInsideCollection(ViewGroup group) {
        View current = group;
        for (int i = 0; i < 5 && current != null; i++) {
            String name = current.getClass().getName();
            if (name.contains("RecyclerView") || name.contains("ViewPager")) return true;
            current = current.getParent() instanceof View
                    ? (View) current.getParent() : null;
        }
        return false;
    }

    private static int dp(View view, float size) {
        return Math.round(size * view.getResources().getDisplayMetrics().density);
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
        int[] pos = new int[2];
        group.getLocationOnScreen(pos);
        float density = group.getResources().getDisplayMetrics().density;
        if (pos[1] < screenHeight * .72f
                || group.getHeight() > 105 * density
                || group.getHeight() < 34 * density) return false;
        int children = group.getChildCount();
        if (children < 3 || children > 8) return false;
        int match = 0;
        for (int i = 0; i < children; i++) {
            View child = group.getChildAt(i);
            String id = "";
            try {
                if (child.getId() != View.NO_ID)
                    id = child.getResources().getResourceEntryName(child.getId());
            } catch (Throwable ignored) { }
            CharSequence label = child.getContentDescription();
            String key = ((label == null ? "" : label.toString()) + " " + id)
                    .toLowerCase(Locale.ROOT);
            if (key.contains("home") || key.contains("search") || key.contains("reels")
                    || key.contains("profile") || key.contains("explore")
                    || key.contains("beranda") || key.contains("cari")
                    || key.contains("profil")) match++;
        }
        return match >= 3;
    }
}
