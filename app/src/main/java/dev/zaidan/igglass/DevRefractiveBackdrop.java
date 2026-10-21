/*
 * This class adapts the real rounded-rect SDF refraction + 7-sample
 * chromatic dispersion shader from QmDeve/AndroidLiquidGlassView.
 *
 * Copyright (c) 2025-2026 Donny Yang / QmDeve and contributors.
 * Original shader: https://github.com/QmDeve/AndroidLiquidGlassView
 * Licensed under the MIT License; see THIRD_PARTY_NOTICES.md.
 *
 * Changes: Android View/LSPosed-adapted BitmapShader input, cached backdrop
 * instead of an eager onPreDraw RenderNode capture, theme-independent glass
 * tint, texture confidence gate for SurfaceView video, safe bitmap lifetime.
 */
package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.view.View;

/**
 * Physical SDF refraction and chromatic dispersion of a cached feed backdrop.
 * Never paints opaque white or black when the backdrop cannot be captured.
 * Uses one stable neutral glass material on every Instagram page and theme.
 */
final class DevRefractiveBackdrop extends View {
    private static final String SDF_AGSL =
            "uniform shader content;\n" +
            "uniform float2 size;\n" +
            "uniform float2 offset;\n" +
            "uniform float4 cornerRadii;\n" +
            "uniform float refractionHeight;\n" +
            "uniform float refractionAmount;\n" +
            "uniform float depthEffect;\n" +
            "uniform float chromaticAberration;\n" +
            "\n" +
            "uniform float contrast;\n" +
            "uniform float whitePoint;\n" +
            "uniform float chromaMultiplier;\n" +
            "\n" +
            "uniform float3 tintColor;\n" +
            "uniform float tintAlpha;\n" +
            "\n" +
            "const half3 rgbToY = half3(0.2126, 0.7152, 0.0722);\n" +
            "\n" +
            "float radiusAt(float2 coord, float4 radii) {\n" +
            "    if (coord.x >= 0.0) {\n" +
            "        if (coord.y <= 0.0) return radii.y;\n" +
            "        else return radii.z;\n" +
            "    } else {\n" +
            "        if (coord.y <= 0.0) return radii.x;\n" +
            "        else return radii.w;\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "float sdRoundedRect(float2 coord, float2 halfSize, float radius) {\n" +
            "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n" +
            "    float outside = length(max(cornerCoord, 0.0)) - radius;\n" +
            "    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);\n" +
            "    return outside + inside;\n" +
            "}\n" +
            "\n" +
            "float safeSign(float value) {\n" +
            "    return value < 0.0 ? -1.0 : 1.0;\n" +
            "}\n" +
            "\n" +
            "float2 safeNormalize(float2 value, float2 fallback) {\n" +
            "    float len = length(value);\n" +
            "    if (len > 0.001) {\n" +
            "        return value / len;\n" +
            "    }\n" +
            "    return fallback;\n" +
            "}\n" +
            "\n" +
            "float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {\n" +
            "    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));\n" +
            "    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {\n" +
            "        float2 outside = max(cornerCoord, 0.0);\n" +
            "        float outsideLength = length(outside);\n" +
            "        if (outsideLength > 0.001) {\n" +
            "            return sign(coord) * (outside / outsideLength);\n" +
            "        }\n" +
            "        float useX = step(cornerCoord.y, cornerCoord.x);\n" +
            "        return float2(\n" +
            "            useX * safeSign(coord.x),\n" +
            "            (1.0 - useX) * safeSign(coord.y)\n" +
            "        );\n" +
            "    } else {\n" +
            "        float gradX = step(cornerCoord.y, cornerCoord.x);\n" +
            "        return sign(coord) * float2(gradX, 1.0 - gradX);\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "float circleMap(float x) {\n" +
            "    return 1.0 - sqrt(1.0 - x * x);\n" +
            "}\n" +
            "\n" +
            "half4 saturateColor(half4 color, float amount) {\n" +
            "    half3 lin = toLinearSrgb(color.rgb);\n" +
            "    float y = dot(lin, rgbToY);\n" +
            "    half3 gray = half3(y);\n" +
            "    half3 sat = fromLinearSrgb(mix(gray, lin, amount));\n" +
            "    return half4(sat, color.a);\n" +
            "}\n" +
            "\n" +
            "half4 main(float2 coord) {\n" +
            "    float2 halfSize = size * 0.5;\n" +
            "    float2 centeredCoord = (coord + offset) - halfSize;\n" +
            "    float radius = radiusAt(centeredCoord, cornerRadii);\n" +
            "\n" +
            "    float sd = sdRoundedRect(centeredCoord, halfSize, radius);\n" +
            "    if (-sd >= refractionHeight) {\n" +
            "        half4 baseColor = content.eval(coord);\n" +
            "\n" +
            "        baseColor = saturateColor(baseColor, chromaMultiplier);\n" +
            "        float3 target = (whitePoint > 0.0) ? float3(1.0) : float3(0.0);\n" +
            "        baseColor.rgb = mix(baseColor.rgb, target, abs(whitePoint));\n" +
            "        baseColor.rgb = (baseColor.rgb - 0.5) * (1.0 + contrast) + 0.5;\n" +
            "\n" +
            "        half3 tintedRGB = mix(baseColor.rgb, tintColor, tintAlpha);\n" +
            "        return half4(tintedRGB, baseColor.a * 0.10);\n" +
            "    }\n" +
            "\n" +
            "    sd = min(sd, 0.0);\n" +
            "    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;\n" +
            "    float smoothRadius = max(radius * 1.5, 30.0);\n" +
            "    float gradRadius = min(smoothRadius, min(halfSize.x, halfSize.y));\n" +
            "\n" +
            "    float2 shapeGrad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);\n" +
            "    float2 depthGrad = safeNormalize(centeredCoord, shapeGrad);\n" +
            "    float2 grad = safeNormalize(shapeGrad + depthEffect * depthGrad, shapeGrad);\n" +
            "\n" +
            "    float2 refractedCoord = coord + d * grad;\n" +
            "    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));\n" +
            "    float2 dispersedCoord = d * grad * dispersionIntensity;\n" +
            "\n" +
            "    half4 color = half4(0.0);\n" +
            "\n" +
            "    half4 red = content.eval(refractedCoord + dispersedCoord);\n" +
            "    color.r += red.r / 3.5;\n" +
            "    color.a += red.a / 7.0;\n" +
            "\n" +
            "    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));\n" +
            "    color.r += orange.r / 3.5;\n" +
            "    color.g += orange.g / 7.0;\n" +
            "    color.a += orange.a / 7.0;\n" +
            "\n" +
            "    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));\n" +
            "    color.r += yellow.r / 3.5;\n" +
            "    color.g += yellow.g / 3.5;\n" +
            "    color.a += yellow.a / 7.0;\n" +
            "\n" +
            "    half4 green = content.eval(refractedCoord);\n" +
            "    color.g += green.g / 3.5;\n" +
            "    color.a += green.a / 7.0;\n" +
            "\n" +
            "    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));\n" +
            "    color.g += cyan.g / 3.5;\n" +
            "    color.b += cyan.b / 3.0;\n" +
            "    color.a += cyan.a / 7.0;\n" +
            "\n" +
            "    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));\n" +
            "    color.b += blue.b / 3.0;\n" +
            "    color.a += blue.a / 7.0;\n" +
            "\n" +
            "    half4 purple = content.eval(refractedCoord - dispersedCoord);\n" +
            "    color.r += purple.r / 7.0;\n" +
            "    color.b += purple.b / 3.0;\n" +
            "    color.a += purple.a / 7.0;\n" +
            "\n" +
            "    color = saturateColor(color, chromaMultiplier);\n" +
            "    float3 target = (whitePoint > 0.0) ? float3(1.0) : float3(0.0);\n" +
            "    color.rgb = mix(color.rgb, target, abs(whitePoint));\n" +
            "    color.rgb = (color.rgb - 0.5) * (1.0 + contrast) + 0.5;\n" +
            "\n" +
            "    half3 tintedRGB = mix(color.rgb, tintColor, tintAlpha);\n" +
            "    return half4(tintedRGB, color.a * 0.60);\n" +
            "}\n";

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG
            | Paint.FILTER_BITMAP_FLAG);
    private final Matrix bitmapScale = new Matrix();
    private final float density;
    private RuntimeShader sdfShader;
    private Bitmap snapshot;
    private boolean disabled;
    private boolean videoMode;
    private float opticalAlpha;
    private boolean loggedShader;
    private int widthAtLastShaderUpdate;
    private int heightAtLastShaderUpdate;

    DevRefractiveBackdrop(Context context, boolean ignoredTheme) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
        try {
            sdfShader = new RuntimeShader(SDF_AGSL);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
            DevFileLog.note("SDF optical pipeline loaded (QmDeve MIT adaptation)");
        } catch (Throwable error) {
            disabled = true;
            DevFileLog.note("SDF optical unavailable " + error.getClass().getSimpleName());
        }
        setAlpha(0f);
    }

