package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Optional SOFT blur, safely isolated from any video SurfaceView / TextureView.
 *
 * A low-alpha frosted sample is captured ONLY while the main FEED is idle and
 * no video surface intersects the glass. Reels, opened comments, modals and
 * any background while not focused get ZERO bitmap drawing.
 *
 * This is a limited Android backdrop approximation, not iOS compositor blur.
 */
final class DevGlassBackdrop implements ViewTreeObserver.OnScrollChangedListener {
    private static final float SCALE = .23f;
    // The former 16% contribution was too subtle to look frosted. With an
    // exact captured backdrop, 42% interpolates blurred/unblurred content;
    // it does not add a white/black color fill or change with night mode.
    private static final float FROST_ALPHA = .58f;
    private static final long MIN_CAPTURE_MS = 1800L;
    private static final long TOO_SLOW_MS = 28L;
    private final Activity activity;
    private final DevGlassUi.Scene scene;
    private final FrameLayout host;
    private final ImageView main;
    private final ImageView search;
    private final Runnable captureTask = this::capture;
    private boolean stopped;
    private boolean started;
    private boolean enabled;
    private boolean tooSlow;
    private boolean skipVideoLogged;
    private long lastCaptureAt;

    DevGlassBackdrop(Activity activity, DevGlassUi.Scene scene) {
        this.activity = activity;
        this.scene = scene;
        this.host = scene.host;
        this.main = addFrostLayer(activity,scene.mainPill);
        this.search = scene.searchPill == null ? null
                : addFrostLayer(activity,scene.searchPill);
    }

    private static ImageView addFrostLayer(Activity activity,FrameLayout shell) {
        ImageView v = new ImageView(activity);
        v.setBackground(null);
        v.setClickable(false);
        v.setFocusable(false);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        v.setScaleType(ImageView.ScaleType.FIT_XY);
        v.setAlpha(0f); // No blur until a VERIFIED valid frame arrives.
        if(Build.VERSION.SDK_INT >= 31) {
            // The snapshot is downscaled to 23%. A modest 5dp blur on that
            // sampled layer is clearer than a huge 11dp blur over 305px.
            float blur = 5f*activity.getResources().getDisplayMetrics().density;
            v.setRenderEffect(RenderEffect.createBlurEffect(
                    blur, blur, Shader.TileMode.CLAMP));
        }
        // Behind the beveled glass rim, active lens and clickable IG tab row.
        shell.addView(v, 0, new FrameLayout.LayoutParams(-1,-1));
        return v;
    }

    void start() {
        if(stopped || started) return;
        started=true;
        ViewTreeObserver obs=host.getViewTreeObserver();
        if(obs.isAlive()) obs.addOnScrollChangedListener(this);
        DevFileLog.note("r14 optical-frost: static content strength=58%, radius=5dp; Reels capture OFF");
    }

    void setEnabled(boolean shouldEnable) {
        boolean allow = !stopped && started && shouldEnable
                && scene.attached() && !scene.isReelsActive()
                && activity.hasWindowFocus();
        if(enabled == allow) return;
        enabled=allow;
        host.removeCallbacks(captureTask);
        if(!enabled) {
            clear(main);
            clear(search);
        } else request(900);
        DevFileLog.note("r12 safe-frost " + (enabled ? "enabled" : "suspended"));
    }

    private static void clear(ImageView v) {
        if(v == null) return;
        v.setAlpha(0f);
        android.graphics.drawable.Drawable old=v.getDrawable();
        v.setImageDrawable(null);
        if(old instanceof android.graphics.drawable.BitmapDrawable) {
            Bitmap bmp=((android.graphics.drawable.BitmapDrawable)old).getBitmap();
            if(bmp!=null && !bmp.isRecycled())
                v.postDelayed(() -> { if(!bmp.isRecycled()) bmp.recycle(); },1800);
        }
    }

    void request(int delayMs) {
        if(!enabled || stopped || tooSlow || scene.isReelsActive()) return;
        host.removeCallbacks(captureTask);
        host.postDelayed(captureTask,Math.max(850,delayMs));
    }

    @Override public void onScrollChanged() {
        // Immediately discard the old frame while Instagram is scrolling,
        // otherwise the bitmap lags behind the moving feed and looks like an
        // opaque, ghosted overlay. Re-frost when scrolling has settled.
        if(enabled) {
            clear(main);
            clear(search);
            request(1100);
        }
    }

