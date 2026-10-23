package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * beta.3 EXPERIMENTAL Reels action glass.
 *
 * Adds optical, touch-transparent material to already-existing clickable
 * Like, Comment, Share, Save, Follow and More controls. Never replaces
 * an Instagram listener, redraws a player, mutates its layout or changes
 * the foreground icon. Only copies a small rectangle DIRECTLY from a
 * verified SurfaceView at a bounded rate; inaccessible video means an
 * optical rim-only fallback. No images leave memory.
 *
 * The drawable lives in ViewOverlay, ABOVE Instagram's own contents.
 * Consequently the backdrop component is intentionally low-opacity to
 * preserve the original icons; this is not system-level iOS refraction.
 */
final class DevReelsMaterial {
    private static final long DETECT_MS = 1700L;
    private static final long SAMPLE_MS = 950L;
    private static final int MAX_NODES = 2000;
    private static final int MAX_DEPTH = 23;
    private static final int MAX_BUTTONS = 12;
    private static final float SCALE = .28f;
    private final Activity activity;
    private final View root;
    private final float density;
    private final int screenWidth;
    private final int screenHeight;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<View, Material> controls = new HashMap<>();
    
    private HandlerThread worker;
    private Handler background;
    private Bitmap sampleBitmap;
    private Rect sampleScreen;
    private SurfaceView preferredSurface;
    private DevReelsRailFallback rail;
    private View railHost;
    private boolean enabled;
    private boolean destroyed;
    private boolean pending;
    private boolean copySuspended;
    private int generation;
    private int errors;
    private long lastDetect;
    private long lastDetectionLog;
    private long lastSample;

    DevReelsMaterial(Activity activity, View root) {
        this.activity = activity;
        this.root = root;
        density = activity.getResources().getDisplayMetrics().density;
        screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
    }

    void sync(DevGlassUi.Scene scene, boolean visible) {
        boolean allow = !destroyed && visible && scene != null
                && scene.attached() && scene.isReelsActive()
                && activity.hasWindowFocus() && !activity.isFinishing()
                && !activity.isDestroyed();
        if (!allow) {
            suspend();
            return;
        }
        if (!enabled) {
            enabled = true;
            copySuspended = false;
            errors = 0;
            generation++;
            lastDetect = 0;
            DevFileLog.note("beta3 Reels optical controls enabled");
        }
        if (SystemClock.uptimeMillis() - lastDetect >= DETECT_MS) {
            lastDetect = SystemClock.uptimeMillis();
            detect(scene);
        }
        // Instagram 452 draws some Reels buttons outside normal clickable
        // View geometry. Use an optical, no-touch fallback where needed.
        if (rail == null || railHost != scene.host) {
            if (rail != null) rail.clear();
            rail = new DevReelsRailFallback(activity, scene.host);
            railHost = scene.host;
        }
        rail.sync(scene, controls.size() < 3);
        if (!copySuspended && !pending && !controls.isEmpty()
                && SystemClock.uptimeMillis() - lastSample >= SAMPLE_MS) {
            sample();
        }
    }

    void suspend() {
        if (rail != null) rail.clear();
        if (!enabled) return;
        enabled = false;
        generation++;
        pending = false;
        preferredSurface = null;
        clearControls();
    }

    void stop() {
        if (destroyed) return;
        suspend();
        rail = null;
        railHost = null;
        destroyed = true;
        if (worker != null) {
            worker.quitSafely();
            worker = null;
            background = null;
        }
    }

    private void clearControls() {
        for (Map.Entry<View, Material> item : controls.entrySet()) {
            try { item.getKey().getOverlay().remove(item.getValue()); }
            catch (Throwable ignored) {}
            item.getValue().setFrame(null, null);
        }
        controls.clear();
        releaseBitmap();
    }

