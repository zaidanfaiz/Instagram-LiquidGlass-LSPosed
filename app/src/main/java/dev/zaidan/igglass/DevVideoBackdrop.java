package dev.zaidan.igglass;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Captures the *underlying video SurfaceView*, not the Instagram window nor
 * the overlaid glass. This avoids snapshotting the glass into itself, and
 * includes GPU video pixels that normal View.draw(Canvas) cannot see.
 *
 * Small 22%-scale union-strip PixelCopy, <=1 request per 1200ms, asynchronous;
 * on error or unsupported view, falls back to 100% transparent base + rim.
 * Never changes visibility of live Instagram controls for a screenshot.
 */
final class DevVideoBackdrop {
    private static final float SCALE = .22f;
    private static final long MIN_INTERVAL_MS = 1200L;

    private final DevGlassUi.Scene scene;
    private final DevRefractiveBackdrop main;
    private final DevRefractiveBackdrop search;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable captureTask = this::captureVideo;
    private boolean stopped;
    private boolean inFlight;
    private int failureCount;
    private boolean noSurfaceLogged;
    private long lastStart;

    DevVideoBackdrop(DevGlassUi.Scene scene, DevRefractiveBackdrop main,
                     DevRefractiveBackdrop search) {
        this.scene = scene;
        this.main = main;
        this.search = search;
    }

    void request(int delayMs) {
        if (stopped || failureCount >= 5 || !scene.isReelsActive()) return;
        mainHandler.removeCallbacks(captureTask);
        mainHandler.postDelayed(captureTask, Math.max(350, delayMs));
    }

    private void captureVideo() {
        if (stopped || inFlight || !scene.isReelsActive()
                || !scene.attached()
                || scene.mainPill.getVisibility() != View.VISIBLE) return;
        if (Build.VERSION.SDK_INT < 26) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastStart < MIN_INTERVAL_MS) {
            request((int)(MIN_INTERVAL_MS - (now - lastStart) + 150));
            return;
        }
        lastStart = now;

        Rect mainRect = screenRect(scene.mainPill);
        Rect searchRect = scene.searchPill == null
                ? mainRect : screenRect(scene.searchPill);
        Rect union = new Rect(mainRect);
        union.union(searchRect);
        SurfaceView source = findMatchingSurface(scene.host, union,
                new int[] {2200}, 0);
        if (source == null) {
            if (!noSurfaceLogged) {
                noSurfaceLogged = true;
                DevFileLog.note("Reels: no full-coverage SurfaceView, use clear glass");
            }
            return;
        }
        int[] surfaceXY = new int[2];
        source.getLocationOnScreen(surfaceXY);
        Rect sourceArea = new Rect(
                union.left - surfaceXY[0], union.top - surfaceXY[1],
                union.right - surfaceXY[0], union.bottom - surfaceXY[1]);
        if (sourceArea.left < 0 || sourceArea.top < 0
                || sourceArea.right > source.getWidth()
                || sourceArea.bottom > source.getHeight()) {
            // Content is not actually underneath both pills.
            return;
        }

        final Bitmap frame;
        try {
            frame = Bitmap.createBitmap(
                    Math.max(1, Math.round(union.width()*SCALE)),
                    Math.max(1, Math.round(union.height()*SCALE)),
                    Bitmap.Config.ARGB_8888);
        } catch (Throwable e) {
            DevFileLog.note("Reels PixelCopy bitmap unavailable "
                    + e.getClass().getSimpleName());
            failureCount = 5;
            return;
        }
        final Rect capturedUnion = new Rect(union);
        final Rect capturedMain = new Rect(mainRect);
        final Rect capturedSearch = new Rect(searchRect);
        inFlight = true;
        try {
            PixelCopy.request(source, sourceArea, frame, result -> {
                inFlight = false;
                try {
                    if (!stopped && scene.isReelsActive() && scene.attached()
                            && result == PixelCopy.SUCCESS) {
                        Bitmap mainCrop = crop(frame, capturedMain, capturedUnion);
                        if (mainCrop != null) main.setVideoSnapshot(mainCrop);
                        if (search != null) {
                            Bitmap searchCrop = crop(frame, capturedSearch,
                                    capturedUnion);
                            if (searchCrop != null) search.setVideoSnapshot(searchCrop);
                        }
                        failureCount = 0;
                        if (noSurfaceLogged) noSurfaceLogged = false;
                        DevFileLog.note("Reels: SurfaceView PixelCopy SUCCESS ("
                                + frame.getWidth() + "x" + frame.getHeight() + ")");
                    } else if (result != PixelCopy.SUCCESS) {
                        failureCount++;
                        if (failureCount <= 2)
                            DevFileLog.note("Reels: PixelCopy result=" + result
                                    + "; transparent fallback");
                    }
                } catch (Throwable callbackError) {
                    failureCount++;
                    DevFileLog.note("Reels: PixelCopy callback error "
                            + callbackError.getClass().getSimpleName());
                } finally {
                    if (!frame.isRecycled()) frame.recycle();
                }
            }, mainHandler);
        } catch (Throwable requestError) {
            inFlight = false;
            failureCount++;
            if (!frame.isRecycled()) frame.recycle();
            DevFileLog.note("Reels: PixelCopy request failed "
                    + requestError.getClass().getSimpleName());
        }
    }

    private static Rect screenRect(View view) {
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        return new Rect(pos[0],pos[1],
                pos[0]+view.getWidth(),pos[1]+view.getHeight());
    }

    private static Bitmap crop(Bitmap original, Rect item, Rect union) {
        int x = Math.max(0, Math.round((item.left - union.left)*SCALE));
        int y = Math.max(0, Math.round((item.top - union.top)*SCALE));
        int w = Math.min(Math.round(item.width()*SCALE), original.getWidth()-x);
        int h = Math.min(Math.round(item.height()*SCALE),original.getHeight()-y);
        if (w <= 0 || h <= 0) return null;
        Bitmap cut = Bitmap.createBitmap(original,x,y,w,h);
        return cut == original ? original.copy(Bitmap.Config.ARGB_8888,false) : cut;
    }

    private static SurfaceView findMatchingSurface(View v, Rect union,
                                                   int[] budget,int depth) {
        if (budget[0]-- <= 0 || depth > 20 || !v.isShown())
            return null;
        if (v instanceof SurfaceView) {
            Rect screen = screenRect(v);
            return screen.contains(union) ? (SurfaceView)v : null;
        }
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)v;
            // Search through the live content tree; glass shells are FrameLayouts
            // and contain no SurfaceView, so there is no sampling recursion.
            for(int i=group.getChildCount()-1; i>=0 && budget[0]>0; i--) {
                SurfaceView found = findMatchingSurface(group.getChildAt(i),
                        union,budget,depth+1);
                if(found!=null) return found;
            }
        }
        return null;
    }

    void stop() {
        stopped = true;
        mainHandler.removeCallbacks(captureTask);
    }
}
