package dev.zaidan.igglass;

import android.app.Activity;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/**
 * Experimental five-page order for Instagram 452:
 * Home(0) -> Reels(1) -> DM(2) -> Own Profile(4) -> Search(3).
 *
 * IMPORTANT: observe native page-swipes AFTER Instagram receives the touch
 * sequence. This class never steals ACTION_DOWN/MOVE from Reels, never
 * touches SurfaceView/TextureView, never rewrites a pager adapter and NEVER
 * treats a horizontal photo-carousel swipe as a page navigation unless the
 * original native selected tab actually changes.
 *
 * One exception is Profile -> Search: native Profile is the rightmost page,
 * so it cannot naturally advance. A deliberate long horizontal swipe across
 * the OWN-profile header can activate the detached native Search control.
 */
final class DevOrderedPageSwipe {
    private static final int[] ORDER = {0,1,2,4,3};
    private static final int MIN_DURATION_MS = 80;
    private static final int MAX_DURATION_MS = 1650;
    private final Activity activity;
    private final DevGlassUi.Scene scene;
    private final View decor;
    private boolean tracking;
    private boolean disposed;
    private int pointer;
    private int startingPage;
    private float downX, downY;
    private long downTime;
    private int generation;

    DevOrderedPageSwipe(Activity activity, DevGlassUi.Scene scene) {
        this.activity = activity;
        this.scene = scene;
        this.decor = activity.getWindow().getDecorView();
        DevFileLog.note("r17 ordered content swipes: Home-Reels-DM-Profile-Search"
                + "; observe-native-only + guarded profile-to-search edge");
    }

    void stop() {
        disposed = true;
        tracking = false;
        generation++;
    }

