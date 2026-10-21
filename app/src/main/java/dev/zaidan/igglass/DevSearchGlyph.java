package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Crisp independent search glyph. Original Instagram Search View remains
 * underneath as the sole clickable target, with visual alpha set to zero;
 * this prevents IG's opaque selection drawable covering the glass circle.
 */
final class DevSearchGlyph extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    DevSearchGlyph(Context context) {
        super(context);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(0xFFF5FAFF);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 2.7f);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() * .47f, cy = getHeight() * .46f;
        float radius = Math.min(getWidth(), getHeight()) * .205f;
        canvas.drawCircle(cx, cy, radius, paint);
        float start = radius * .72f;
        float end = radius * 1.53f;
        canvas.drawLine(cx + start, cy + start,
                cx + end, cy + end, paint);
    }
}
