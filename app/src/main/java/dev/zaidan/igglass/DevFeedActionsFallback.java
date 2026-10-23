package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Beta3.3: an opt-in-by-detection optical fallback for the Home feed
 * when Instagram custom-draws actions and leaves no accessible action IDs.
 *
 * No fixed-Y painting on a vertically scrolling feed: require at least three
 * visible numeric action counters on ONE horizontal row. This deliberately
 * fails closed on views without accessible counter labels.
 *
 * No changes to Instagram click targets / listeners / media / layout.
 * Aggregated diagnostics only; count contents are neither logged nor saved.
 */
final class DevFeedActionsFallback {
    private static final long POLL_MS = 2000L;
    private final Activity activity;
    private final FrameLayout host;
    private final float density;
    private final DevReelsRailFallback.RailMaterial material;
    private boolean added;
    private long lastPoll;
    private long lastLog;
    private int previousTargets=-1;

    DevFeedActionsFallback(Activity activity, FrameLayout host) {
        this.activity=activity;
        this.host=host;
        density=activity.getResources().getDisplayMetrics().density;
        material=new DevReelsRailFallback.RailMaterial(activity);
        material.setClickable(false);
        material.setLongClickable(false);
        material.setFocusable(false);
        material.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void sync(DevGlassUi.Scene scene, boolean showHome) {
        if (!showHome || scene == null || !scene.attached()
                || scene.visualOriginalPage()!=0 || scene.isReelsActive()
                || !activity.hasWindowFocus()
                || activity.isFinishing() || activity.isDestroyed()) {
            clear();
            return;
        }
        if (!added) {
            host.addView(material,new FrameLayout.LayoutParams(-1,-1));
            added=true;
            lastPoll=0;
        }
        long now=SystemClock.uptimeMillis();
        if (now-lastPoll < POLL_MS) return;
        lastPoll=now;
        ArrayList<Rect> candidates = new ArrayList<>();
        int[] budget = {1800};
        int bottom = navTop(scene);
        collect(host,candidates,budget,0,bottom);
        ArrayList<RectF> targets = new ArrayList<>();
        ArrayList<Rect> best = bestRow(candidates);
        if (best.size() >= 3) {
            int y=best.get(0).centerY();
            int radius=dp(22);
            ArrayList<Integer> actionXs=new ArrayList<>();
            for (Rect counter : best) {
                int x=counter.left-dp(22);
                if (x > dp(20) && x < host.getWidth()*.88f)
                    actionXs.add(x);
            }
            Collections.sort(actionXs);
            if (actionXs.size()>=3) {
                // For rows with 3 counters (like/repost/share), comment has
                // no visible number. Place its lens between the first two.
                if (actionXs.size()==3) {
                    int inserted=actionXs.get(0)+
                            Math.round((actionXs.get(1)-actionXs.get(0))*.63f);
                    actionXs.add(1,inserted);
                }
                // Save is located at the right of the Home action row.
                actionXs.add(Math.round(host.getWidth()*.940f));
                for (int x : actionXs) {
                    RectF box=new RectF(x-radius,y-radius,x+radius,y+radius);
                    if (box.left<0 || box.right>host.getWidth()
                            || box.top<host.getHeight()*.2f
                            || box.bottom>=bottom-dp(5)
                            || overlap(box,scene.mainPill)
                            || overlap(box,scene.searchPill))
                        continue;
                    boolean duplicate=false;
                    for (RectF old:targets) {
                        if (RectF.intersects(old,box)) { duplicate=true;break; }
                    }
                    if (!duplicate) targets.add(box);
                }
            }
        }
        material.setTargets(targets);
        if (targets.size()!=previousTargets || now-lastLog>8500L) {
            previousTargets=targets.size();
            lastLog=now;
            DevFileLog.note("beta3.3 feed action fallback candidates="
                    +candidates.size()+" row="+best.size()
                    +" circles="+targets.size()+" no-fixed-scroll-position=true");
        }
    }

    private ArrayList<Rect> bestRow(ArrayList<Rect> candidates) {
        ArrayList<Rect> strongest=new ArrayList<>();
        for (Rect a:candidates) {
            ArrayList<Rect> row=new ArrayList<>();
            for (Rect b:candidates)
                if (Math.abs(b.centerY()-a.centerY()) <= dp(13))
                    row.add(b);
            if (row.size()>strongest.size())
                strongest=row;
        }
        if (strongest.size()<3 || strongest.size()>5)
            return new ArrayList<>();
        Collections.sort(strongest, Comparator.comparingInt(v->v.centerX()));
        // Reject cramped/irregular groups that could be unrelated post text.
        if (strongest.get(strongest.size()-1).centerX()
                -strongest.get(0).centerX()<host.getWidth()*.26f)
            return new ArrayList<>();
        return strongest;
    }

    private void collect(View view,ArrayList<Rect> out,int[] budget,
                         int depth,int navTop) {
        if (view==null || view==material || budget[0]--<=0
                || depth>23 || !view.isShown()) return;
        if (view instanceof TextView && out.size()<35) {
            CharSequence cs=((TextView)view).getText();
            if (cs!=null && cs.length()>0 && cs.length()<=11
                    && numeric(cs.toString())) {
                Rect r=new Rect();
                if (view.getGlobalVisibleRect(r)) {
                    int[] loc=new int[2];
                    host.getLocationOnScreen(loc);
                    r.offset(-loc[0],-loc[1]);
                    if (r.centerX()>host.getWidth()*.08f
                            && r.centerX()<host.getWidth()*.83f
                            && r.centerY()>host.getHeight()*.28f
                            && r.bottom<navTop-dp(22)
                            && r.width()<dp(92)
                            && r.height()<dp(34))
                        out.add(r);
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)view;
            int limit=Math.min(g.getChildCount(),110);
            for(int i=0;i<limit && budget[0]>0;i++)
                collect(g.getChildAt(i),out,budget,depth+1,navTop);
        }
    }

    private static boolean numeric(String v) {
        String s=v.replace(" ","").toUpperCase(Locale.ROOT);
        return s.matches("[0-9][0-9.,]{0,9}(RB|JT|K|M|B)?");
    }

    private int navTop(DevGlassUi.Scene scene) {
        int[] base=new int[2], pill=new int[2];
        host.getLocationOnScreen(base);
        scene.mainPill.getLocationOnScreen(pill);
        return pill[1]-base[1];
    }

    private boolean overlap(RectF target,View v) {
        if (v==null || v.getVisibility()!=View.VISIBLE) return false;
        int[] xy=new int[2],base=new int[2];
        v.getLocationOnScreen(xy);
        host.getLocationOnScreen(base);
        return RectF.intersects(target,new RectF(xy[0]-base[0],xy[1]-base[1],
                xy[0]-base[0]+v.getWidth(),xy[1]-base[1]+v.getHeight()));
    }

    void clear() {
        if (!added) return;
        added=false;
        lastPoll=0;
        material.setTargets(new ArrayList<>());
        ViewParent parent=material.getParent();
        if (parent instanceof ViewGroup) ((ViewGroup)parent).removeView(material);
    }
    private int dp(float val) {return Math.round(val*density);}
}