    void onDispatch(MotionEvent e) {
        if (disposed || e == null) return;
        int action = e.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                tracking = false;
                ++generation;
                if (!activity.hasWindowFocus() || !scene.attached()
                        || scene.mainPill.getVisibility() != View.VISIBLE)
                    return;
                if (inside(scene.mainPill,e) || inside(scene.searchPill,e))
                    return; // Nav capsule has its OWN verified gesture router.
                if (e.getPointerCount()!=1) return;
                int nativePage = scene.nativeSelectedOriginalPage();
                startingPage = nativePage>=0
                        ? nativePage : scene.visualOriginalPage();
                if (positionOf(startingPage)<0)return;
                downX = e.getRawX();
                downY = e.getRawY();
                pointer = e.getPointerId(0);
                downTime = SystemClock.uptimeMillis();
                tracking = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                generation++;
                break;
            case MotionEvent.ACTION_UP:
                if (!tracking || e.getPointerCount()!=1
                        || e.getPointerId(0)!=pointer) return;
                tracking = false;
                float dx = e.getRawX()-downX;
                float dy = e.getRawY()-downY;
                long elapsed = SystemClock.uptimeMillis()-downTime;
                if (elapsed<MIN_DURATION_MS || elapsed>MAX_DURATION_MS) return;
                float width = decor.getWidth();
                if (width<=0 || Math.abs(dx)<width*.26f
                        || Math.abs(dx)<Math.abs(dy)*1.7f) return;
                final int from=startingPage;
                final int direction=dx<0 ? 1 : -1;
                final int desired=neighbor(from,direction);
                final int token=++generation;
                // At the requested END of the five-page cycle, IG's native
                // order has Search BEFORE Profile. A left swipe from Search
                // would incorrectly advance to Profile. Undo only if the
                // real native page actually changed (never touch carousel).
                if (desired<0) {
                    if (from==3 && direction==1)
                        decor.postDelayed(() -> restoreSearchEnd(token),510);
                    return;
                }
                final boolean profileHeaderException=(from==4
                        && direction==1 && dx<0
                        && downY>decor.getHeight()*.055f
                        && downY<decor.getHeight()*.38f
                        && downX>decor.getWidth()*.12f
                        && downX<decor.getWidth()*.88f
                        && Math.abs(dx)>=width*.36f);
                DevFileLog.note("r17 page swipe observed orig="+from
                        +" intended="+desired
                        +" direction="+(direction==1?"next":"previous")
                        +" profileHeader="+profileHeaderException);
                decor.postDelayed(() -> evaluate(token,from,direction,desired,
                        profileHeaderException,false),420);
                break;
            default:
                break;
        }
    }

    private void evaluate(int token,int from,int direction,int desired,
                          boolean profileHeaderException,boolean second) {
        if(disposed || token!=generation || !scene.attached()
                || !activity.hasWindowFocus()
                || scene.mainPill.getVisibility()!=View.VISIBLE)
            return;
        // Never redirect an opened chat, story, another user's profile or
        // modal comment sheet. This is only native MAIN-tab remapping.
        if (DevScreenDetector.hiddenBecause(activity,scene.nav,
                scene.sourceParent)!=null) return;

        // The native selection may not change until pager settling ends.
        scene.syncSelection();
        int nativeAfter=scene.nativeSelectedOriginalPage();
        int seen=nativeAfter>=0?nativeAfter:scene.visualOriginalPage();
        int nativeDelta=seen-from;
        boolean genuineNativeTurn=seen!=from
                && Math.abs(nativeDelta)<=2
                && Integer.signum(nativeDelta)==direction;
        if (!genuineNativeTurn && !profileHeaderException) {
            if (!second) {
                decor.postDelayed(() -> evaluate(token,from,direction,desired,
                        false,true),360);
            } else {
                DevFileLog.note("r17 ignored ordinary horizontal swipe "
                        + "from="+from+" selectedNow="+seen
                        +" nativeSelected="+nativeAfter);
            }
            return;
        }
        if(seen==desired) {
            DevFileLog.note("r17 native swipe already matches requested page "
                    + "orig="+seen);
            return;
        }
        // Profile -> Search is a special case at the native pager boundary.
        // Restrict to the user's own header, never an unrelated Profile page.
        if(profileHeaderException && !genuineNativeTurn
                && !"OWN_PROFILE".equals(DevScreenDetector.pageName(activity))) {
            DevFileLog.note("r17 profile-to-search ignored: not own profile");
            return;
        }

        // Navigate using exactly the original tab listener; no fake screen,
        // no synthetic swipe on the video, and no ViewPager mutation.
        boolean handled=scene.navigateOriginalPage(desired,
                "contentSwipe:"+from+"->"+seen);
        DevFileLog.note("r17 content swipe routed orig="+from
                +" nativeAfter="+seen+" target="+desired
                +" handled="+handled);
    }

    private void restoreSearchEnd(int token) {
        if(disposed || token!=generation || !scene.attached()
                || !activity.hasWindowFocus()
                || scene.mainPill.getVisibility()!=View.VISIBLE) return;
        if(DevScreenDetector.hiddenBecause(activity,scene.nav,
                scene.sourceParent)!=null) return;
        scene.syncSelection();
        int nativeAfter=scene.nativeSelectedOriginalPage();
        int seen=nativeAfter>=0?nativeAfter:scene.visualOriginalPage();
        if(seen==4) {
            boolean handled=scene.navigateOriginalPage(3,"search-last-page");
            DevFileLog.note("r17 Search end-of-order guarded native bounce "
                    +"handled="+handled);
        }
    }

    private static int positionOf(int original) {
        for (int i=0;i<ORDER.length;i++)
            if(ORDER[i]==original) return i;
        return -1;
    }

    private static int neighbor(int original,int direction) {
        int i=positionOf(original)+direction;
        return i>=0 && i<ORDER.length ? ORDER[i] : -1;
    }

    private static boolean inside(View v, MotionEvent e) {
        if(v==null||v.getVisibility()!=View.VISIBLE) return false;
        int[] xy=new int[2];
        v.getLocationOnScreen(xy);
        float x=e.getRawX(),y=e.getRawY();
        return x>=xy[0]&&x<=xy[0]+v.getWidth()
                && y>=xy[1]&&y<=xy[1]+v.getHeight();
    }
}