    private void detect(DevGlassUi.Scene scene) {
        HashSet<View> found = new HashSet<>();
        try {
            int[] budget = {MAX_NODES};
            HashSet<View> unlabelled = new HashSet<>();
            scan(root, scene, 0, budget, found, unlabelled);
            int labelCount = found.size();
            // Obfuscated Instagram builds may have NO action resource names.
            // Only accept fallback if a right-side aligned vertical cluster of
            // >=3 native CLICKABLE views is visible. No clicks are synthesized.
            if (found.size() < 3)
                attachVerticalRail(unlabelled, found);
            long now = SystemClock.uptimeMillis();
            if (now - lastDetectionLog > 6500L) {
                lastDetectionLog = now;
                DevFileLog.note("beta3 controls scan visited="
                        + (MAX_NODES - budget[0]) + " labelled=" + labelCount
                        + " candidates=" + unlabelled.size()
                        + " attached=" + found.size()
                        + " geometric=" + Math.max(0,found.size()-labelCount));
            }
            Iterator<Map.Entry<View, Material>> it = controls.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<View, Material> entry = it.next();
                if (!found.contains(entry.getKey())) {
                    entry.getKey().getOverlay().remove(entry.getValue());
                    it.remove();
                }
            }
        } catch (Throwable t) {
            DevFileLog.note("beta3 Reels detection fallback "
                    + t.getClass().getSimpleName());
            clearControls();
            copySuspended = true;
        }
    }

    private void scan(View view, DevGlassUi.Scene scene, int depth,
                      int[] budget, HashSet<View> found, HashSet<View> unlabelled) {
        if (view == null || depth > MAX_DEPTH || budget[0]-- <= 0
                || view.getVisibility() != View.VISIBLE
                || view.getAlpha() < .50f) return;
        if (view == scene.mainPill || view == scene.searchPill
                || view == scene.nav || view == scene.search) return;
        if (!view.isShown()) return;
        if (view instanceof SurfaceView || view instanceof TextureView) return;
        String className = view.getClass().getName().toLowerCase(Locale.ROOT);
        // Do NOT prune RecyclerView/player CONTAINERS: in Instagram 452 the
        // real native action buttons often live BELOW one of these groups.
        // Exclude video surfaces themselves, but inspect their UI siblings.
        if (found.size() < MAX_BUTTONS) {
            String action = actionOf(view);
            if (action != null) {
                View target = bestTouchTarget(view, action);
                if (target != null && allowedPosition(target, action))
                    attach(target, view, found);
            } else if (view.isClickable()
                    && withinSize(view, "like")
                    && inReelsRail(view) && unlabelled.size() < 36) {
                unlabelled.add(view);
            }
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) view;
        int count = Math.min(parent.getChildCount(), 85);
        for (int i = 0; i < count && budget[0] > 0; i++) {
            scan(parent.getChildAt(i), scene, depth + 1, budget, found, unlabelled);
        }
    }

    private void attach(View target, View source, HashSet<View> found) {
        if (!found.add(target)) return;
        Material drawable = controls.get(target);
        if (drawable == null) {
            drawable = new Material(target, density);
            controls.put(target, drawable);
            target.getOverlay().add(drawable);
        }
        drawable.setBounds(0, 0, target.getWidth(), target.getHeight());
        drawable.setActive(isActive(target) || isActive(source));
        drawable.setFrame(sampleBitmap, sampleScreen);
    }

    private boolean inReelsRail(View v) {
        if (!v.isShown() || v.getWindowToken() == null) return false;
        Rect rect = screenRect(v);
        if (rect.centerX() < screenWidth * .75f
                || rect.top < screenHeight * .27f
                || rect.bottom > screenHeight * .89f)
            return false;
        Rect visible = new Rect();
        return v.getGlobalVisibleRect(visible)
                && visible.width() >= rect.width() * .85f
                && visible.height() >= rect.height() * .85f;
    }

    private void attachVerticalRail(HashSet<View> candidates,
                                    HashSet<View> found) {
        // Three distinct tap hitboxes at a shared x-axis distinguish a
        // Reels action rail from a random corner control. Avoid nested views
        // by requiring separate vertical rows.
        View pivot = null;
        int strongest = 0;
        for (View anchor : candidates) {
            Rect a = screenRect(anchor);
            int count = 0;
            for (View v : candidates) {
                Rect b = screenRect(v);
                if (Math.abs(b.centerX() - a.centerX()) <= dp(27)
                        && Math.abs(b.centerY() - a.centerY()) > dp(28))
                    count++;
            }
            if (count > strongest) {
                pivot = anchor;
                strongest = count;
            }
        }
        if (pivot == null || strongest < 2) return;
        final int middleX = screenRect(pivot).centerX();
        java.util.ArrayList<View> column = new java.util.ArrayList<>();
        for (View view : candidates) {
            if (Math.abs(screenRect(view).centerX() - middleX) <= dp(27))
                column.add(view);
        }
        java.util.Collections.sort(column,
                (a,b) -> Integer.compare(screenRect(a).centerY(),
                                         screenRect(b).centerY()));
        int lastY = -99999;
        java.util.ArrayList<View> selected = new java.util.ArrayList<>();
        for (View view : column) {
            int centerY = screenRect(view).centerY();
            if (centerY - lastY < dp(32)) continue;
            selected.add(view);
            lastY = centerY;
            if (selected.size() >= MAX_BUTTONS) break;
        }
        if (selected.size() < 3) return;
        for (View button : selected)
            attach(button, button, found);
    }

    private View bestTouchTarget(View source, String action) {
        View chosen = source;
        View candidate = source;
        for (int i = 0; i < 3; i++) {
            ViewParent parent = candidate.getParent();
            if (!(parent instanceof View)) break;
            View upper = (View) parent;
            if (!withinSize(upper, action) || upper.getVisibility() != View.VISIBLE)
                break;
            // Prefer the original hitbox over a tiny image icon.
            if (upper.isClickable() || (chosen.getWidth() < dp(36)
                    && upper.getWidth() >= dp(38))) chosen = upper;
            candidate = upper;
        }
        return withinSize(chosen, action) ? chosen : null;
    }

    private boolean withinSize(View view, String action) {
        int w = view.getWidth(), h = view.getHeight();
        int maxWidth = action.equals("follow") ? dp(190) : dp(105);
        return w >= dp(25) && w <= maxWidth
                && h >= dp(25) && h <= dp(95);
    }

    private boolean allowedPosition(View view, String action) {
        if (!view.isShown() || view.getWindowToken() == null) return false;
        Rect r = screenRect(view);
        Rect visible = new Rect();
        if (!view.getGlobalVisibleRect(visible)
                || visible.width() < r.width() * .75f
                || visible.height() < r.height() * .75f) return false;
        if (r.top < screenHeight * .13f || r.bottom > screenHeight * .97f)
            return false;
        if (!action.equals("follow")
                && r.centerX() < screenWidth * .60f) return false;
        return true;
    }

    private static String actionOf(View view) {
        String id = "";
        if (view.getId() != View.NO_ID) {
            try { id = view.getResources().getResourceEntryName(view.getId())
                    .toLowerCase(Locale.ROOT); }
            catch (Throwable ignored) {}
        }
        if (has(id, "likes_count", "comment_count", "like_count",
                "avatar", "media", "player", "video", "loading",
                "progress", "feed_item", "story", "comment_list",
                "comment_composer", "dialog", "tab_bar")) return null;
        String desc = "";
        CharSequence sequence = view.getContentDescription();
        if (sequence != null && sequence.length() < 72)
            desc = sequence.toString().toLowerCase(Locale.ROOT).trim();
        boolean buttonId = has(id, "_button", "_action", "_icon", "_container",
                "clips_", "reels_");
        if (buttonId && has(id,"like")) return "like";
        if (buttonId && has(id,"comment")) return "comment";
        if (buttonId && has(id,"share", "send_to")) return "share";
        if (buttonId && has(id,"save", "bookmark")) return "save";
        if (buttonId && has(id,"follow")) return "follow";
        if (buttonId && has(id,"more", "overflow")) return "more";
        if (matches(desc,"like","unlike","suka","sukai","menyukai",
                "liked","batal suka","hapus suka")) return "like";
        if (matches(desc,"comment","comments","komentar","beri komentar","tulis komentar")) return "comment";
        if (matches(desc,"share","bagikan","kirim ke","repost","kirim")) return "share";
        if (matches(desc,"save","unsave","saved","simpan","bookmark",
                "tersimpan","batalkan simpan")) return "save";
        if (matches(desc,"follow","following","ikuti",
                "mengikuti","diikuti","unfollow")) return "follow";
        if (matches(desc,"more","more options","lainnya",
                "opsi lainnya")) return "more";
        return null;
    }

    private static boolean matches(String desc, String... words) {
        for (String word : words) {
            if (desc.equals(word) || desc.startsWith(word + " ")) return true;
        }
        return false;
    }
    private static boolean has(String v, String... parts) {
        for (String s : parts) if (v.contains(s)) return true;
        return false;
    }
    private static boolean isActive(View v) {
        if (v.isSelected() || v.isActivated()) return true;
        CharSequence label = v.getContentDescription();
        if (label == null || label.length() > 72) return false;
        String s = label.toString().toLowerCase(Locale.ROOT);
        return has(s, "unlike", "liked", "batal suka", "hapus suka",
                "following", "mengikuti", "diikuti", "unsave", "saved", "tersimpan");
    }

    private void sample() {
        if (!enabled || destroyed || pending || copySuspended || controls.isEmpty())
            return;
        Rect union = null;
        for (View target : controls.keySet()) {
            Rect r = screenRect(target);
            // Sample only the right-hand Reels rail, not captions/messages.
            if (r.centerX() < screenWidth * .60f) continue;
            if (union == null) union = new Rect(r);
            else union.union(r);
        }
        if (union == null || union.width() < dp(25)
                || union.width() > screenWidth * .37f
                || union.height() > screenHeight * .79f) return;
        SurfaceView source = findSurface(root, union, new int[]{900}, 0);
        if (source == null) {
            if (preferredSurface != null)
                DevFileLog.note("beta3 Reels video source unavailable; optical fallback");
            preferredSurface = null;
            copySuspended = true; // reset on next Reels entry
            return;
        }
        preferredSurface = source;
        int[] xyz = new int[2];
        source.getLocationOnScreen(xyz);
        Rect relative = new Rect(union);
        relative.offset(-xyz[0], -xyz[1]);
        Rect clipped = new Rect(relative);
        if (!clipped.intersect(0, 0, source.getWidth(), source.getHeight())
                || clipped.width() < relative.width() * .96f
                || clipped.height() < relative.height() * .96f) return;

        if (worker == null) {
            worker = new HandlerThread("IGGlass-reels-actions");
            worker.start();
            background = new Handler(worker.getLooper());
        }
        final int token = generation;
        final Rect covered = new Rect(union);
        final Bitmap destination;
        try {
            destination = Bitmap.createBitmap(
                    Math.max(2, Math.round(clipped.width() * SCALE)),
                    Math.max(2, Math.round(clipped.height() * SCALE)),
                    Bitmap.Config.ARGB_8888);
            pending = true;
            lastSample = SystemClock.uptimeMillis();
            PixelCopy.request(source, clipped, destination, result ->
                    main.post(() -> {
                        pending = false;
                        if (!enabled || destroyed || token != generation) {
                            recycleLater(destination);
                            return;
                        }
                        if (result != PixelCopy.SUCCESS || !usable(destination)) {
                            recycleLater(destination);
                            if (++errors >= 3) {
                                copySuspended = true;
                                clearSampleOnly();
                                DevFileLog.note("beta3 Reels sample stopped; optical fallback");
                            }
                            return;
                        }
                        errors = 0;
                        Bitmap old = sampleBitmap;
                        sampleBitmap = destination;
                        sampleScreen = covered;
                        for (Map.Entry<View, Material> item : controls.entrySet()) {
                            item.getValue().setFrame(sampleBitmap, sampleScreen);
                            item.getKey().invalidate();
                        }
                        if (old != null && old != destination) recycleLater(old);
                    }), background);
        } catch (Throwable error) {
            pending = false;
            copySuspended = true;
            DevFileLog.note("beta3 Reels copy exception "
                    + error.getClass().getSimpleName());
        }
    }

    private void clearSampleOnly() {
        for (Material material : controls.values()) material.setFrame(null, null);
        releaseBitmap();
    }

    private void releaseBitmap() {
        Bitmap old = sampleBitmap;
        sampleBitmap = null;
        sampleScreen = null;
        if (old != null) recycleLater(old);
    }
    private void recycleLater(Bitmap bitmap) {
        main.postDelayed(() -> {
            if (!bitmap.isRecycled()) bitmap.recycle();
        }, 1800L);
    }
    private static boolean usable(Bitmap image) {
        int n = 0, dark = 0, white = 0;
        for (int y = 0; y < image.getHeight(); y += Math.max(1,image.getHeight()/7)) {
            for (int x = 0; x < image.getWidth(); x += Math.max(1,image.getWidth()/8)) {
                int c = image.getPixel(x,y);
                n++;
                int avg=(Color.red(c)+Color.green(c)+Color.blue(c))/3;
                if (avg < 3) dark++;
                if (avg > 252) white++;
            }
        }
        return n > 0 && dark < n * .99f && white < n * .99f;
    }
    private SurfaceView findSurface(View view, Rect area, int[] budget, int depth) {
        if (view == null || budget[0]-- <= 0 || depth > 19
                || view.getVisibility() != View.VISIBLE) return null;
        if (view instanceof SurfaceView) {
            SurfaceView s = (SurfaceView)view;
            if (!s.getHolder().getSurface().isValid()
                    || s.getWidth() < screenWidth * .45f
                    || s.getHeight() < screenHeight * .35f) return null;
            Rect box = screenRect(s);
            return box.contains(area) ? s : null;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            int size = Math.min(group.getChildCount(), 90);
            for (int i = 0; i < size && budget[0] > 0; i++) {
                SurfaceView found = findSurface(group.getChildAt(i),area,budget,depth+1);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static Rect screenRect(View view) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        return new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());
    }
    private int dp(float n) { return Math.round(n * density); }

    private static final class Material extends Drawable {
        private final float density;
        private final View host;
        private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG
                | Paint.FILTER_BITMAP_FLAG);
        private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outer = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF round = new RectF();
        private final Path clip = new Path();
        private Bitmap frame;
        private Rect captureArea;
        private boolean active;
        private int oldW = -1, oldH = -1;
        private boolean oldActive;

        Material(View host, float density) {
            this.host = host;
            this.density = density;
            outer.setStyle(Paint.Style.STROKE);
            outer.setStrokeWidth(Math.max(1f,density * 1.05f));
            inner.setStyle(Paint.Style.STROKE);
            inner.setStrokeWidth(Math.max(.7f,density * .6f));
            glint.setStyle(Paint.Style.STROKE);
            glint.setStrokeWidth(Math.max(.8f,density * 1.05f));
        }
        void setActive(boolean next) {
            if (next != active) { active = next; invalidateSelf(); }
        }
        void setFrame(Bitmap bitmap, Rect rectangle) {
            frame = bitmap;
            captureArea = rectangle == null ? null : new Rect(rectangle);
            invalidateSelf();
        }
        @Override public void draw(Canvas canvas) {
            int w = getBounds().width(),h = getBounds().height();
            if (w < 1 || h < 1) return;
            if (w != oldW || h != oldH || active != oldActive) {
                oldW = w; oldH = h; oldActive = active;
                shade.setShader(new LinearGradient(0,0,w,h,
                        active ? new int[]{0x43FFFFFF,0x25B4C6E8,0x3EFFFFFF}
                               : new int[]{0x1C0A1020,0x08040A18,0x270E1725},
                        null, Shader.TileMode.CLAMP));
                outer.setShader(new LinearGradient(0,0,w,h,
                        active ? new int[]{0xF8FFFFFF,0x92DFF4FF,0x5AB4C6DE,0xE2FFFFFF}
                               : new int[]{0xD3FFFFFF,0x79D6E2F0,0x4765758D,0xA6F1F8FF},
                        null,Shader.TileMode.CLAMP));
                inner.setShader(new LinearGradient(0,0,0,h,
                        0x92FFFFFF, 0x06FFFFFF,Shader.TileMode.CLAMP));
                glint.setShader(new RadialGradient(w * .26f,0f,
                        Math.max(w * .70f,h * .7f),
                        new int[]{0xACFFFFFF,0x30FFFFFF,0x00FFFFFF},
                        null,Shader.TileMode.CLAMP));
            }
            float pad = density * 1.25f;
            float radius = Math.min(w,h) / 2f - pad;
            radius = Math.max(radius, density*7);
            round.set(pad,pad,w-pad,h-pad);
            clip.rewind();
            clip.addRoundRect(round,radius,radius,Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            if (frame != null && !frame.isRecycled() && captureArea != null
                    && captureArea.width() > 0 && captureArea.height() > 0) {
                Rect real = screenRect(host);
                int left = Math.max(0,Math.round((real.left-captureArea.left)
                        * frame.getWidth()/(float)captureArea.width()));
                int top = Math.max(0,Math.round((real.top-captureArea.top)
                        * frame.getHeight()/(float)captureArea.height()));
                int right = Math.min(frame.getWidth(),Math.round((real.right-captureArea.left)
                        * frame.getWidth()/(float)captureArea.width()));
                int bottom = Math.min(frame.getHeight(),Math.round((real.bottom-captureArea.top)
                        * frame.getHeight()/(float)captureArea.height()));
                if (right > left && bottom > top) {
                    backdrop.setAlpha(active ? 54 : 83);
                    canvas.drawBitmap(frame,new Rect(left,top,right,bottom),round,backdrop);
                }
            }
            canvas.drawRoundRect(round,radius,radius,shade);
            canvas.restore();
            canvas.drawRoundRect(round,radius,radius,outer);
            float inset = pad + density*1.7f;
            RectF inside = new RectF(inset,inset,w-inset,h-inset);
            canvas.drawRoundRect(inside,Math.max(1,radius-inset),
                    Math.max(1,radius-inset),inner);
            canvas.save();
            canvas.clipRect(0,0,w,h*.39f);
            canvas.drawRoundRect(inside,Math.max(1,radius-inset),
                    Math.max(1,radius-inset),glint);
            canvas.restore();
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
