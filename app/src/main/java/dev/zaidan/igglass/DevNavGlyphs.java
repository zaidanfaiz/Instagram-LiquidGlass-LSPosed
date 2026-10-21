package dev.zaidan.igglass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Only the four native Instagram tab surfaces are made visually invisible.
 * They remain attached and retain ALL real onClick/onTouch listeners.
 *
 * This independent, non-clickable icon layer draws crisp white symbols with
 * subtle dark contour so original Instagram's opaque tab painting can NEVER
 * fill the entire custom glass surface again.
 */
final class DevNavGlyphs extends View {
    private final ViewGroup nativeTabs;
    private final Paint foreground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path symbol = new Path();
    private final Path avatarClip = new Path();
    private final RectF oval = new RectF();
    private final float density;
    private int selectedIndex;

    DevNavGlyphs(Context context, ViewGroup nativeTabs) {
        super(context);
        this.nativeTabs = nativeTabs;
        density = getResources().getDisplayMetrics().density;
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
        backing.setStyle(Paint.Style.STROKE);
        backing.setStrokeWidth(5.5f * density);
        backing.setStrokeJoin(Paint.Join.ROUND);
        backing.setStrokeCap(Paint.Cap.ROUND);
        backing.setColor(0xBF0B131D);
        foreground.setStyle(Paint.Style.STROKE);
        foreground.setStrokeWidth(2.8f * density);
        foreground.setStrokeJoin(Paint.Join.ROUND);
        foreground.setStrokeCap(Paint.Cap.ROUND);
        foreground.setColor(0xFFF9FCFF);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFF9FCFF);
    }

    void activate(int index) {
        selectedIndex = index;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = nativeTabs.getChildCount();
        if (count < 4 || nativeTabs.getWidth() < 1) return;
        for (int index = 0; index < 4; index++) {
            View item = nativeTabs.getChildAt(index);
            float x = nativeTabs.getLeft()
                    + (item.getLeft() + item.getRight()) * .5f;
            float y = nativeTabs.getTop()
                    + (item.getTop() + item.getBottom()) * .5f;
            float size = Math.min(24f * density,
                    Math.min(item.getWidth() * .31f, item.getHeight() * .37f));
            if (size <= 0) continue;
            int saved = canvas.save();
            canvas.translate(x, y);
            if (index == 0) drawHome(canvas,size);
            else if (index == 1) drawReels(canvas,size);
            else if (index == 2) drawInbox(canvas,size);
            else drawProfile(canvas,size,item);
            canvas.restoreToCount(saved);
        }
    }

    private void drawPath(Canvas canvas,Path path) {
        canvas.drawPath(path,backing);
        canvas.drawPath(path,foreground);
    }

    private void drawHome(Canvas canvas,float r) {
        symbol.reset();
        symbol.moveTo(-.79f*r,-.08f*r);
        symbol.lineTo(0,-.81f*r);
        symbol.lineTo(.79f*r,-.08f*r);
        symbol.lineTo(.79f*r,.80f*r);
        symbol.lineTo(.28f*r,.80f*r);
        symbol.lineTo(.28f*r,.22f*r);
        symbol.lineTo(-.28f*r,.22f*r);
        symbol.lineTo(-.28f*r,.80f*r);
        symbol.lineTo(-.79f*r,.80f*r);
        symbol.close();
        drawPath(canvas,symbol);
    }

    private void drawReels(Canvas canvas,float r) {
        float corner=.34f*r;
        oval.set(-.79f*r,-.73f*r,.79f*r,.73f*r);
        canvas.drawRoundRect(oval,corner,corner,backing);
        canvas.drawRoundRect(oval,corner,corner,foreground);
        symbol.reset();
        symbol.moveTo(-.23f*r,-.41f*r);
        symbol.lineTo(.50f*r,0);
        symbol.lineTo(-.23f*r,.41f*r);
        symbol.close();
        canvas.drawPath(symbol,fill);
    }

    private void drawInbox(Canvas canvas,float r) {
        symbol.reset();
        symbol.moveTo(-.85f*r,-.45f*r);
        symbol.quadTo(-.90f*r,-.66f*r,-.60f*r,-.63f*r);
        symbol.lineTo(.83f*r,-.55f*r);
        symbol.quadTo(1.03f*r,-.53f*r,.83f*r,-.25f*r);
        symbol.lineTo(.15f*r,.84f*r);
        symbol.quadTo(.02f*r,1.00f*r,-.10f*r,.75f*r);
        symbol.lineTo(-.26f*r,.14f*r);
        symbol.close();
        drawPath(canvas,symbol);
        symbol.reset();
        symbol.moveTo(-.25f*r,.14f*r);
        symbol.lineTo(.56f*r,-.34f*r);
        drawPath(canvas,symbol);
    }

    private void drawProfile(Canvas canvas,float r,View item) {
        View avatar = findAvatar(item,0);
        if (avatar != null && avatar.getWidth() > 0 && avatar.getHeight() > 0) {
            int checkpoint=canvas.save();
            avatarClip.reset();
            avatarClip.addCircle(0,0,.80f*r,Path.Direction.CW);
            canvas.clipPath(avatarClip);
            canvas.scale(1.6f*r/avatar.getWidth(),
                    1.6f*r/avatar.getHeight());
            canvas.translate(-avatar.getWidth()/2f,-avatar.getHeight()/2f);
            avatar.draw(canvas);
            canvas.restoreToCount(checkpoint);
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(2.2f*density);
            border.setColor(0xFFF8FCFF);
            canvas.drawCircle(0,0,.80f*r,border);
            return;
        }
        // Fall back to a legible outline rather than a fake account image.
        canvas.drawCircle(0,-.36f*r,.34f*r,backing);
        canvas.drawCircle(0,-.36f*r,.34f*r,foreground);
        symbol.reset();
        symbol.moveTo(-.68f*r,.74f*r);
        symbol.cubicTo(-.66f*r,.06f*r,.66f*r,.06f*r,.68f*r,.74f*r);
        drawPath(canvas,symbol);
    }

    private View findAvatar(View v,int depth) {
        if (depth > 5 || v == null) return null;
        int low=(int)(21*density), high=(int)(88*density);
        if (v.getWidth() >= low && v.getWidth() <= high
                && v.getHeight() >= low && v.getHeight() <= high
                && Math.abs(v.getWidth()-v.getHeight()) < v.getWidth()*.28f) {
            if (v instanceof ImageView
                    && ((ImageView)v).getDrawable() != null) return v;
            String name=DevGlassUi.resourceName(v).toLowerCase();
            if (name.contains("avatar") || name.contains("profile_picture")
                    || name.contains("profile_photo")) return v;
        }
        if (v instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)v;
            for(int i=0; i<group.getChildCount(); i++) {
                View found=findAvatar(group.getChildAt(i),depth+1);
                if(found!=null)return found;
            }
        }
        return null;
    }
}
