package dev.zaidan.igglass;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/**
 * A pill which leaves normal taps and vertical gestures to Instagram's
 * original View listeners but intercepts a deliberate horizontal drag.
 * Only after touch slop is exceeded does it own the remainder of the
 * gesture. On release the target original tab receives performClick().
 */
final class DevGesturePill extends FrameLayout {
    interface SlideListener {
        void onSlideStart(float localX);
        void onSlideMove(float localX);
        void onSlideEnd(float localX);
        void onSlideCancel();
    }

    private final float slop;
    private float downX;
    private float downY;
    private boolean sliding;
    private SlideListener listener;

    DevGesturePill(Context context) {
        super(context);
        slop = ViewConfiguration.get(context).getScaledTouchSlop() * 1.25f;
        setClickable(false);
    }

    void setSlideListener(SlideListener listener) {
        this.listener = listener;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (listener == null) return super.onInterceptTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                sliding = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (sliding) return true;
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    sliding = true;
                    listener.onSlideStart(event.getX());
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                sliding = false;
                return false;
            default:
                return false;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!sliding || listener == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                listener.onSlideMove(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                listener.onSlideEnd(event.getX());
                sliding = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                listener.onSlideCancel();
                sliding = false;
                return true;
            default:
                return true;
        }
    }
}
