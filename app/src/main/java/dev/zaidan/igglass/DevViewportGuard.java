package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

/**
 * Non-invasive edge-to-edge viewport diagnostics for IG452 on Android16.
 * This does NOT change system-bar flags, orientation, Window flags, video
 * SurfaceView size, or decorFitsSystemWindows state.
 */
final class DevViewportGuard {
    private DevViewportGuard() {}

    static boolean hasLiveVideo(View container) {
        if (container == null) return false;
        int rootW = container.getWidth();
        int rootH = container.getHeight();
        return videoRecursive(container, rootW, rootH, new int[] {1300}, 0);
    }

    private static boolean videoRecursive(View node, int rootW, int rootH,
                                          int[] budget, int depth) {
        if (budget[0]-- <= 0 || depth > 18
                || node.getVisibility() != View.VISIBLE || !node.isShown()) {
            return false;
        }
        if (node instanceof SurfaceView || node instanceof TextureView) {
            Rect visible = new Rect();
            return node.getGlobalVisibleRect(visible)
                    && visible.width() >= rootW * .45f
                    && visible.height() >= rootH * .22f;
        }
        if (node instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) node;
            for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) {
                if (videoRecursive(group.getChildAt(i), rootW, rootH,
                        budget, depth + 1)) return true;
            }
        }
        return false;
    }

    static void note(Activity activity, DevGlassUi.Scene scene, String reason) {
        try {
            View decor = activity.getWindow().getDecorView();
            View host = scene.host;
            int[] xy = new int[2];
            host.getLocationOnScreen(xy);
            int[] mainXY = new int[2];
            scene.mainPill.getLocationOnScreen(mainXY);
            int navBottom = -1;
            int topInset = -1;
            WindowInsets insets = decor.getRootWindowInsets();
            if (Build.VERSION.SDK_INT >= 30 && insets != null) {
                navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                topInset = insets.getInsets(WindowInsets.Type.statusBars()).top;
            }
            DevFileLog.note("viewport [" + reason + "]"
                    + " decor=" + decor.getWidth() + "x" + decor.getHeight()
                    + " content=" + host.getWidth() + "x" + host.getHeight()
                    + " contentXY=" + xy[0] + "," + xy[1]
                    + " glassY=" + mainXY[1] + " glassBottom="
                    + (mainXY[1] + scene.mainPill.getHeight())
                    + " systemInsets(top,nav)=" + topInset + "," + navBottom
                    + " systemUiFlags=0x"
                    + Integer.toHexString(decor.getSystemUiVisibility())
                    + " windowFlags=0x"
                    + Integer.toHexString(activity.getWindow().getAttributes().flags));
        } catch (Throwable e) {
            DevFileLog.note("viewport inspect failed " + e.getClass().getSimpleName());
        }
    }
}
