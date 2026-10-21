package dev.zaidan.igglass;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Prevent the verified IG452 native layouts from recreating tab-bar gaps.
 * Instead of repairing a 180px margin AFTER Instagram relayout (r13),
 * clear it in the same setLayoutParams call that Instagram initiates.
 * This avoids a second layout pass while SurfaceView is decoding Reels.
 */
final class DevNativeMarginGuard {
    private static final WeakHashMap<View, Integer> MONITORED =
            new WeakHashMap<>();
    private static boolean installed;
    private DevNativeMarginGuard() {}

    static synchronized void install() {
        if(installed) return;
        try {
            XposedHelpers.findAndHookMethod(View.class,"setLayoutParams",
                    ViewGroup.LayoutParams.class,new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    View view=(View)p.thisObject;
                    Integer reserved;
                    synchronized(MONITORED){ reserved=MONITORED.get(view); }
                    if(reserved==null) return;
                    ViewGroup.LayoutParams params=(ViewGroup.LayoutParams)p.args[0];
                    if(!(params instanceof ViewGroup.MarginLayoutParams))return;
                    ViewGroup.MarginLayoutParams margins=
                            (ViewGroup.MarginLayoutParams)params;
                    if(margins.bottomMargin == reserved) {
                        margins.bottomMargin=0;
                        DevFileLog.note("r14 prevented native bottom reservation="
                                +reserved+"px id="+DevGlassUi.resourceName(view));
                    }
                }
            });
            installed=true;
            DevFileLog.note("r14 native margin intercept installed");
        }catch(Throwable e){
            DevFileLog.note("r14 native margin intercept unavailable "
                    +e.getClass().getSimpleName());
        }
    }

    static void watch(View v,int reserved) {
        if(v==null||reserved<=0)return;
        synchronized(MONITORED){MONITORED.put(v,reserved);}
    }
    static void unwatch(View v) {
        if(v==null)return;
        synchronized(MONITORED){MONITORED.remove(v);}
    }

    static void clearKnownChildren(ViewGroup p) {
        if(p==null)return;
        for(int i=0;i<p.getChildCount();i++) {
            View v=p.getChildAt(i);
            String id=DevGlassUi.resourceName(v);
            if("layout_container_main".equals(id)
                    ||"swipeable_tab_view_pager".equals(id)
                    ||"layout_bottom_searchbar".equals(id))
                unwatch(v);
        }
    }
}
