package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * r18 EXPERIMENTAL VIDEO GLASS.
 *
 * PixelCopy reads a tiny rectangle DIRECTLY from the selected visible
 * SurfaceView underlying Reels. It NEVER invokes SurfaceView.draw(),
 * forces a View relayout, grabs the whole display, or mutates the player.
 * Async PixelCopy requests, one in flight, cap capture work to ~2 fps.
 *
 * A missing/inaccessible surface (TextureView, DRM, etc.) means fully
 * transparent r10 rim-only fallback. NEVER replace a failed copy with a
 * white, black or full-opacity placeholder.
 */
final class DevVideoGlassCapture {
    private static final long INTERVAL_MS=470L;
    private static final float SCALE=.30f;
    private static final float OPACITY=.76f;
    private static final int MAX_FAILURES=7;
    private final Activity activity;
    private final DevGlassUi.Scene scene;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ImageView mainImage;
    private final ImageView searchImage;
    private final Runnable tick=this::capture;
    private HandlerThread worker;
    private Handler copyHandler;
    private boolean enabled;
    private boolean destroyed;
    private boolean pending;
    private int generation;
    private int failures;
    private int successes;
    private long lastCapture;
    private boolean warnedMissing;
    private boolean disabledThisReels;

    DevVideoGlassCapture(Activity activity, DevGlassUi.Scene scene) {
        this.activity=activity;
        this.scene=scene;
        mainImage=addOpticalVideoLayer(activity,scene.mainPill);
        searchImage=scene.searchPill==null ? null
                : addOpticalVideoLayer(activity,scene.searchPill);
        DevFileLog.note("r18 video-glass installed: SurfaceView PixelCopy"
                +" 30% image scale; 2fps MAX; no fullscreen copy");
    }

    private static ImageView addOpticalVideoLayer(Activity activity,
                                                  FrameLayout capsule) {
        ImageView layer=new ImageView(activity);
        layer.setBackground(null);
        layer.setImageDrawable(null);
        layer.setAlpha(0f);
        layer.setClickable(false);
        layer.setFocusable(false);
        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        layer.setScaleType(ImageView.ScaleType.FIT_XY);
        layer.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        if(Build.VERSION.SDK_INT>=31) {
            float radius=9f*activity.getResources().getDisplayMetrics().density;
            layer.setRenderEffect(RenderEffect.createBlurEffect(
                    radius,radius, Shader.TileMode.CLAMP));
        }
        // r10 rim is the other passive child; at index 1 the video is
        // BEHIND rim highlights, active lens, icons and swipe shields.
        capsule.addView(layer,Math.min(1,capsule.getChildCount()),
                new FrameLayout.LayoutParams(-1,-1));
        return layer;
    }

    void setEnabled(boolean on) {
        boolean allow=on && !destroyed && scene.attached()
                && scene.mainPill.getVisibility()==View.VISIBLE
                && scene.isReelsActive() && activity.hasWindowFocus()
                && !activity.isFinishing() && !activity.isDestroyed();
        if(!allow && !enabled) {
            // Retry is permitted after leaving Reels and returning later.
            disabledThisReels=false;
            return;
        }
        if(allow==enabled)return;
        if(allow && disabledThisReels) return;
        generation++;
        if(!allow) disabledThisReels=false;
        enabled=allow;
        mainHandler.removeCallbacks(tick);
        if(!enabled) {
            hide(mainImage);hide(searchImage);
            DevFileLog.note("r18 video-glass suspended, clear rim fallback");
            return;
        }
        if(worker==null) {
            worker=new HandlerThread("IGGlass-video-copy");
            worker.start();
            copyHandler=new Handler(worker.getLooper());
        }
        warnedMissing=false;
        failures=0;
        DevFileLog.note("r18 video-glass enabled on Reels");
        schedule(230);
    }

    private void schedule(long delay) {
        if(!enabled||destroyed)return;
        mainHandler.removeCallbacks(tick);
        mainHandler.postDelayed(tick,Math.max(60,delay));
    }