    private void capture() {
        if(!enabled || stopped || tooSlow || scene.isReelsActive()
                || scene.mainPill.getVisibility()!=View.VISIBLE
                || !activity.hasWindowFocus() || activity.isDestroyed()
                || activity.isFinishing()) return;
        long now=SystemClock.uptimeMillis();
        if(now-lastCaptureAt < MIN_CAPTURE_MS) {
            request((int)(MIN_CAPTURE_MS-(now-lastCaptureAt)+100));
            return;
        }
        lastCaptureAt=now;

        int[] hostXY=new int[2];
        host.getLocationOnScreen(hostXY);
        Rect mainR=relativeRect(scene.mainPill,hostXY);
        Rect searchR=scene.searchPill==null ? null
                : relativeRect(scene.searchPill,hostXY);
        Rect bounds=new Rect(mainR);
        if(searchR!=null)bounds.union(searchR);
        if(bounds.width()<25 || bounds.height()<25
                || bounds.width()>host.getWidth() || bounds.height()>host.getHeight()) return;

        // HARD SAFETY GATE: never call View.draw on a hierarchy containing
        // a live video surface under the sampling strip, preventing playback
        // freezes caused by video layers being sampled or re-laid out.
        if(hasVideoSurface(host,bounds,hostXY,0,new int[]{1400})) {
            if(!skipVideoLogged) {
                DevFileLog.note("r12 safe-frost: live video detected, clear material kept");
                skipVideoLogged=true;
            }
            clear(main);
            clear(search);
            return;
        }
        skipVideoLogged=false;
        Bitmap combined=null;
        long began=SystemClock.uptimeMillis();
        try {
            combined=Bitmap.createBitmap(
                    Math.max(1,Math.round(bounds.width()*SCALE)),
                    Math.max(1,Math.round(bounds.height()*SCALE)),
                    Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(combined);
            c.scale(SCALE,SCALE);
            c.translate(-bounds.left,-bounds.top);
            c.clipRect(bounds);
            for(int i=0;i<host.getChildCount();i++) {
                View child=host.getChildAt(i);
                if(child==scene.mainPill || child==scene.searchPill
                        || child.getVisibility()!=View.VISIBLE)continue;
                int checkpoint=c.save();
                c.translate(child.getLeft()-host.getScrollX(),
                        child.getTop()-host.getScrollY());
                c.concat(child.getMatrix());
                child.draw(c);
                c.restoreToCount(checkpoint);
            }
            long took=SystemClock.uptimeMillis()-began;
            if(took>TOO_SLOW_MS) {
                tooSlow=true;
                clear(main);
                clear(search);
                DevFileLog.note("r12 safe-frost: disabled after slow draw " + took+"ms");
                return;
            }
            if(!isUsable(combined)) {
                clear(main);
                clear(search);
                return;
            }
            Bitmap x=crop(combined,mainR,bounds);
            if(x!=null)replace(main,x);
            if(search!=null && searchR!=null) {
                Bitmap y=crop(combined,searchR,bounds);
                if(y!=null)replace(search,y);
            }
            DevFileLog.note("r13 optical-frost sample=" + combined.getWidth()
                    +"x"+combined.getHeight()+" blend=58% radius=5dp took="+took+"ms");
        }catch(Throwable error) {
            tooSlow=true;
            clear(main);clear(search);
            DevFileLog.note("r12 safe-frost disabled: "
                    +error.getClass().getSimpleName());
        }finally {
            if(combined!=null&&!combined.isRecycled())combined.recycle();
        }
    }

    private static boolean isUsable(Bitmap bmp) {
        int w=bmp.getWidth(),h=bmp.getHeight();
        if(w<2||h<2)return false;
        double n=0,sum=0,squares=0;
        for(int y=0;y<h;y+=Math.max(1,h/7)) {
            for(int x=0;x<w;x+=Math.max(1,w/15)) {
                int c=bmp.getPixel(x,y);
                if(Color.alpha(c)<220)return false;
                double v=(.213*Color.red(c)+.715*Color.green(c)
                        +.072*Color.blue(c))/255.;
                sum+=v;squares+=v*v;n++;
            }
        }
        double avg=sum/n,dev=Math.sqrt(Math.max(0,squares/n-avg*avg));
        return dev>.07 && avg<.90 && avg>.07;
    }

    private static void replace(ImageView v, Bitmap b) {
        android.graphics.drawable.Drawable previous=v.getDrawable();
        v.setImageBitmap(b);
        v.setAlpha(FROST_ALPHA);
        if(previous instanceof android.graphics.drawable.BitmapDrawable) {
            Bitmap old=((android.graphics.drawable.BitmapDrawable)previous).getBitmap();
            if(old!=null&&old!=b&&!old.isRecycled())
                v.postDelayed(() -> { if(!old.isRecycled())old.recycle(); },1800);
        }
    }

    private static Bitmap crop(Bitmap original,Rect item,Rect all) {
        int x=Math.max(0,Math.round((item.left-all.left)*SCALE));
        int y=Math.max(0,Math.round((item.top-all.top)*SCALE));
        if(x>=original.getWidth()||y>=original.getHeight())return null;
        int w=Math.min(original.getWidth()-x,Math.max(1,Math.round(item.width()*SCALE)));
        int h=Math.min(original.getHeight()-y,Math.max(1,Math.round(item.height()*SCALE)));
        Bitmap part=Bitmap.createBitmap(original,x,y,w,h);
        return part==original ? original.copy(Bitmap.Config.ARGB_8888,false):part;
    }

    private static Rect relativeRect(View v,int[] hostXY) {
        int[] position=new int[2]; v.getLocationOnScreen(position);
        int x=position[0]-hostXY[0],y=position[1]-hostXY[1];
        return new Rect(x,y,x+v.getWidth(),y+v.getHeight());
    }

    private static boolean hasVideoSurface(View v, Rect target,int[] hostXY,
                                           int depth,int[] budget) {
        if(budget[0]--<=0||depth>19||v.getVisibility()!=View.VISIBLE)return false;
        if(v instanceof SurfaceView||v instanceof TextureView) {
            Rect rect=relativeRect(v,hostXY);
            return Rect.intersects(rect,target);
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount()&&budget[0]>0;i++) {
                if(hasVideoSurface(g.getChildAt(i),target,hostXY,
                        depth+1,budget))return true;
            }
        }
        return false;
    }

    void stop() {
        if(stopped)return;
        setEnabled(false);
        stopped=true;
        host.removeCallbacks(captureTask);
        ViewTreeObserver obs=host.getViewTreeObserver();
        if(obs.isAlive())obs.removeOnScrollChangedListener(this);
        clear(main);clear(search);
    }
}
