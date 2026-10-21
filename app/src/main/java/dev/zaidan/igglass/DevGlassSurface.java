package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/**
 * Procedural glass layers, independent of Android's day/night theme.
 *
 * This deliberately avoids a solid white fill. The backdrop is placed BELOW
 * this view, and Instagram's real icons remain ABOVE it. Drawing is limited
 * to a few round-rects and cached shaders on the render thread.
 *
 * Concept reference: neutral smoked/translucent capsule, silver bevel,
 * directional specular reflection, and a brighter independent selection lens.
 * This is an Android approximation, not native iOS backdrop refraction.
 */
final class DevGlassSurface extends View {
    private final boolean selectedLens;
    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lowerShade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float inset;
    private float radius;
    private int lastW, lastH;

    DevGlassSurface(Context context, boolean selectedLens) {
        super(context);
        this.selectedLens = selectedLens;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
        inset = dp(1.1f);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(dp(selectedLens ? 1.30f : 1.15f));
        innerEdge.setStyle(Paint.Style.STROKE);
        innerEdge.setStrokeWidth(dp(.72f));
        lowerShade.setStyle(Paint.Style.STROKE);
        lowerShade.setStrokeWidth(dp(1.6f));
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w <= 0 || h <= 0) return;
        lastW = w;
        lastH = h;
        radius = h / 2f;
        if (selectedLens) {
            tint.setShader(new LinearGradient(0f, 0f, w, h,
                    new int[] {0x65D3DFEA, 0x40BECEDD, 0x58E1E8F0},
                    new float[] {0f, .46f, 1f}, Shader.TileMode.CLAMP));
        } else {
            tint.setShader(new LinearGradient(0f, 0f, w, h,
                    new int[] {0x37CFD9E3, 0x28ABBCCD, 0x32DCE5ED},
                    new float[] {0f, .51f, 1f}, Shader.TileMode.CLAMP));
        }
        edge.setShader(new LinearGradient(0, 0, w, h,
                selectedLens
                    ? new int[] {0xF4FFFFFF, 0xB9DDEBF3, 0x96EDF5FA, 0xE4F8FFFF}
                    : new int[] {0xE5F5FAFF, 0x87BECFDD, 0x65CBD9E4, 0xD9F4FBFF},
                new float[] {0f, .28f, .72f, 1f}, Shader.TileMode.CLAMP));
        innerEdge.setShader(new LinearGradient(0, 0, 0, h,
                new int[] {
                        selectedLens ? 0x86FFFFFF : 0x4DFFFFFF,
                        0x16F4FAFF,
                        0x09FFFFFF,
                        selectedLens ? 0x45FFFFFF : 0x29FFFFFF
                }, null, Shader.TileMode.CLAMP));
        specular.setShader(new RadialGradient(w * .24f, h * -.07f,
                Math.max(w * .53f, h),
                selectedLens
                    ? new int[] {0x79FFFFFF, 0x22FFFFFF, 0x00FFFFFF}
                    : new int[] {0x53F5FAFF, 0x10F4FAFF, 0x00FFFFFF},
                new float[] {0f, .42f, 1f}, Shader.TileMode.CLAMP));
        lowerShade.setShader(new LinearGradient(0, 0, 0, h,
                new int[] {0x00B5D8EE, 0x00B5D8EE, 0x42798FA1},
                new float[] {0f, .65f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || lastW != w || lastH != h) return;
        float left = inset;
        float top = inset;
        float right = w - inset;
        float bottom = h - inset;
        float r = Math.max(1, radius - inset);

        // A neutral, clear material: no dark/light Instagram theme decision.
        canvas.drawRoundRect(left, top, right, bottom, r, r, tint);
        // Broad top-left reflection gives volume without a white rectangle.
        canvas.drawRoundRect(left, top, right, bottom, r, r, specular);
        // Thin metallic perimeter plus internal refraction-like second line.
        canvas.drawRoundRect(left, top, right, bottom, r, r, edge);
        float inner = inset + dp(1.65f);
        canvas.drawRoundRect(inner, inner, w - inner, h - inner,
                Math.max(1, r - dp(1.5f)), Math.max(1, r - dp(1.5f)), innerEdge);
        // Subtle lower rim creates the suspended/convex impression.
        canvas.drawRoundRect(left, top, right, bottom, r, r, lowerShade);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