    private void capture() {
        if(!enabled||destroyed||pending||!scene.attached()
                ||scene.mainPill.getVisibility()!=View.VISIBLE
                ||!scene.isReelsActive()||!activity.hasWindowFocus())return;
        long now=SystemClock.uptimeMillis();
        if(now-lastCapture<INTERVAL_MS){
            schedule(INTERVAL_MS-(now-lastCapture)+12);
            return;
        }
        Rect screenRect=screenRect(scene.mainPill);
        if(searchImage!=null&&scene.searchPill.getVisibility()==View.VISIBLE)
            screenRect.union(screenRect(scene.searchPill));
        if(screenRect.width()<50||screenRect.height()<20
                ||screenRect.width()>2100||screenRect.height()>450) {
            schedule(750);return;
        }
        SurfaceView surface=findSurface(scene.host,screenRect);
        if(surface==null || !surface.getHolder().getSurface().isValid()) {
            if(!warnedMissing) {
                DevFileLog.note("r18 video-glass: no fully covering, valid"
                        +" SurfaceView; rim fallback (TextureView unsupported)");
                warnedMissing=true;
            }
            hide(mainImage);hide(searchImage);
            schedule(1200);
            return;
        }
        warnedMissing=false;
        int[] surfacePos=new int[2];
        surface.getLocationOnScreen(surfacePos);
        Rect sourceRect=new Rect(screenRect);
        sourceRect.offset(-surfacePos[0],-surfacePos[1]);
        // The native Reel can stop 20-40px above Android's gesture area.
        // Capture the intersection instead of demanding 100% coverage:
        // PixelCopy will upscale its narrow video band to the glass area.
        // This is refraction-like edge extension, not a fake color fill.
        Rect clipped=new Rect(sourceRect);
        if(!clipped.intersect(0,0,surface.getWidth(),surface.getHeight())
                || clipped.width()<screenRect.width()*.86f
                || clipped.height()<screenRect.height()*.52f) {
            schedule(700);return;
        }
        sourceRect=clipped;
        Bitmap destination=null;
        final int frameGeneration=generation;
        try {
            int width=Math.max(2,Math.round(screenRect.width()*SCALE));
            int height=Math.max(2,Math.round(screenRect.height()*SCALE));
            destination=Bitmap.createBitmap(width,height,
                    Bitmap.Config.ARGB_8888);
            final Bitmap image=destination;
            final Rect capturedScreen=new Rect(screenRect);
            pending=true;
            lastCapture=now;
            PixelCopy.request(surface,sourceRect,image,
                    result -> mainHandler.post(() -> {
                        pending=false;
                        try {
                            if(destroyed||frameGeneration!=generation||!enabled
                                    ||!scene.isReelsActive())return;
                            if(result!=PixelCopy.SUCCESS) {
                                failures++;
                                if(failures==1||failures==MAX_FAILURES)
                                    DevFileLog.note("r18 PixelCopy status="+result
                                            +" failures="+failures);
                                if(failures>=MAX_FAILURES) {
                                    enabled=false;
                                    disabledThisReels=true;
                                    hide(mainImage);hide(searchImage);
                                    DevFileLog.note("r18 PixelCopy disabled for this"
                                            +" Reels session; transparent fallback");
                                    return;
                                }
                            } else if(!usableFrame(image)) {
                                failures++;
                                if(failures==1)
                                    DevFileLog.note("r18 video sample black/blank;"
                                            +" keeping transparent material");
                                if(failures>=MAX_FAILURES) {
                                    enabled=false;
                                    disabledThisReels=true;
                                    hide(mainImage);hide(searchImage);
                                    DevFileLog.note("r18 blank video copies: capture"
                                            +" disabled until leaving Reels");
                                    return;
                                }
                            } else {
                                failures=0;
                                successes++;
                                apply(image,capturedScreen);
                                if(successes==1||successes%22==0)
                                    DevFileLog.note("r18 Reels blur SUCCESS "
                                            +image.getWidth()+"x"+image.getHeight()
                                            +" frame="+successes);
                            }
                        } finally {
                            // Two small sub-bitmaps own their pixel data.
                            // PixelCopy destination can be discarded now.
                            if(!image.isRecycled())image.recycle();
                            if(enabled)schedule(INTERVAL_MS);
                        }
                    }),copyHandler);
        }catch(Throwable error) {
            pending=false;
            if(destination!=null&&!destination.isRecycled())destination.recycle();
            failures++;
            if(failures==1)DevFileLog.note("r18 video PixelCopy unavailable: "
                    +error.getClass().getSimpleName());
            if(failures>=MAX_FAILURES) {
                enabled=false;
                disabledThisReels=true;
                hide(mainImage);hide(searchImage);
                DevFileLog.note("r18 video-glass disabled after repeated errors");
            } else schedule(1000);
        }
    }

    private void apply(Bitmap captured,Rect entireScreen) {
        setPart(mainImage,scene.mainPill,captured,entireScreen);
        if(searchImage!=null)
            setPart(searchImage,scene.searchPill,captured,entireScreen);
    }

