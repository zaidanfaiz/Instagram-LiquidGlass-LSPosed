package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;
import java.util.WeakHashMap;

/**
 * Instagram 452 uses a proprietary page router, not the standard AndroidX
 * fragment API. Detect the VISIBLE foreground page from multiple public UI
 * labels + view IDs, never usernames, posts, input contents or chat messages.
 *
 * Policy:
 *   show: HOME, REELS, SEARCH, own PROFILE, DIRECT INBOX LIST
 *   hide: opened DM conversation, other person's PROFILE, STORY, unfocused
 *
 * Signals are intentionally conservative; ambiguous content preserves the
 * previous page classification when appropriate. No identities logged.
 */
final class DevScreenDetector {
    private DevScreenDetector() {}
    private enum Page {
        HOME_OR_REELS, OWN_PROFILE, DIRECT_INBOX, OTHER_PROFILE,
        DM_CONVERSATION, REELS_COMMENTS, STORY, UNKNOWN
    }
    private static final WeakHashMap<Activity, Page> CURRENT =
            new WeakHashMap<>();

    static String hiddenBecause(Activity activity, View nav,
                                View formerParent) {
        if (activity.isFinishing() || activity.isDestroyed()) return "closing";
        if (!activity.hasWindowFocus()) return "unfocused";
        if (!activity.getClass().getName().contains("InstagramMainActivity"))
            return "secondary-activity";

        Page page = inspect(activity);
        Page old = CURRENT.put(activity, page);
        if (old != page) {
            DevFileLog.note("page routing " + (old == null ? "start" : old)
                    + " -> " + page);
        }

        if (page == Page.OWN_PROFILE || page == Page.DIRECT_INBOX)
            return null;
        if (page == Page.OTHER_PROFILE) return "other-user-profile";
        if (page == Page.DM_CONVERSATION) return "dm-conversation-open";
        if (page == Page.REELS_COMMENTS) return "reels-comments-overlay";
        if (page == Page.STORY) return "story-viewer";
        if (formerParent != null
                && (!formerParent.isShown() || formerParent.getAlpha() < .10f))
            return "instagram-tab-parent-hidden";
        // The original tab row is intentionally alpha=0 to stop Instagram
        // painting opaque pixels; it is still VISIBLE and clickable.
        // Never treat deliberate visual transparency as screen navigation.
        if (nav.getVisibility() != View.VISIBLE)
            return "native-tab-hidden";
        return null;
    }

    static String fragmentNames(Activity activity) {
        Page page = CURRENT.get(activity);
        return page == null ? "(router not detected)" : "page=" + page;
    }

    static String pageName(Activity activity) {
        return inspect(activity).name();
    }

    private static Page inspect(Activity activity) {
        View root = activity.getWindow().getDecorView();
        if (!(root instanceof ViewGroup)) return Page.UNKNOWN;
        Signals v = new Signals(root.getHeight());
        inspectChildren((ViewGroup) root, 0, new int[] {5000}, v);
        Page page = choose(v);
        // Only fixed, public UI marker COUNTS are logged. Never actual labels,
        // usernames, avatars, message body or input text.
        if (page == Page.UNKNOWN) {
            return Page.HOME_OR_REELS;
        }
        return page;
    }

    private static final class Signals {
        final int screenHeight;
        final int[] own = new int[3]; // edit, share, dashboard
        final int[] inbox = new int[4]; // main, requests, filter, search/meta
        int profileStats;
        boolean followButton;
        int profileStructure;
        int dmComposer;
        int dmThreadHeader;
        int storyMarker;
        int commentSheet, commentHeader, commentComposer, commentList;
        Signals(int height) { screenHeight = height; }
    }

