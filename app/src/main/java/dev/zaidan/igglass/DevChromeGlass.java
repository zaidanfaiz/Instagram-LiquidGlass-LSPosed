package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * beta.2: conservative glass accents for Instagram chrome.
 *
 * This does not replace Instagram backgrounds, listeners, padding, layout
 * parameters, focus behavior, text, or media views. ViewOverlay draws a very
 * light tinted rim above selected controls. It is NOT a backdrop blur.
 *
 * Resource names differ between Instagram builds, so ONLY named, measured
 * controls are decorated; unknown/obfuscated views are left untouched.
 * A bounded, throttled walk avoids the per-frame work and SurfaceView
 * interference that previously broke Reels.
 */
final class DevChromeGlass {
    private static final long REFRESH_MS = 2200L;
    private static final int MAX_VISITED = 1350;
    private static final int MAX_DECORATED = 28;
    private static final int MAX_DEPTH = 20;
    private final Activity activity;
    private final View root;
    private final WeakHashMap<View, GlassAccent> active = new WeakHashMap<>();
    private final int screenWidth;
    private final int screenHeight;
    private final float density;
    private long lastRefresh;
    private long lastStatsLog;
    private boolean disabled;

    DevChromeGlass(Activity activity, View root) {
        this.activity = activity;
        this.root = root;
        this.density = activity.getResources().getDisplayMetrics().density;
        this.screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        this.screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
    }