    private static void setPart(ImageView dest,View pill,Bitmap frame,Rect full) {
        if(pill==null||pill.getVisibility()!=View.VISIBLE)return;
        Rect area=screenRect(pill);
        int x=Math.max(0,Math.round((area.left-full.left)*SCALE));
        int y=Math.max(0,Math.round((area.top-full.top)*SCALE));
        if(x>=frame.getWidth()||y>=frame.getHeight())return;
        int w=Math.min(frame.getWidth()-x,Math.max(1,Math.round(area.width()*SCALE)));
        int h=Math.min(frame.getHeight()-y,Math.max(1,Math.round(area.height()*SCALE)));
        Bitmap cropped=Bitmap.createBitmap(frame,x,y,w,h).copy(
                Bitmap.Config.ARGB_8888,false);
        android.graphics.drawable.Drawable old=dest.getDrawable();
        dest.setImageBitmap(cropped);
        dest.setAlpha(OPACITY);
        releaseLater(dest,old);
    }

    private static void hide(ImageView image) {
        if(image==null)return;
        image.setAlpha(0f);
        android.graphics.drawable.Drawable old=image.getDrawable();
        image.setImageDrawable(null);
        releaseLater(image,old);
    }

    private static void releaseLater(View view,android.graphics.drawable.Drawable d) {
        if(!(d instanceof android.graphics.drawable.BitmapDrawable))return;
        Bitmap b=((android.graphics.drawable.BitmapDrawable)d).getBitmap();
        if(b!=null&&!b.isRecycled())
            view.postDelayed(() -> {
                // Previous frame is no longer in the View tree, and GPU has
                // had ample time to retire the previous render node.
                if(!b.isRecycled())b.recycle();
            },1700);
    }

    private static boolean usableFrame(Bitmap image) {
        // Reject all-black/white/uninitialized Surface copies.
        double mean=0,square=0;
        int samples=0,almostBlack=0,almostWhite=0;
        int strideX=Math.max(1,image.getWidth()/16);
        int strideY=Math.max(1,image.getHeight()/6);
        for(int y=0;y<image.getHeight();y+=strideY) {
            for(int x=0;x<image.getWidth();x+=strideX) {
                int argb=image.getPixel(x,y);
                if(Color.alpha(argb)<240)return false;
                double lum=(.213*Color.red(argb)+.715*Color.green(argb)
                        +.072*Color.blue(argb))/255d;
                mean+=lum;square+=lum*lum;samples++;
                if(lum<.018)almostBlack++;
                if(lum>.982)almostWhite++;
            }
        }
        if(samples==0 || almostBlack>=samples*.98
                || almostWhite>=samples*.98)return false;
        double avg=mean/samples;
        double stdev=Math.sqrt(Math.max(0,square/samples-avg*avg));
        return avg>.023&&avg<.975 && stdev>.016;
    }

    private static SurfaceView findSurface(ViewGroup host,Rect requested) {
        SurfaceCandidate found=new SurfaceCandidate();
        scan(host,requested,found,new int[]{1700},0);
        return found.surface;
    }

    private static void scan(View view,Rect wanted,SurfaceCandidate found,
                             int[] budget,int depth) {
        if(budget[0]--<=0||depth>20
                ||view.getVisibility()!=View.VISIBLE||!view.isShown())return;
        if(view instanceof SurfaceView) {
            SurfaceView surface=(SurfaceView)view;
            if(surface.getWidth()<120||surface.getHeight()<120)return;
            Rect rect=screenRect(surface);
            Rect visible=new Rect();
            if(!surface.getGlobalVisibleRect(visible)) return;
            if(!visible.intersect(wanted)) return;
            long covered=(long)visible.width()*visible.height();
            long requested=(long)wanted.width()*wanted.height();
            if(requested<=0 || covered*100<requested*55
                    || visible.width()<wanted.width()*.88f)return;
            long area=(long)rect.width()*rect.height();
            if(found.surface==null||area>found.area) {
                found.area=area;
                found.surface=surface;
            }
        } else if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount()&&budget[0]>0;i++)
                scan(group.getChildAt(i),wanted,found,budget,depth+1);
        }
    }

    private static class SurfaceCandidate {
        SurfaceView surface;
        long area;
    }

    private static Rect screenRect(View view) {
        int[] loc=new int[2];
        view.getLocationOnScreen(loc);
        return new Rect(loc[0],loc[1],
                loc[0]+view.getWidth(),loc[1]+view.getHeight());
    }

    void stop() {
        if(destroyed)return;
        setEnabled(false);
        destroyed=true;
        generation++;
        pending=false;
        mainHandler.removeCallbacks(tick);
        hide(mainImage);hide(searchImage);
        if(worker!=null){
            worker.quitSafely();
            worker=null;
            copyHandler=null;
        }
        DevFileLog.note("r18 video-glass renderer stopped");
    }
}