    void setSnapshot(Bitmap image) {
        if (image == null || image.isRecycled()) return;
        // Videos drawn on SurfaceView are absent from View.draw(Canvas).
        // Uniform captures are unreliable and must NOT become black/white
        // pills. In that case leave a clear glass rim drawn by DevGlassSurface.
        float[] stats = analyze(image);
        float luma = stats[0], deviation = stats[1];
        float confidence = Math.max(0f, Math.min(1f, (deviation - .055f) / .13f));
        // Optical refraction may only color the *edge*. At 100% confidence
        // final center contribution is <= 5%; never a solid white/black pill.
        float opacity = disabled ? 0f : .48f * confidence;
        if (luma > .81f) opacity *= .86f;
        opticalAlpha = opacity;
        setAlpha(videoMode ? 0f : opacity);
        Bitmap previous = snapshot;
        snapshot = image;
        invalidate();
        if (previous != null && previous != image) {
            postDelayed(() -> {
                if (previous != snapshot && !previous.isRecycled())
                    previous.recycle();
            }, 1800);
        }
        DevFileLog.note("SDF optics luma=" + Math.round(luma * 100)
                + "% detail=" + Math.round(deviation * 100)
                + "% confidence=" + Math.round(confidence * 100)
                + "% optical-edge-max=" + Math.round(opacity * .60f * 100)
                + "% center-max=" + Math.round(opacity * .10f * 100) + "%");
    }