    private static Page choose(Signals s) {
        // Bottom-sheet overlays can leave the underlying Home / Reels /
        // Profile markers attached. Inspect this highest-priority foreground
        // UI FIRST so the floating pills never cover an open comment sheet.
        // Two independent signals are required to avoid captions such as
        // "lihat komentar" accidentally hiding navigation.
        boolean confirmedComments = (s.commentSheet > 0
                && (s.commentHeader > 0 || s.commentComposer > 0
                        || s.commentList > 0))
                || (s.commentHeader > 0
                        && (s.commentComposer > 0 || s.commentList > 0));
        if (confirmedComments) return Page.REELS_COMMENTS;
        int ownCount = (s.own[0] > 0 ? 1 : 0)
                + (s.own[1] > 0 ? 1 : 0)
                + (s.own[2] > 0 ? 1 : 0);
        int inboxCount = (s.inbox[0] > 0 ? 1 : 0)
                + (s.inbox[1] > 0 ? 1 : 0)
                + (s.inbox[2] > 0 ? 1 : 0)
                + (s.inbox[3] > 0 ? 1 : 0);

        // Exact "Edit profil" and "Bagikan profil" distinguish the user's
        // own page from someone else's, even when the avatar/grid is shared.
        if (ownCount >= 2) return Page.OWN_PROFILE;
        // A live conversation input is more specific than old inbox tabs
        // that Instagram may keep attached beneath a new chat screen.
        if (s.dmComposer > 0 && (s.dmThreadHeader > 0 || inboxCount < 3))
            return Page.DM_CONVERSATION;
        // Instagram's inbox selection screen has "Utama" + "Permintaan".
        // A conversation without an input is not considered confirmed.
        if ((s.inbox[0] > 0 && s.inbox[1] > 0)
                || (inboxCount >= 3 && s.dmComposer == 0))
            return Page.DIRECT_INBOX;
        if (s.profileStats >= 2 &&
                (s.followButton || s.profileStructure >= 2))
            return Page.OTHER_PROFILE;
        if (s.storyMarker > 0) return Page.STORY;
        return Page.UNKNOWN;
    }

    private static void inspectChildren(ViewGroup group, int depth,
                                        int[] remaining, Signals found) {
        if (depth > 20 || remaining[0] <= 0) return;
        for (int i=0; i<group.getChildCount() && remaining[0]>0; i++) {
            View child = group.getChildAt(i);
            remaining[0]--;
            if (!child.isShown() || child.getAlpha() < .10f
                    || child.getWidth() == 0 || child.getHeight() == 0)
                continue;
            String id = DevGlassUi.resourceName(child)
                    .toLowerCase(Locale.ROOT);
            classifyResource(id, child, found);
            if (child instanceof EditText) {
                // Read only the INPUT HINT; never read any typed message.
                CharSequence hint = ((EditText) child).getHint();
                if (hint != null && hint.length() <= 50) {
                    String value = hint.toString().trim().toLowerCase(Locale.ROOT);
                    if (value.contains("tambahkan komentar")
                            || value.contains("add a comment")
                            || value.contains("tulis komentar")
                            || value.contains("write a comment"))
                        found.commentComposer++;
                    if (value.equals("pesan") || value.equals("message")
                            || value.equals("kirim pesan")
                            || value.equals("tulis pesan")
                            || value.equals("message...") || value.equals("pesan..."))
                        found.dmComposer++;
                }
            } else if (child instanceof TextView) {
                CharSequence shown = ((TextView) child).getText();
                if (shown != null && shown.length() > 0 && shown.length() <= 36) {
                    // Guard tiny UI text matching by visibility and position.
                    Rect visible = new Rect();
                    if (child.getGlobalVisibleRect(visible)
                            && visible.height() > 0
                            && visible.top < found.screenHeight * .80f) {
                        classifyLabel(shown.toString().trim()
                                .toLowerCase(Locale.ROOT), found);
                    }
                }
            }
            if (child instanceof ViewGroup)
                inspectChildren((ViewGroup) child, depth+1, remaining, found);
        }
    }

