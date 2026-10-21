package dev.zaidan.igglass;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

/**
 * Targets only the last verified 60px of Instagram 452's root coordinator.
 *
 * Runtime logs for 1440x3200 show:
 *  SwipeNavigationContainer height 3200
 *   layout_container_center_right_coordinator_layout height 3140
 *    LayoutParams.bottomMargin=60
 *
 * This is distinct from the 180px native tab reservation fixed separately.
 * Only clear a small app-owned inset while system navbar reports no inset.
 * Restore the original layout when leaving the main feed. No system UI flags
 * or Window color values are changed.
 */
final class DevEdgeSpaceFix {
    private DevEdgeSpaceFix() {}

    static final class Session {
        private final Activity activity;
        private final View coordinator;
        private final int originalMargin;
        private boolean applied;

        Session(Activity activity, View coordinator, int margin) {
            this.activity = activity;
            this.coordinator = coordinator;
            this.originalMargin = margin;
        }

        void setActive(boolean visibleOnFeed) {
            if (coordinator.getParent() == null) return;
            ViewGroup.LayoutParams params = coordinator.getLayoutParams();
            if (!(params instanceof ViewGroup.MarginLayoutParams)) return;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) params;
            if (visibleOnFeed && !applied) {
                if (lp.bottomMargin != originalMargin || !safeSystemBars(activity)) {
                    return;
                }
                lp.bottomMargin = 0;
                coordinator.setLayoutParams(lp);
                applied = true;
                DevNativeMarginGuard.watch(coordinator,originalMargin);
                DevFileLog.note("edge-space: reclaimed verified " + originalMargin
                        + "px root coordinator bottom margin");
            } else if (!visibleOnFeed && applied) {
                if (lp.bottomMargin == 0) {
                    lp.bottomMargin = originalMargin;
                    coordinator.setLayoutParams(lp);
                }
                applied = false;
                DevFileLog.note("edge-space: restored " + originalMargin
                        + "px inset for non-feed screen");
            } else if (visibleOnFeed && applied && lp.bottomMargin == originalMargin) {
                // Instagram may reapply its native margin on a tab switch.
                lp.bottomMargin = 0;
                coordinator.setLayoutParams(lp);
                DevFileLog.note("edge-space: maintained zero root inset");
            }
        }

        void unregister() {
            DevNativeMarginGuard.unwatch(coordinator);
        }

        void restore() {
            DevNativeMarginGuard.unwatch(coordinator);
            setActive(false);
        }
    }

    static Session inspect(Activity activity, View originalParent) {
        try {
            ViewGroup container = null;
            View current = originalParent;
            for (int n = 0; n < 6 && current != null; n++) {
                if ("layout_container_center_right_coordinator_layout".equals(
                        DevGlassUi.resourceName(current))
                        && current.getParent() instanceof ViewGroup
                        && "swipe_navigation_container".equals(
                                DevGlassUi.resourceName((View) current.getParent()))) {
                    container = (ViewGroup) current.getParent();
                    break;
                }
                current = current.getParent() instanceof View
                        ? (View) current.getParent() : null;
            }
            if (container == null || current == null) {
                DevFileLog.note("edge-space: expected 452 container not found");
                return null;
            }
            ViewGroup.LayoutParams params = current.getLayoutParams();
            if (!(params instanceof ViewGroup.MarginLayoutParams)) return null;
            int bottom = ((ViewGroup.MarginLayoutParams) params).bottomMargin;
            float density = current.getResources().getDisplayMetrics().density;
            int min = Math.round(8 * density);
            int max = Math.round(24 * density);
            int actualGap = container.getHeight() - current.getHeight();
            if (bottom < min || bottom > max || Math.abs(actualGap - bottom) > 3) {
                DevFileLog.note("edge-space: unchanged native bottom="
                        + bottom + "px gap=" + actualGap + "px");
                return null;
            }
            if (!safeSystemBars(activity)) {
                DevFileLog.note("edge-space: system navigation inset is nonzero");
                return null;
            }
            DevFileLog.note("edge-space: verified root gap " + bottom + "px");
            return new Session(activity, current, bottom);
        } catch (Throwable e) {
            DevFileLog.note("edge-space: inspect error " + e.getClass().getSimpleName());
            return null;
        }
    }

    private static boolean safeSystemBars(Activity activity) {
        if (activity.getWindow().getNavigationBarColor() != Color.TRANSPARENT) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsets insets = activity.getWindow().getDecorView()
                    .getRootWindowInsets();
            if (insets == null) return false;
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom == 0;
        }
        return false;
    }
}