    void setVideoMode(boolean enabled) {
        if (videoMode == enabled) return;
        videoMode = enabled;
        // Reels commonly use SurfaceView, absent from software View.draw.
        // Never display a wrongly captured (possibly white) static image.
        setAlpha(videoMode ? 0f : opticalAlpha);
        DevFileLog.note("optics video mode=" + videoMode
                + "; base alpha always 0%");
    }

    private static float[] analyze(Bitmap b) {
        float mean = 0f, squared = 0f;
        int count = 0, dx = Math.max(1, b.getWidth()/12);
        int dy = Math.max(1, b.getHeight()/6);
        for (int y=0; y<b.getHeight(); y+=dy) {
            for (int x=0; x<b.getWidth(); x+=dx) {
                int c = b.getPixel(x, y);
                float v = (0.2126f * ((c>>16)&255)
                        + 0.7152f * ((c>>8)&255)
                        + 0.0722f * (c&255))/255f;
                mean += v;
                squared += v*v;
                count++;
            }
        }
        if (count == 0) return new float[]{0f, 0f};
        mean /= count;
        return new float[]{mean, (float)Math.sqrt(Math.max(0f,
                squared/count-mean*mean))};
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap b = snapshot;
        if (disabled || b == null || b.isRecycled()
                || getWidth() < 2 || getHeight() < 2) return;
        try {
            float w = getWidth(), h = getHeight();
            BitmapShader bitmapInput = new BitmapShader(b,
                    Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            bitmapScale.reset();
            bitmapScale.setScale(w/b.getWidth(), h/b.getHeight());
            bitmapInput.setLocalMatrix(bitmapScale);

            sdfShader.setInputShader("content", bitmapInput);
            if (widthAtLastShaderUpdate != getWidth()
                    || heightAtLastShaderUpdate != getHeight()) {
                widthAtLastShaderUpdate = getWidth();
                heightAtLastShaderUpdate = getHeight();
                float r = h*.5f;
                sdfShader.setFloatUniform("size",w,h);
                sdfShader.setFloatUniform("offset",0f,0f);
                sdfShader.setFloatUniform("cornerRadii",r,r,r,r);
                sdfShader.setFloatUniform("refractionHeight",
                        Math.min(19f*density,h*.27f));
                sdfShader.setFloatUniform("refractionAmount",
                        -Math.min(23f*density,h*.32f));
                sdfShader.setFloatUniform("depthEffect",.15f);
                sdfShader.setFloatUniform("chromaticAberration",.12f);
                sdfShader.setFloatUniform("contrast",0f);
                sdfShader.setFloatUniform("whitePoint",0f);
                sdfShader.setFloatUniform("chromaMultiplier",1.06f);
                sdfShader.setFloatUniform("tintColor",.91f,.94f,.98f);
                sdfShader.setFloatUniform("tintAlpha",.075f);
                if (!loggedShader) {
                    loggedShader=true;
                    DevFileLog.note("SDF refraction+dispersion uniform setup "
                            + getWidth() + "x" + getHeight());
                }
            }
            paint.setShader(sdfShader);
            canvas.drawRect(0,0,w,h,paint);
        } catch (Throwable error) {
            disabled = true;
            setAlpha(0f);
            DevFileLog.note("SDF shader failed; clear-glass fallback "
                    + error.getClass().getSimpleName() + " "
                    + String.valueOf(error.getMessage()));
        }
    }

    @Override protected void onDetachedFromWindow() {
        Bitmap old = snapshot;
        snapshot = null;
        if (old != null && !old.isRecycled()) {
            postDelayed(() -> {
                if (old != snapshot && !old.isRecycled()) old.recycle();
            }, 1800);
        }
        super.onDetachedFromWindow();
    }
}
