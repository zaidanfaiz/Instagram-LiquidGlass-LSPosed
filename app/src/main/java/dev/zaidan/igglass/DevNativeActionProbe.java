package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Passive action-renderer discovery for verified Instagram 452.
 *
 * Does NOT add a view, overlay, drawable, listener or bitmap capture.
 * Only reports class/ID/count/geometry of existing VIEW ancestry over
 * five expected Reels action positions. This is intentionally a probe,
 * not a claim that these are five native button Views.
 *
 * NEVER reads or records TextView text, content descriptions, account
 * names, media pixels, URLs or user content.
 */
final class DevNativeActionProbe {
    private static final long INTERVAL_MS = 6500L;
    private static final int MAX_NODES = 1800;
    private static final int MAX_DEPTH = 25;
    private final Activity activity;
    private final View root;
    private long lastProbe;

    DevNativeActionProbe(Activity activity, View root) {
        this.activity = activity;
        this.root = root;
    }

    void inspect(DevGlassUi.Scene scene, boolean visible) {
        if (!visible || scene == null || !scene.attached()
                || !scene.isReelsActive() || !activity.hasWindowFocus()
                || activity.isFinishing() || activity.isDestroyed()) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastProbe < INTERVAL_MS) return;
        lastProbe = now;
        int[] decor = new int[2];
        root.getLocationOnScreen(decor);
        int width = root.getWidth(), height = root.getHeight();
        if (width < 320 || height < 650) return;
        int x = decor[0] + Math.round(width * .92f);
        float[] ys = {.493f,.575f,.655f,.734f,.807f};
        String[] names = {"like","comment","repost","share","save"};
        for (int index = 0; index < ys.length; index++) {
            int y = decor[1] + Math.round(height * ys[index]);
            ArrayList<Candidate> candidates = new ArrayList<>();
            scan(root,x,y,0,new int[]{MAX_NODES},candidates);
            Collections.sort(candidates, Comparator.comparingLong(c -> c.area));
            StringBuilder info = new StringBuilder();
            int limit = Math.min(4,candidates.size());
            for(int i=0;i<limit;i++) {
                Candidate c=candidates.get(i);
                if(i!=0)info.append(" <- ");
                info.append(c.view.getClass().getName());
                info.append("(").append(c.width).append("x")
                        .append(c.height).append(",click=")
                        .append(c.view.isClickable());
                String id=resourceId(c.view);
                if(!id.isEmpty())info.append(",id=").append(id);
                info.append(")");
            }
            // A huge common container is NOT an addressable button.
            boolean independent = !candidates.isEmpty()
                    && candidates.get(0).width < width*.18f
                    && candidates.get(0).height < height*.15f;
            DevFileLog.note("beta3 native-probe action="+names[index]
                    +" independentView="+independent
                    +" matches="+candidates.size()
                    +" path="+(info.length()==0?"none":info.toString()));
        }
    }

    private void scan(View view, int px, int py, int depth,
                      int[] left, ArrayList<Candidate> found) {
        if (view==null || left[0]--<=0 || depth>MAX_DEPTH
                || !view.isShown() || view.getAlpha()<.10f
                || view.getWidth()<1 || view.getHeight()<1) return;
        Rect rect=new Rect();
        if (!view.getGlobalVisibleRect(rect) || !rect.contains(px,py))
            return;
        if (found.size() < 100) {
            Candidate c=new Candidate();
            c.view=view;
            c.width=view.getWidth();
            c.height=view.getHeight();
            c.area=(long)c.width*c.height;
            found.add(c);
        }
        if (view instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)view;
            int count=Math.min(g.getChildCount(),130);
            for(int i=0;i<count && left[0]>0;i++)
                scan(g.getChildAt(i),px,py,depth+1,left,found);
        }
    }

    private static String resourceId(View v) {
        if(v.getId()==View.NO_ID)return "";
        try {
            String id=v.getResources().getResourceEntryName(v.getId());
            return id.length()>72?id.substring(0,72):id;
        } catch (Throwable t) { return ""; }
    }

    private static final class Candidate {
        View view;
        int width;
        int height;
        long area;
    }
}
