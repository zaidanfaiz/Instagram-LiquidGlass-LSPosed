package dev.zaidan.igglass;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * Exclusive interaction zone for the floating main capsule.
 * IG Reels uses horizontal hold/drag to seek or change playback speed.
 * Native invisible View children do not always own the ACTION_DOWN, so
 * allowing DOWN to propagate produced accidental Reel fast-forward.
 *
 * This FrameLayout captures DOWN immediately and forwards ONLY a resulting
 * click/selection to the original native Instagram tab's click listener.
 * Search is intentionally separate and unchanged.
 */
final class DevGesturePill extends FrameLayout {
    interface SlideListener {
        void onSlideStart(float localX);
        void onSlideMove(float localX);
        void onSlideEnd(float localX);
        void onSlideCancel();
        void onTap(float localX);
    }

    private final float slop;
    private float downX;
    private float downY;
    private boolean dragging;
    private boolean tracking;
    private SlideListener listener;

    DevGesturePill(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop() * 1.15f;
        // This interactive capsule MUST own ACTION_DOWN in Reels so the
        // background SurfaceView can never receive a hold-to-speed gesture.
        setClickable(true);
    }

    void setSlideListener(SlideListener value) { listener = value; }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (listener == null || getVisibility() != VISIBLE)
            return super.onInterceptTouchEvent(e);
        // Main capsule exclusively captures DOWN/UP/MOVE, but does not
        // interfere with vertical gestures OUTSIDE the visual capsule.
        return true;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (listener == null || getVisibility() != VISIBLE)
            return super.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX=e.getX();
                downY=e.getY();
                dragging=false;
                tracking=true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!tracking) return true;
                float dx=e.getX()-downX;
                float dy=e.getY()-downY;
                if (!dragging && Math.abs(dx)>slop
                        && Math.abs(dx)>Math.abs(dy)*1.15f) {
                    dragging=true;
                    listener.onSlideStart(e.getX());
                }
                if(dragging) listener.onSlideMove(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
                if(!tracking) return true;
                tracking=false;
                if(dragging) {
                    dragging=false;
                    listener.onSlideEnd(e.getX());
                } else if(Math.abs(e.getX()-downX)<slop*2.2f
                        && Math.abs(e.getY()-downY)<slop*2.2f) {
                    listener.onTap(e.getX());
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                if(dragging) listener.onSlideCancel();
                dragging=false;
                tracking=false;
                return true;
            default:
                return true;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
