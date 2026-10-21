package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * Subtle optical refraction of a CACHED, low-resolution content snapshot.
 *
 * AGSL bends UV coordinates near the edge of a capsule before sampling the
 * underlying image. RenderEffect softens ONLY this backdrop layer; crisp
 * rim, selector and Instagram icons are drawn by other views above it.
 *
 * This approximates the supplied translucent-glass concept. It is not a
 * system compositor behind-window blur or Apple's physically accurate glass.
 */
final class DevRefractiveBackdrop extends View {
    private static final String SHADER =
            "uniform shader u_content;\n"
          + "uniform float2 u_size;\n"
          + "uniform float u_dark;\n"
          + "half4 main(float2 pos) {\n"
          + "  float2 uv = pos / max(u_size, float2(1.0));\n"
          + "  float2 delta = uv - 0.5;\n"
          + "  float edgeX = pow(clamp(abs(delta.x) * 2.0, 0.0, 1.0), 3.0);\n"
          + "  float edgeY = pow(clamp(abs(delta.y) * 2.0, 0.0, 1.0), 3.0);\n"
          + "  float bulge = 0.035 * (edgeX + edgeY);\n"
          + "  float2 bent = pos + delta * bulge * u_size;\n"
          + "  half4 sampled = u_content.eval(bent);\n"
          + "  half3 darkTone = sampled.rgb * 0.54 + half3(0.055, 0.065, 0.08);\n"
          + "  half3 lightTone = sampled.rgb * 0.63 + half3(0.08, 0.09, 0.105);\n"
          + "  half3 tone = mix(lightTone, darkTone, half(u_dark));\n"
          + "  return half4(tone, 1.0);\n"
          + "}\n";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final Matrix imageMatrix = new Matrix();
    private final boolean dark;
    private RuntimeShader shader;
    private Bitmap bitmap;
    private float backdropAlpha = .30f;
    private boolean setupFailed;

    DevRefractiveBackdrop(Context context, boolean dark) {
        super(context);
        this.dark = dark;
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                shader = new RuntimeShader(SHADER);
            } catch (Throwable e) {
                shader = null;
                DevFileLog.note("glass refraction fallback " +
                        e.getClass().getSimpleName());
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            float density = getResources().getDisplayMetrics().density;
            setRenderEffect(RenderEffect.createBlurEffect(
                    5f * density, 5f * density, Shader.TileMode.CLAMP));
        }
        setAlpha(backdropAlpha);
    }

    void setSnapshot(Bitmap value) {
        if (value == null || value.isRecycled()) return;
        // The background may be virtually uniform while video is displayed
        // through SurfaceView; don't wash out icons with a white placeholder.
        float[] info = analyze(value);
        float luminance = info[0];
        float variation = info[1];
        float alpha = variation < .065f ? .10f
                : luminance > .67f ? .24f : .34f;
        backdropAlpha = alpha;
        setAlpha(alpha);

        Bitmap old = bitmap;
        bitmap = value;
        setupFailed = false;
        invalidate();
        // Give RenderThread time to finish consuming old graphics resources.
        if (old != null && old != value) {
            postDelayed(() -> {
                if (old != bitmap && !old.isRecycled()) old.recycle();
            }, 1500);
        }
        DevFileLog.note("glass material luma=" +
                Math.round(luminance * 100) + "% texture=" +
                Math.round(variation * 100) + "% blend=" +
                Math.round(alpha * 100) + "%");
    }

    private static float[] analyze(Bitmap bitmap) {
        int count = 0;
        double total = 0;
        double totalSq = 0;
        int xStep = Math.max(1, bitmap.getWidth() / 11);
        int yStep = Math.max(1, bitmap.getHeight() / 5);
        for (int y = 0; y < bitmap.getHeight(); y += yStep) {
            for (int x = 0; x < bitmap.getWidth(); x += xStep) {
                int c = bitmap.getPixel(x, y);
                double v = (.2126 * android.graphics.Color.red(c)
                        + .7152 * android.graphics.Color.green(c)
                        + .0722 * android.graphics.Color.blue(c)) / 255.;
                total += v;
                totalSq += v * v;
                count++;
            }
        }
        if (count == 0) return new float[] {0f, 0f};
        double mean = total / count;
        double sd = Math.sqrt(Math.max(0., totalSq / count - mean * mean));
        return new float[] {(float) mean, (float) sd};
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap current = bitmap;
        if (current == null || current.isRecycled()
                || getWidth() == 0 || getHeight() == 0) return;
        try {
            imageMatrix.reset();
            imageMatrix.setScale(getWidth() / (float) current.getWidth(),
                    getHeight() / (float) current.getHeight());
            BitmapShader input = new BitmapShader(current,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            input.setLocalMatrix(imageMatrix);
            if (shader != null && !setupFailed) {
                shader.setInputShader("u_content", input);
                shader.setFloatUniform("u_size",
                        (float) getWidth(), (float) getHeight());
                shader.setFloatUniform("u_dark", dark ? 1f : 0f);
                paint.setShader(shader);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            } else {
                // Existing visual contrast remains protected by the low-alpha
                // material base. Plain bitmap is a graceful fallback.
                paint.setShader(null);
                canvas.drawBitmap(current, null,
                        new android.graphics.Rect(0, 0, getWidth(), getHeight()), paint);
            }
        } catch (Throwable e) {
            if (!setupFailed) {
                setupFailed = true;
                DevFileLog.note("refractive draw fallback " +
                        e.getClass().getSimpleName());
            }
            paint.setShader(null);
            paint.setColor(dark ? 0xFF5B6472 : 0xFFD3DCE3);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
    }

    @Override protected void onDetachedFromWindow() {
        Bitmap old = bitmap;
        bitmap = null;
        if (old != null && !old.isRecycled()) {
            postDelayed(() -> {
                if (old != bitmap && !old.isRecycled()) old.recycle();
            }, 1500);
        }
        super.onDetachedFromWindow();
    }
}
