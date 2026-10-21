package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * Optional, rate-limited frosted snapshot of the content behind BOTH pills.
 *
 * Unlike old v0.9 this captures the small union strip ONCE (not twice) and
 * only after interaction/scroll has settled. At most one capture per 1200ms;
 * there is no Choreographer frame callback and no 12fps full-content capture.
 * A slow draw disables future captures. AGSL highlights remain independent.
 */
final class DevGlassBackdrop implements ViewTreeObserver.OnScrollChangedListener {
    private static final float SCALE = .22f;
    private static final long MIN_CAPTURE_MS = 1200L;
    private static final long TOO_SLOW_MS = 38L;

    private final Activity activity;
    private final DevGlassUi.Scene scene;
    private final FrameLayout host;
    private final DevRefractiveBackdrop mainImage;
    private final DevRefractiveBackdrop searchImage;
    private final DevVideoBackdrop videoCapture;
    private final Runnable refresh = this::capture;
    private boolean stopped;
    private boolean slowDevice;
    private long lastCaptureAt;
    private boolean started;

    DevGlassBackdrop(Activity activity, DevGlassUi.Scene scene) {
        this.activity = activity;
        this.scene = scene;
        this.host = scene.host;
        this.mainImage = imageFor(activity, scene.mainPill, scene.dark);
        this.searchImage = scene.searchPill == null ? null :
                imageFor(activity, scene.searchPill, scene.dark);
        this.videoCapture = new DevVideoBackdrop(scene,mainImage,searchImage);
    }

    private static DevRefractiveBackdrop imageFor(Activity activity,
                                                   FrameLayout shell,
                                                   boolean dark) {
        DevRefractiveBackdrop image = new DevRefractiveBackdrop(activity, dark);
        // The image stays behind the semi-translucent material, specular rim,
        // moving selection lens and original Instagram buttons.
        shell.addView(image, 0, new FrameLayout.LayoutParams(-1, -1));
        return image;
    }

    void start() {
        if (started || stopped) return;
        started = true;
        ViewTreeObserver observer = host.getViewTreeObserver();
        if (observer.isAlive()) observer.addOnScrollChangedListener(this);
        request(700);
    }

    void request(int delayMs) {
        if (stopped || slowDevice || !started) return;
        if (scene.isReelsActive()) {
            host.removeCallbacks(refresh);
            videoCapture.request(delayMs);
            return;
        }
        host.removeCallbacks(refresh);
        host.postDelayed(refresh, delayMs);
    }

    @Override public void onScrollChanged() {
        request(780);
    }

    private void capture() {
        if (stopped || slowDevice || !scene.attached()
                || scene.isReelsActive()
                || scene.mainPill.getVisibility() != View.VISIBLE
                || activity.isFinishing() || activity.isDestroyed()
                || !activity.hasWindowFocus()) return;

        long now = SystemClock.uptimeMillis();
        if (now - lastCaptureAt < MIN_CAPTURE_MS) {
            request((int) (MIN_CAPTURE_MS - (now - lastCaptureAt) + 100));
            return;
        }
        lastCaptureAt = now;

        int[] hostXY = new int[2];
        int[] mainXY = new int[2];
        host.getLocationOnScreen(hostXY);
        scene.mainPill.getLocationOnScreen(mainXY);

        int mainX = mainXY[0] - hostXY[0];
        int mainY = mainXY[1] - hostXY[1];
        int mainW = scene.mainPill.getWidth();
        int mainH = scene.mainPill.getHeight();
        if (mainW < 20 || mainH < 20) return;
        int sx = mainX, sy = mainY, ex = mainX + mainW, ey = mainY + mainH;
        int searchX = 0, searchY = 0, searchW = 0, searchH = 0;
        if (scene.searchPill != null) {
            int[] searchXY = new int[2];
            scene.searchPill.getLocationOnScreen(searchXY);
            searchX = searchXY[0] - hostXY[0];
            searchY = searchXY[1] - hostXY[1];
            searchW = scene.searchPill.getWidth();
            searchH = scene.searchPill.getHeight();
            sx = Math.min(sx, searchX);
            sy = Math.min(sy, searchY);
            ex = Math.max(ex, searchX + searchW);
            ey = Math.max(ey, searchY + searchH);
        }
        int w = ex - sx, h = ey - sy;
        if (w < 20 || h < 20 || w > host.getWidth() || h > host.getHeight()) {
            return;
        }

        long began = SystemClock.uptimeMillis();
        Bitmap combined = null;
        try {
            combined = Bitmap.createBitmap(
                    Math.max(1, Math.round(w * SCALE)),
                    Math.max(1, Math.round(h * SCALE)), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(combined);
            // Bitmap starts fully transparent. NEVER put opaque fake dark/white
            // fallback pixels under a glass shader (especially on Reels).
            canvas.scale(SCALE, SCALE);
            canvas.translate(-sx, -sy);
            canvas.clipRect(sx, sy, ex, ey);

            if (host.getBackground() != null) host.getBackground().draw(canvas);
            for (int i = 0; i < host.getChildCount(); i++) {
                View child = host.getChildAt(i);
                if (child == scene.mainPill || child == scene.searchPill
                        || child.getVisibility() != View.VISIBLE) continue;
                int checkpoint = canvas.save();
                canvas.translate(child.getLeft() - host.getScrollX(),
                        child.getTop() - host.getScrollY());
                canvas.concat(child.getMatrix());
                child.draw(canvas);
                canvas.restoreToCount(checkpoint);
            }
            Bitmap main = section(combined, mainX - sx, mainY - sy, mainW, mainH);
            if (main != null) mainImage.setSnapshot(main);
            if (searchImage != null) {
                Bitmap search = section(combined, searchX - sx,
                        searchY - sy, searchW, searchH);
                if (search != null) searchImage.setSnapshot(search);
            }
            long duration = SystemClock.uptimeMillis() - began;
            if (duration > TOO_SLOW_MS) {
                slowDevice = true;
                DevFileLog.note("frost: capture took " + duration
                        + "ms; further captures disabled to prevent lag");
            } else {
                DevFileLog.note("frost: single combined strip captured in "
                        + duration + "ms; cache active");
            }
        } catch (Throwable e) {
            slowDevice = true;
            DevFileLog.note("frost: capture disabled "
                    + e.getClass().getSimpleName());
        } finally {
            if (combined != null && !combined.isRecycled()) combined.recycle();
        }
    }

    private static Bitmap section(Bitmap original, int left, int top,
                                   int width, int height) {
        int l = Math.max(0, Math.round(left * SCALE));
        int t = Math.max(0, Math.round(top * SCALE));
        if (l >= original.getWidth() || t >= original.getHeight()) return null;
        int rw = Math.min(Math.max(1, Math.round(width * SCALE)),
                original.getWidth() - l);
        int rh = Math.min(Math.max(1, Math.round(height * SCALE)),
                original.getHeight() - t);
        if (rw <= 0 || rh <= 0) return null;
        Bitmap cropped = Bitmap.createBitmap(original, l, t, rw, rh);
        // Full-size crops may be returned as the original bitmap itself.
        // The shared strip is recycled after both crops are installed.
        return cropped == original
                ? original.copy(Bitmap.Config.ARGB_8888, false) : cropped;
    }

    void stop() {
        if (stopped) return;
        stopped = true;
        videoCapture.stop();
        host.removeCallbacks(refresh);
        ViewTreeObserver observer = host.getViewTreeObserver();
        if (observer.isAlive()) observer.removeOnScrollChangedListener(this);
    }
}