    void sync(DevGlassUi.Scene scene, boolean mainTabVisible) {
        if (disabled || scene == null || !scene.attached()
                || !activity.hasWindowFocus()) {
            clear();
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastRefresh < REFRESH_MS) return;
        lastRefresh = now;
        HashSet<View> seen = new HashSet<>();
        int[] budget = {MAX_VISITED, MAX_DECORATED};
        try {
            visit(root, 0, scene, seen, budget, mainTabVisible);
            if (now - lastStatsLog >= 7000L) {
                lastStatsLog = now;
                int feedCount = 0;
                for (View v : seen) if (isFeedAction(v)) feedCount++;
                DevFileLog.note("beta3.1 chrome surfaces=" + seen.size()
                        + " feedActions=" + feedCount
                        + " visitedBudget=" + budget[0]);
            }
            Iterator<Map.Entry<View, GlassAccent>> iterator = active.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<View, GlassAccent> entry = iterator.next();
                View view = entry.getKey();
                if (view == null || !seen.contains(view)) {
                    if (view != null) view.getOverlay().remove(entry.getValue());
                    iterator.remove();
                }
            }
        } catch (Throwable error) {
            // Fail closed: no partial mutations to original IG widgets.
            clear();
            disabled = true;
            DevFileLog.note("beta2 chrome disabled after "
                    + error.getClass().getSimpleName());
        }
    }

    void clear() {
        for (Map.Entry<View, GlassAccent> entry : active.entrySet()) {
            if (entry.getKey() != null) {
                try { entry.getKey().getOverlay().remove(entry.getValue()); }
                catch (Throwable ignored) {}
            }
        }
        active.clear();
        lastRefresh = 0L;
    }

    private void visit(View view, int depth, DevGlassUi.Scene scene,
                       HashSet<View> seen, int[] budget, boolean mainTabVisible) {
        if (view == null || depth > MAX_DEPTH || budget[0]-- <= 0
                || view.getVisibility() != View.VISIBLE
                || view.getAlpha() < .55f) return;
        if (view == scene.mainPill || view == scene.searchPill
                || view == scene.nav || view == scene.search) return;
        String className = view.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("surfaceview") || className.contains("textureview")
                || className.contains("player") || className.contains("videoview"))
            return;
        // Do not recursively style the original moved Instagram navigation.
        if (view.getParent() == scene.nav) return;

        int w = view.getWidth(), h = view.getHeight();
        if (w > 0 && h > 0 && budget[1] > 0
                && view.isShown() && view.getWindowToken() != null) {
            int category = classify(view, className, w, h, mainTabVisible);
            if (category != 0) {
                budget[1]--;
                seen.add(view);
                GlassAccent decoration = active.get(view);
                if (decoration == null) {
                    decoration = new GlassAccent(category, density);
                    active.put(view, decoration);
                    view.getOverlay().add(decoration);
                }
                decoration.setBounds(0, 0, w, h);
                view.invalidate();
            }
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        int count = Math.min(group.getChildCount(), 60);
        for (int i = 0; i < count && budget[0] > 0; i++) {
            visit(group.getChildAt(i), depth + 1, scene, seen,
                    budget, mainTabVisible);
        }
    }

    private int classify(View view, String className, int w, int h,
                         boolean mainTabVisible) {
        final int min = dp(20), max = dp(108);
        // Leave large panels, scroll/media surfaces and full-width Reels overlays alone.
        if (h < min || h > max || w < dp(22)
                || w > screenWidth * .98f || h > screenHeight * .11f)
            return 0;
        if (className.contains("recycler") || className.contains("viewpager")
                || className.contains("scrollview"))
            return 0;
        // Feed post action rows are often ImageViews with no stable resource
        // ID. Use only short accessible ACTION labels, never text content.
        if (mainTabVisible && isFeedAction(view)) return 3;
        if (view instanceof EditText) {
            String id = resourceName(view);
            return containsAny(id, "search", "composer", "message_input",
                    "comment_input", "thread_input", "chat_input") ? 1 : 0;
        }
        String id = resourceName(view);
        if (id.isEmpty() || containsAny(id, "tab_bar", "bottom_navigation",
                "player", "video", "thumbnail", "photo", "avatar", "media",
                "story_ring", "feed_item", "recycler", "progress", "camera",
                "keyboard", "scrim", "background", "placeholder", "overlay"))
            return 0;
        if (containsAny(id, "search_bar", "search_box", "search_field",
                "search_container", "search_input", "search_pill",
                "direct_inbox_search", "inbox_search", "search_edit"))
            return 1;
        if (containsAny(id, "comment_composer", "comment_input_container",
                "message_composer", "thread_composer", "chat_composer",
                "reply_composer", "composer_container"))
            return 1;
        if (containsAny(id, "filter_chip", "filter_pill", "filter_button",
                "inbox_filter", "category_chip", "sort_chip", "segmented_control"))
            return 2;
        if (containsAny(id, "edit_profile", "profile_edit", "share_profile",
                "profile_share", "follow_button", "following_button",
                "message_button", "profile_action", "contact_button",
                "subscribe_button"))
            return 2;
        // Only small identified chrome elements, never whole toolbars.
        if (containsAny(id, "toolbar_action", "header_action", "action_chip",
                "overflow_menu_button", "more_options_button",
                "notification_button", "reels_action_button",
                "comment_send_button"))
            return 2;
        // Certain builds expose a small horizontal toolbar as a named view.
        if (mainTabVisible && w > dp(155) && h <= dp(76)
                && containsAny(id, "top_toolbar", "header_toolbar",
                "inbox_header_container", "profile_header_actions"))
            return 1;
        return 0;
    }

    private boolean isFeedAction(View view) {
        if (view.getWidth() > dp(175)
                || view.getHeight() > dp(100)
                || view.getHeight() < dp(22)) return false;
        Rect screen = new Rect();
        if (!view.getGlobalVisibleRect(screen)) return false;
        if (screen.centerY() < screenHeight * .55f
                || screen.bottom > screenHeight * .998f) return false;
        CharSequence cs = view.getContentDescription();
        if (cs == null || cs.length() < 3 || cs.length() > 70) return false;
        String label = cs.toString().trim().toLowerCase(Locale.ROOT);
        return startsAction(label, "like","unlike","liked","suka",
                "sukai","batal suka","komentar","comment","comments",
                "share","bagikan","repost","save","unsave","simpan",
                "bookmark","tersimpan","ikuti","follow","following");
    }

    private static boolean startsAction(String label, String... commands) {
        for (String command : commands) {
            if (label.equals(command) || label.startsWith(command + " ")
                    || label.startsWith(command + ","))
                return true;
        }
        return false;
    }

    private int dp(float n) { return (int) (density * n + .5f); }

    private static boolean containsAny(String input, String... terms) {
        for (String term : terms) if (input.contains(term)) return true;
        return false;
    }

    private static String resourceName(View v) {
        if (v.getId() == View.NO_ID) return "";
        try { return v.getResources().getResourceEntryName(v.getId())
                .toLowerCase(Locale.ROOT); }
        catch (Throwable ignored) { return ""; }
    }

    /** Draw-only, pointer-transparent; never becomes a touch target. */
    private static final class GlassAccent extends Drawable {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final int category;
        private final float density;
        private int width = -1, height = -1, alpha = 255;

        GlassAccent(int category, float density) {
            this.category = category;
            this.density = density;
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(Math.max(1, density * .92f));
            shine.setStyle(Paint.Style.STROKE);
            shine.setStrokeWidth(Math.max(1, density * .72f));
        }

        @Override public void draw(Canvas canvas) {
            int w = getBounds().width(), h = getBounds().height();
            if (w <= 0 || h <= 0) return;
            if (w != width || h != height) {
                width = w; height = h;
                fill.setShader(new LinearGradient(0, 0, w, h,
                        category == 1
                                ? new int[]{0x48E8F5FF, 0x1FC8DCF4, 0x34FFFFFF}
                                : category == 3
                                ? new int[]{0x40162231, 0x2D293545, 0x481B2939}
                                : new int[]{0x37FFFFFF, 0x1ADAe8FA, 0x2EF0F8FF},
                        null, Shader.TileMode.CLAMP));
                rim.setShader(new LinearGradient(0, 0, w, h,
                        new int[]{0xE5FFFFFF, 0x86DCE9FA,
                                0x507586A1, 0xD8FFFFFF},
                        null, Shader.TileMode.CLAMP));
                shine.setShader(new LinearGradient(0, 0, 0, h,
                        0x65FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            }
            float inset = density * 1.3f;
            rect.set(inset, inset, w - inset, h - inset);
            float radius = Math.min(h / 2f, category == 1 ? density * 22 : h / 2f);
            fill.setAlpha(alpha);
            rim.setAlpha(alpha);
            shine.setAlpha(alpha);
            canvas.drawRoundRect(rect, radius, radius, fill);
            canvas.drawRoundRect(rect, radius, radius, rim);
            canvas.save();
            canvas.clipRect(0, 0, w, h * .36f);
            float inner = inset + density * 1.5f;
            rect.set(inner, inner, w - inner, h - inner);
            canvas.drawRoundRect(rect, Math.max(1f, radius - inner),
                    Math.max(1f, radius - inner), shine);
            canvas.restore();
        }

        @Override public void setAlpha(int value) {
            alpha = value;
            invalidateSelf();
        }
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
