package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Optical glass rim inspired by the user's translucent iOS-style concept.
 * Renders only the thin outer glint and inner edge (no opaque fill).
 * Native Instagram icons stay above it. Avoids screenshot/frame polling.
 */
final class DevGlassRim extends View {
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint upper = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final boolean dark;
    private float density;

    DevGlassRim(Context context, boolean dark) {
        super(context);
        this.dark = dark;
        density = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);
        stroke.setStyle(Paint.Style.STROKE);
        inner.setStyle(Paint.Style.STROKE);
        upper.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.05f * density);
        inner.setStrokeWidth(.85f * density);
        upper.setStrokeWidth(.85f * density);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float rad = Math.min(w, h) * .5f;
        float edge = Math.max(1f, stroke.getStrokeWidth() / 2f);
        rect.set(edge, edge, w - edge, h - edge);
        stroke.setShader(new LinearGradient(0, 0, 0, h,
                new int[] {
                    dark ? 0xE5FFFFFF : 0xFAFFFFFF,
                    dark ? 0x8DDAF4FF : 0xC4E7F6FF,
                    dark ? 0x3A90A5B9 : 0x65909BAA,
                    dark ? 0x9DDFEEFA : 0x98FFFFFF
                },
                new float[] {0f, .30f, .73f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, rad, rad, stroke);

        float inset = 2.3f * density;
        if (w <= inset * 2 || h <= inset * 2) return;
        rect.set(inset, inset, w - inset, h - inset);
        inner.setShader(new LinearGradient(0, 0, 0, h,
                dark ? 0x50FFFFFF : 0x91FFFFFF,
                0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, rad - inset, rad - inset, inner);

        // High specular upper lip, like light skimming the glass surface.
        float topInset = 3.2f * density;
        if (w > topInset * 2) {
            upper.setShader(new LinearGradient(0, 0, w, 0,
                    new int[] {0x00FFFFFF, 0xA9FFFFFF, 0xE9FFFFFF,
                            0xA9FFFFFF, 0x00FFFFFF},
                    new float[] {0f, .19f, .5f, .81f, 1f}, Shader.TileMode.CLAMP));
            RectF top = new RectF(topInset, topInset,
                    w - topInset, h - topInset);
            // RoundRect rim is preferred over a flat straight highlight.
            canvas.save();
            canvas.clipRect(0, 0, w, h * .32f);
            canvas.drawRoundRect(top, rad - topInset, rad - topInset, upper);
            canvas.restore();
        }
    }
}