    private static void classifyResource(String id, View v, Signals f) {
        // Class fallback works even when the obfuscated Instagram View has
        // NO public resource ID. Require a large on-screen panel.
        boolean largePanel = v.getWidth() > f.screenHeight * .12f
                && v.getHeight() > f.screenHeight * .20f;
        String className = v.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if ((className.contains("comment") && (className.contains("sheet")
                        || className.contains("panel") || className.contains("dialog")))
                && largePanel) f.commentSheet++;
        if (id.isEmpty()) return;
        if ((id.contains("comments_bottom_sheet")
                || id.contains("comment_bottom_sheet")
                || id.contains("comment_sheet")
                || id.contains("comments_container")
                || id.contains("comments_dialog")
                || id.contains("comment_thread_container")
                || id.contains("reel_comments_container")
                || (id.contains("bottom_sheet") && !id.contains("tab_bar")))
                && largePanel) f.commentSheet++;
        if ((id.contains("comment_list") || id.contains("comments_list")
                || id.contains("comment_recycler") || id.contains("comment_thread_list"))
                && largePanel) f.commentList++;
        if ((id.contains("comment_composer") || id.contains("comment_input")
                || id.contains("comment_text_field")
                || id.contains("comment_edit_text"))
                && v.getHeight() < f.screenHeight * .30f)
            f.commentComposer++;
        if (id.contains("edit_profile") || id.contains("profile_edit_button"))
            f.own[0]++;
        if (id.contains("share_profile") || id.contains("profile_share_button"))
            f.own[1]++;
        if (id.contains("professional_dashboard") || id.contains("profile_dashboard"))
            f.own[2]++;
        if (id.contains("direct_inbox_header") || id.contains("inbox_tab"))
            f.inbox[0]++;
        if (id.contains("direct_inbox_list") || id.contains("direct_thread_list"))
            f.inbox[1]++;
        if (id.contains("inbox_filter")) f.inbox[2]++;
        if (id.contains("direct_inbox_search")) f.inbox[3]++;
        if (id.contains("user_profile_container") || id.contains("profile_media_grid"))
            f.profileStructure++;
        if ((id.contains("message_composer") || id.contains("thread_composer")
                || id.contains("thread_input") || id.contains("chat_input"))
                && v.getHeight() < f.screenHeight * .30f) f.dmComposer++;
        if (id.contains("direct_thread_header") || id.contains("chat_thread_header")
                || id.contains("conversation_header")) f.dmThreadHeader++;
        // Do NOT treat generic "reel_viewer" as a Story; Reels is allowed.
        if (id.contains("story_viewer") || id.contains("story_playback")
                || id.contains("stories_viewer") || id.contains("story_reel_viewer")) {
            if (v.getHeight() > f.screenHeight * .55f) f.storyMarker++;
        }
    }

    private static void classifyLabel(String label, Signals f) {
        if (label.equals("komentar") || label.equals("comments")
                || label.equals("komentar reels") || label.equals("reels comments"))
            f.commentHeader++;
        if (label.equals("edit profil") || label.equals("edit profile")) f.own[0]++;
        else if (label.equals("bagikan profil") || label.equals("share profile"))
            f.own[1]++;
        else if (label.equals("dasbor anda") || label.equals("professional dashboard"))
            f.own[2]++;
        else if (label.equals("utama") || label.equals("primary")) f.inbox[0]++;
        else if (label.equals("permintaan") || label.equals("requests")) f.inbox[1]++;
        else if (label.equals("filter")) f.inbox[2]++;
        else if (label.equals("cari atau tanya meta ai")
                || label.equals("search or ask meta ai")) f.inbox[3]++;
        else if (label.endsWith(" postingan") || label.equals("postingan")
                || label.endsWith(" posts") || label.equals("posts")
                || label.endsWith(" pengikut") || label.equals("pengikut")
                || label.endsWith(" followers") || label.equals("followers")
                || label.endsWith(" mengikuti") || label.equals("mengikuti")
                || label.endsWith(" following") || label.equals("following"))
            f.profileStats++;
        else if (label.equals("ikuti") || label.equals("follow")
                || label.equals("mengikuti") || label.equals("following")
                || label.equals("kirim pesan") || label.equals("message"))
            f.followButton = true;
    }
}
