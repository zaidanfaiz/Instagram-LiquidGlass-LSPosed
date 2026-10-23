package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/**
 * Experimental Reels action material when Instagram paints its action
 * icons without individual Android clickable Views or stable resource IDs.
 *
 * Never recreates, replaces, clicks, hooks or modifies Instagram buttons.
 * The optical layer has NO touch handlers. Prefer actual on-screen counter
 * label geometry. A screenshot-calibrated five-action fallback is restricted to the
 * developer's documented screen size AND exact tested Instagram version.
 * Unknown devices/builds are left unmodified.
 *
 * This is a calibrated optical overlay, not compositor refraction. Content
 * labels and user data are neither logged nor persisted.
 */
final class DevReelsRailFallback {
    private static final long SCAN_MS = 2600L;
    private static final int BUDGET = 2100;
    private final Activity activity;
    private final FrameLayout host;
    private final float density;
    private final RailMaterial material;
    private boolean added;
    private long previousScan;
    private long previousLog;
    private int lastCount = -1;
    private String lastMode = "";
    private final int[] screen = new int[2];

    DevReelsRailFallback(Activity activity, FrameLayout host) {
        this.activity = activity;
        this.host = host;
        density = activity.getResources().getDisplayMetrics().density;
        material = new RailMaterial(activity);
        material.setClickable(false);
        material.setLongClickable(false);
        material.setFocusable(false);
        material.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void sync(DevGlassUi.Scene scene, boolean fallbackNeeded) {
        if (!fallbackNeeded || scene == null || !scene.attached()
                || !scene.isReelsActive() || !activity.hasWindowFocus()
                || activity.isFinishing() || activity.isDestroyed()) {
            clear();
            return;
        }
        if (!added) {
            host.addView(material, new FrameLayout.LayoutParams(-1,-1));
            added = true;
            previousScan = 0L;
        }
        long now = SystemClock.uptimeMillis();
        if (now - previousScan < SCAN_MS) return;
        previousScan = now;
        screen[0] = host.getWidth();
        screen[1] = host.getHeight();
        if (screen[0] < 320 || screen[1] < 650) return;

        ArrayList<Rect> counts = new ArrayList<>();
        scanCounters(host, counts, new int[]{BUDGET}, 0);
        Collections.sort(counts, (a,b) -> Integer.compare(a.centerY(), b.centerY()));
        ArrayList<Rect> selected = new ArrayList<>();
        int spacing = dp(35);
        for (Rect box : counts) {
            if (!selected.isEmpty()
                    && box.centerY()-selected.get(selected.size()-1).centerY() < spacing)
                continue;
            selected.add(box);
        }

        String mode = "disabled";
        ArrayList<RectF> targets = new ArrayList<>();
        int radius = dp(24);
        if (selected.size() >= 3 && selected.size() <= 7) {
            for (Rect counter : selected) {
                // Counters sit just BELOW original action icons in Reels.
                int x = counter.centerX();
                int y = counter.centerY() - dp(31);
                addIfClear(targets, x, y, radius, scene);
            }
            mode = "counter-anchors";
        } else if (documentedBuild()) {
            // Screen coordinates calibrated ONLY from owner-supplied 1440x3200
            // Instagram 452 Reels screenshot. DO NOT apply on other builds.
            int x = Math.round(host.getWidth() * .923f);
            for (float y : new float[]{.493f,.575f,.655f,.734f,.807f}) {
                addIfClear(targets,x,Math.round(host.getHeight()*y),radius,scene);
            }
            mode = "device-calibrated-aligned";
        }

        int railCount = targets.size();
        // If a real labelled Follow control is exposed, add a separately
        // measured pill. Never infer a Follow position from the screenshot:
        // it moves with captions and author overlays.
        RectF follow = findFollow(host, new int[]{BUDGET}, 0);
        int followPills = 0;
        if (follow != null && !overlaps(follow,scene.mainPill)
                && !overlaps(follow,scene.searchPill)) {
            targets.add(follow);
            followPills = 1;
        }
        material.setTargets(targets);
        if (targets.size() != lastCount || !mode.equals(lastMode)
                || now-previousLog > 9000L) {
            lastCount = targets.size();
            lastMode = mode;
            previousLog = now;
            DevFileLog.note("beta3.2 reels rail fallback mode="+mode
                    +" counters="+selected.size()+" circles="+railCount+" followPills="+followPills
                    +" collision-checked=true");
        }
    }

    private RectF findFollow(View view, int[] remaining, int depth) {
        if (view == null || remaining[0]-- <= 0 || depth > 21
                || view == material || !view.isShown()) return null;
        if (view instanceof TextView) {
            CharSequence value = ((TextView)view).getText();
            if (value != null && value.length() < 20) {
                String label = value.toString().trim().toLowerCase(Locale.ROOT);
                if (label.equals("ikuti") || label.equals("follow")
                        || label.equals("mengikuti") || label.equals("following")) {
                    Rect r = new Rect();
                    if (view.getGlobalVisibleRect(r)) {
                        int[] offset = new int[2];
                        host.getLocationOnScreen(offset);
                        r.offset(-offset[0], -offset[1]);
                        if (r.centerY() > host.getHeight()*.62f
                                && r.centerY() < host.getHeight()*.88f
                                && r.centerX() > host.getWidth()*.18f
                                && r.centerX() < host.getWidth()*.82f) {
                            RectF glass = new RectF(r);
                            glass.inset(-dp(12),-dp(7));
                            if (glass.width() >= dp(50)
                                    && glass.width() < dp(185)
                                    && glass.height() >= dp(28)
                                    && glass.height() < dp(85))
                                return glass;
                        }
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)view;
            int count = Math.min(g.getChildCount(),90);
            for (int i=0;i<count && remaining[0]>0;i++) {
                RectF found=findFollow(g.getChildAt(i),remaining,depth+1);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean documentedBuild() {
        int w = host.getWidth(), h = host.getHeight();
        if (Math.abs(w-1440)>48 || Math.abs(h-3200)>100) return false;
        try {
            String version = activity.getPackageManager()
                    .getPackageInfo("com.instagram.android",0).versionName;
            return version != null && version.startsWith("452.0.0.48.64");
        } catch (Throwable ignored) { return false; }
    }

    private void addIfClear(ArrayList<RectF> output, float screenX,
                            float screenY, int radius, DevGlassUi.Scene scene) {
        RectF circle = new RectF(screenX-radius,screenY-radius,
                screenX+radius,screenY+radius);
        if (circle.left < 0 || circle.right > host.getWidth()
                || circle.top < 0 || circle.bottom > host.getHeight())
            return;
        // Never paint over the relocated bottom navigation or Search pill.
        if (overlaps(circle,scene.mainPill) || overlaps(circle,scene.searchPill))
            return;
        for (RectF earlier : output) {
            if (RectF.intersects(earlier,circle)) return;
        }
        output.add(circle);
    }

    private boolean overlaps(RectF circle, View shell) {
        if (shell == null || shell.getVisibility()!=View.VISIBLE) return false;
        int[] pos = new int[2], offset = new int[2];
        shell.getLocationOnScreen(pos);
        host.getLocationOnScreen(offset);
        RectF bounds = new RectF(pos[0]-offset[0],pos[1]-offset[1],
                pos[0]-offset[0]+shell.getWidth(),
                pos[1]-offset[1]+shell.getHeight());
        return RectF.intersects(circle,bounds);
    }

    private void scanCounters(View view, ArrayList<Rect> out,
                              int[] remaining, int depth) {
        if (view == null || remaining[0]-- <= 0 || depth>23
                || view.getVisibility()!=View.VISIBLE || !view.isShown())
            return;
        if (view == material) return;
        if (view instanceof TextView && out.size() < 20) {
            TextView textView = (TextView)view;
            CharSequence raw = textView.getText();
            if (raw != null && raw.length() > 0 && raw.length() <= 12
                    && looksLikeCount(raw.toString())) {
                Rect bounds = new Rect();
                if (view.getGlobalVisibleRect(bounds)) {
                    int[] origin = new int[2];
                    host.getLocationOnScreen(origin);
                    bounds.offset(-origin[0],-origin[1]);
                    if (bounds.centerX() > host.getWidth()*.82f
                            && bounds.centerY() > host.getHeight()*.44f
                            && bounds.centerY() < host.getHeight()*.92f
                            && bounds.height() < dp(38)
                            && bounds.width() < dp(95))
                        out.add(bounds);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)view;
            int limit = Math.min(g.getChildCount(),100);
            for (int i=0;i<limit && remaining[0]>0;i++)
                scanCounters(g.getChildAt(i),out,remaining,depth+1);
        }
    }

    private static boolean looksLikeCount(String raw) {
        String v = raw.replace(" ","").toUpperCase(Locale.ROOT);
        return v.matches("[0-9][0-9.,]{0,9}(RB|JT|K|M|B)?");
    }

    void clear() {
        if (!added) return;
        added = false;
        previousScan = 0L;
        material.setTargets(new ArrayList<>());
        ViewParent parent = material.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).removeView(material);
    }

    private int dp(float n) { return Math.round(n*density); }

    private static final class RailMaterial extends View {
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inside = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private final ArrayList<RectF> targets = new ArrayList<>();

        RailMaterial(Activity activity) {
            super(activity);
            density = activity.getResources().getDisplayMetrics().density;
            setWillNotDraw(false);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(Math.max(1f,density*1.1f));
            inside.setStyle(Paint.Style.STROKE);
            inside.setStrokeWidth(Math.max(.75f,density*.7f));
            shine.setStyle(Paint.Style.STROKE);
            shine.setStrokeWidth(Math.max(1f,density*1f));
        }

        void setTargets(ArrayList<RectF> value) {
            targets.clear();
            targets.addAll(value);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (RectF rect : targets) {
                float w = rect.width(), h=rect.height();
                float radius = Math.min(w,h)*.5f;
                // Smoked clear optical material; icons stay visible because
                // fill is deliberately translucent and the draw-only layer
                // neither replaces icons nor captures/intercepts touch.
                shade.setShader(new LinearGradient(rect.left,rect.top,
                        rect.right,rect.bottom,
                        new int[]{0x45101824,0x251C2A3A,0x50233042},
                        null,Shader.TileMode.CLAMP));
                edge.setShader(new LinearGradient(rect.left,rect.top,
                        rect.right,rect.bottom,
                        new int[]{0xF0FFFFFF,0x9AD8E9F5,0x48677794,0xCFFFFFFF},
                        null,Shader.TileMode.CLAMP));
                inside.setShader(new LinearGradient(rect.left,rect.top,
                        rect.left,rect.bottom,0x8AFFFFFF,0x06FFFFFF,
                        Shader.TileMode.CLAMP));
                shine.setShader(new RadialGradient(
                        rect.left+w*.23f,rect.top,
                        Math.max(w,h)*.81f,
                        new int[]{0x7DFFFFFF,0x18FFFFFF,0x00FFFFFF},
                        null,Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect,radius,radius,shade);
                canvas.drawRoundRect(rect,radius,radius,edge);
                RectF inner = new RectF(rect);
                inner.inset(density*2.4f,density*2.4f);
                float innerRadius = Math.max(1f,radius-density*2.4f);
                canvas.drawRoundRect(inner,innerRadius,innerRadius,inside);
                canvas.save();
                canvas.clipRect(rect.left,rect.top,rect.right,
                        rect.top+rect.height()*.34f);
                canvas.drawRoundRect(inner,innerRadius,innerRadius,shine);
                canvas.restore();
            }
        }
    }
}
