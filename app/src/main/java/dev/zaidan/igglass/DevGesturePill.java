package dev.zaidan.igglass;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

/**
 * The custom glass pill owns all touches from ACTION_DOWN, not merely after a
 * horizontal movement is detected. This prevents Instagram's Reels SurfaceView
 * or its long-press 2x-speed detector from receiving the beginning of a glass
 * navigation gesture.
 *
 * Original Instagram tab actions are invoked by Scene.tapNative() and
 * Scene.slideEnd(); this class NEVER invents Instagram navigation routes.
 * A vertical drag starting over the pill is cancelled rather than becoming
 * a tab click. Gestures outside the pill are completely untouched.
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
    private boolean tracking;
    private boolean sliding;
    private boolean cancelled;
    private SlideListener listener;

    DevGesturePill(Context context) {
        super(context);
        // Low threshold for quick one-handed navigation, but horizontal
        // direction still must dominate to avoid accidental switching.
        slop = ViewConfiguration.get(context).getScaledTouchSlop() * .78f;
        setClickable(true);
        setFocusable(false);
    }

    void setSlideListener(SlideListener listener) {
        this.listener = listener;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (listener == null) return super.onInterceptTouchEvent(ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            // Take the DOWN *before* Instagram's video long press sees it.
            return true;
        }
        return tracking || super.onInterceptTouchEvent(ev);
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (listener == null) return super.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                tracking = true;
                sliding = false;
                cancelled = false;
                ViewParent p = getParent();
                if (p != null) p.requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!tracking) return false;
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (!sliding && !cancelled) {
                    if (Math.abs(dx) >= slop
                            && Math.abs(dx) >= Math.abs(dy) * 1.10f) {
                        sliding = true;
                        listener.onSlideStart(ev.getX());
                    } else if (Math.abs(dy) > slop * 1.5f
                            && Math.abs(dy) > Math.abs(dx) * 1.35f) {
                        cancelled = true;
                    }
                }
                if (sliding) listener.onSlideMove(ev.getX());
                return true;
            case MotionEvent.ACTION_UP:
                if (!tracking) return false;
                tracking = false;
                if (sliding) listener.onSlideEnd(ev.getX());
                else if (!cancelled) listener.onTap(ev.getX());
                sliding = false;
                cancelled = false;
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (sliding) listener.onSlideCancel();
                tracking = false;
                sliding = false;
                cancelled = false;
                return true;
            default:
                return tracking;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
