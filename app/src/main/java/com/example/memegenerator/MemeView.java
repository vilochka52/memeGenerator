package com.example.memegenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Фон (fit-center) + тексты. Перетаскивание без ограничений.
 * Двойной тап по тексту -> запрос редактирования.
 * По отпусканию перетаскивания шлёт колбэк с новыми координатами (чтобы VM запомнила).
 */
public class MemeView extends View {

    @Nullable private Bitmap bg;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TextItem> items = Collections.emptyList();

    private final Rect bgSrc = new Rect();
    private final Rect bgDst = new Rect();

    private int draggingIndex = -1;
    private float dragDx = 0f, dragDy = 0f;
    private boolean moved = false;

    private final GestureDetector gestureDetector;

    public interface OnTextEditRequestListener { void onRequestEdit(int index, TextItem item); }
    public interface OnTextMovedListener { void onTextMoved(int index, TextItem itemWithNewPos); }

    private OnTextEditRequestListener editListener;
    private OnTextMovedListener movedListener;

    public MemeView(Context c) { super(c); gestureDetector = gd(c); init(); }
    public MemeView(Context c, AttributeSet a) { super(c, a); gestureDetector = gd(c); init(); }
    public MemeView(Context c, AttributeSet a, int s) { super(c, a, s); gestureDetector = gd(c); init(); }

    private void init() {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setSubpixelText(true);
    }

    private GestureDetector gd(Context ctx) {
        return new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (items.isEmpty()) return false;
                int idx = findClosestTextIndex(e.getX(), e.getY());
                if (idx >= 0 && editListener != null) { editListener.onRequestEdit(idx, items.get(idx)); return true; }
                return false;
            }
            @Override public boolean onDown(MotionEvent e) { return true; }
        });
    }

    public void setOnTextEditRequestListener(OnTextEditRequestListener l) { this.editListener = l; }
    public void setOnTextMovedListener(OnTextMovedListener l) { this.movedListener = l; }

    public void setBackgroundBitmap(@Nullable Bitmap newBg) {
        // рециклим только если это не HARDWARE
        if (bg != null && !bg.isRecycled() && !(Build.VERSION.SDK_INT >= 26 && bg.getConfig() == Bitmap.Config.HARDWARE)) {
            bg.recycle();
        }
        bg = newBg;
        requestLayout();
        invalidate();
    }

    public void setTextItems(List<TextItem> list) {
        items = (list == null) ? Collections.emptyList() : new ArrayList<>(list);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeBgRects(w, h);
    }

    private void computeBgRects(int vw, int vh) {
        bgDst.set(0, 0, vw, vh);
        if (bg == null) return;
        int bw = bg.getWidth(), bh = bg.getHeight();
        bgSrc.set(0, 0, bw, bh);

        float viewRatio = vw / (float) vh;
        float bmpRatio  = bw / (float) bh;

        if (bmpRatio > viewRatio) {
            int dstW = vw;
            int dstH = Math.round(vw / bmpRatio);
            int top  = (vh - dstH) / 2;
            bgDst.set(0, top, dstW, top + dstH);
        } else {
            int dstH = vh;
            int dstW = Math.round(vh * bmpRatio);
            int left = (vw - dstW) / 2;
            bgDst.set(left, 0, left + dstW, dstH);
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bg != null) {
            if (bgDst.width() == 0 || bgDst.height() == 0) computeBgRects(getWidth(), getHeight());
            canvas.drawBitmap(bg, bgSrc, bgDst, null);
        }
        for (TextItem item : items) {
            textPaint.setTextSize(sp(item.textSizeSp));
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            textPaint.setColor(item.color);
            canvas.drawText(item.text, item.x, item.y, textPaint);
        }
    }

    private float sp(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    public Bitmap exportToBitmap() {
        Bitmap out = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        draw(c);
        return out;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        if (items.isEmpty()) return super.onTouchEvent(e);

        float x = e.getX(), y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                draggingIndex = findClosestTextIndex(x, y);
                moved = false;
                if (draggingIndex >= 0) {
                    TextItem item = items.get(draggingIndex);
                    dragDx = x - item.x; dragDy = y - item.y;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (draggingIndex >= 0) {
                    float nx = x - dragDx;
                    float ny = y - dragDy;
                    // БЕЗ ограничений: можно увести за любые края
                    TextItem old = items.get(draggingIndex);
                    items.set(draggingIndex, old.withPosition(nx, ny));
                    moved = true;
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingIndex >= 0 && moved && movedListener != null) {
                    movedListener.onTextMoved(draggingIndex, items.get(draggingIndex));
                }
                draggingIndex = -1;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onTouchEvent(e);
    }

    private int findClosestTextIndex(float touchX, float touchY) {
        int idx = -1; float best = Float.MAX_VALUE;
        for (int i = 0; i < items.size(); i++) {
            TextItem it = items.get(i);
            float dx = it.x - touchX, dy = it.y - touchY, d = dx*dx + dy*dy;
            if (d < best) { best = d; idx = i; }
        }
        return idx;
    }
}
