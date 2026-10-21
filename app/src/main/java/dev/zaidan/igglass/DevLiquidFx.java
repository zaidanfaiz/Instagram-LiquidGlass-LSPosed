package dev.zaidan.igglass;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Small procedural specular/ripple effect, rendered by AGSL on Android 13+.
 * No framebuffer capture or pixel readback. Shader output is transparent
 * except for moving glass highlights; touch animations last <700ms.
 */
final class DevLiquidFx extends View {
    private static final String AGSL =
            "uniform float2 u_size;\n"
          + "uniform float2 u_touch;\n"
          + "uniform float u_progress;\n"
          + "half4 main(float2 p) {\n"
          + "  float2 s = max(u_size, float2(1.0));\n"
          + "  float2 uv = p/s;\n"
          + "  float distanceToTouch = distance(uv, u_touch);\n"
          + "  float rip = sin(distanceToTouch * 35.0 - u_progress * 17.0);\n"
          + "  float fade = exp(-distanceToTouch * 10.0);\n"
          + "  float motion = sin(u_progress * 3.14159265);\n"
          + "  float wave = rip * fade * motion;\n"
          + "  float upper = exp(-pow((uv.y - 0.14)*9.0, 2.0));\n"
          + "  float lower = exp(-pow((uv.y - 0.90)*18.0, 2.0));\n"
          + "  float rim = exp(-pow((uv.x - 0.03)*24.0, 2.0))"
          + " + exp(-pow((uv.x - 0.97)*24.0, 2.0));\n"
          + "  float a = clamp((upper*0.075 + lower*0.028"
          + " + rim*0.035) * motion + wave*0.19, 0.0, 0.22);\n"
          + "  return half4(half3(0.87, 0.95, 1.0), half(a));\n"
          + "}\n";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RuntimeShader shader;
    private ValueAnimator animator;
    private float progress;
    private float touchX = 0.5f;
    private float touchY = 0.5f;

    DevLiquidFx(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                shader = new RuntimeShader(AGSL);
                paint.setShader(shader);
            } catch (Throwable e) {
                shader = null;
                DevFileLog.note("AGSL unsupported: " + e.getClass().getSimpleName());
            }
        }
    }

    void pulse(float x, float y) {
        touchX = Math.max(0f, Math.min(1f,
                x / Math.max(1f, getWidth())));
        touchY = Math.max(0f, Math.min(1f,
                y / Math.max(1f, getHeight())));
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (Float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
        DevFileLog.note("GPU liquid touch ripple started");
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (shader == null || getWidth() <= 0 || getHeight() <= 0) return;
        try {
            shader.setFloatUniform("u_size", (float) getWidth(), (float) getHeight());
            shader.setFloatUniform("u_touch", touchX, touchY);
            shader.setFloatUniform("u_progress", progress);
            float radius = Math.min(getWidth(), getHeight()) / 2f;
            canvas.drawRoundRect(0, 0, getWidth(), getHeight(),
                    radius, radius, paint);
        } catch (Throwable error) {
            shader = null;
            DevFileLog.note("AGSL draw fallback " + error.getClass().getSimpleName());
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }
}
