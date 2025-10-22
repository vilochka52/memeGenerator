package com.example.memegenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemeView extends View {
    @Nullable private Bitmap baseOriginal = null;
    private final Matrix imageMatrix = new Matrix();
    private final RectF srcRect = new RectF();
    private final RectF dstRect = new RectF();
    private final Paint imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private float baseScale = 1f;
    private float baseOffsetX = 0f;
    private float baseOffsetY = 0f;

    private int contentBottomInsetPx = 0;

    @Nullable private Bitmap bg;
    private final Rect bgSrc = new Rect();
    private final Rect bgDst = new Rect();

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TextItem> items = Collections.emptyList();

    private int selectedTextIndex = -1;
    private boolean resizingText = false;
    private float resizeStartX;
    private int draggingIndex = -1;
    private float dragDx = 0f, dragDy = 0f;
    private boolean moved = false;

    private static final float HANDLE_SIZE_DP = 16f;
    private static final float BOX_STROKE_DP  = 1.5f;

    private final GestureDetector gestureDetector;

    public interface OnTextEditRequestListener { void onRequestEdit(int index, TextItem item); }
    public interface OnTextMovedListener       { void onTextMoved(int index, TextItem itemWithNewPos); }
    private OnTextEditRequestListener editListener;
    private OnTextMovedListener movedListener;

    public MemeView(Context c) {
        super(c);
        gestureDetector = createGestureDetector(c);
        init();
    }
    public MemeView(Context c, AttributeSet a) {
        super(c, a);
        gestureDetector = createGestureDetector(c);
        init();
    }
    public MemeView(Context c, AttributeSet a, int s) {
        super(c, a, s);
        gestureDetector = createGestureDetector(c);
        init();
    }

    private void init() {
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setSubpixelText(true);
        setWillNotDraw(false);
    }

    private GestureDetector createGestureDetector(Context ctx) {
        return new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) {
                if (items.isEmpty()) return false;
                int idx = hitTestTextIndex(e.getX(), e.getY());
                if (idx >= 0 && editListener != null) {
                    editListener.onRequestEdit(idx, items.get(idx));
                    return true;
                }
                return false;
            }
            @Override public boolean onDown(MotionEvent e) { return true; }
        });
    }

    public void setOnTextEditRequestListener(OnTextEditRequestListener l) { this.editListener = l; }
    public void setOnTextMovedListener(OnTextMovedListener l) { this.movedListener = l; }

    public void setBackgroundBitmap(@Nullable Bitmap newBg) {
        if (bg != null && !bg.isRecycled()
                && !(Build.VERSION.SDK_INT >= 26 && bg.getConfig() == Bitmap.Config.HARDWARE)) {
            bg.recycle();
        }
        bg = newBg;
        requestLayout();
        invalidate();
    }

    public void setTextItems(@Nullable List<TextItem> list) {
        items = (list == null) ? Collections.emptyList() : new ArrayList<>(list);
        invalidate();
    }

    public void addImageBitmap(@Nullable Bitmap bmp) {
        if (bmp == null) return;
        baseOriginal = bmp;
        recomputeImageMatrix();
        invalidate();
    }

    public boolean hasImage() {
        return baseOriginal != null && !baseOriginal.isRecycled();
    }

    public void setContentBottomInsetPx(int px) {
        if (px < 0) px = 0;
        if (contentBottomInsetPx != px) {
            contentBottomInsetPx = px;
            recomputeImageMatrix();
            invalidate();
        }
    }

    public Bitmap exportToBitmap() {
        Bitmap out = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        draw(c);
        return out;
    }

    public Bitmap exportToBitmapAtOriginal() {
        if (!hasImage()) {
            return exportToBitmap();
        }

        int outW = baseOriginal.getWidth();
        int outH = baseOriginal.getHeight();
        Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);

        c.drawBitmap(baseOriginal, 0f, 0f, null);

        float inv = (baseScale == 0f) ? 1f : (1f / baseScale);

        for (int i = 0; i < items.size(); i++) {
            TextItem item = items.get(i);

            TextPaint tp = new TextPaint(textPaint);
            tp.setTextSize(sp(item.textSizeSp) * inv);
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            tp.setColor(item.color);

            CharSequence cs = item.text == null ? "" : item.text;

            float widthPxExport = item.boxWidth > 0 ? item.boxWidth * inv : 0f;
            int lw = (int) Math.max(1, widthPxExport);

            StaticLayout layout;
            if (widthPxExport > 0f) {
                if (Build.VERSION.SDK_INT >= 23) {
                    layout = StaticLayout.Builder.obtain(cs, 0, cs.length(), tp, lw)
                            .setAlignment(mapAlign(item.align))
                            .setIncludePad(false)
                            .setLineSpacing(0, 1f)
                            .build();
                } else {
                    layout = new StaticLayout(cs, tp, lw, mapAlign(item.align), 1f, 0f, false);
                }
            } else {
                int singleW = (int) Math.ceil(tp.measureText(cs, 0, cs.length()));
                if (Build.VERSION.SDK_INT >= 23) {
                    layout = StaticLayout.Builder.obtain(cs, 0, cs.length(), tp, Math.max(1, singleW))
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setIncludePad(false)
                            .build();
                } else {
                    layout = new StaticLayout(cs, tp, Math.max(1, singleW),
                            Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
                }
            }

            float xOnOriginal;
            if (widthPxExport <= 0f) {
                float tw = tp.measureText(cs, 0, cs.length());
                if (item.align == TextItem.ALIGN_CENTER)      xOnOriginal = ((item.x - baseOffsetX) * inv) - tw / 2f;
                else if (item.align == TextItem.ALIGN_RIGHT)  xOnOriginal = ((item.x - baseOffsetX) * inv) - tw;
                else                                          xOnOriginal =  (item.x - baseOffsetX) * inv;
            } else {
                xOnOriginal = (item.x - baseOffsetX) * inv;
            }

            Paint.FontMetrics fm = tp.getFontMetrics();
            float baselineY = ((item.y - baseOffsetY) * inv);
            float topY = baselineY + fm.ascent;

            c.save();
            c.translate(xOnOriginal, topY);
            layout.draw(c);
            c.restore();
        }

        return out;
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeBgRects(w, h);
        recomputeImageMatrix();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (selectedTextIndex >= items.size()) selectedTextIndex = -1;
        if (draggingIndex >= items.size()) draggingIndex = -1;

        if (bg != null) {
            if (bgDst.width() == 0 || bgDst.height() == 0) computeBgRects(getWidth(), getHeight());
            canvas.drawBitmap(bg, bgSrc, bgDst, null);
        }

        if (hasImage()) {
            canvas.drawBitmap(baseOriginal, imageMatrix, imgPaint);
        }

        for (int i = 0; i < items.size(); i++) {
            TextItem item = items.get(i);

            float widthPx = item.boxWidth > 0 ? item.boxWidth : 0f;
            StaticLayout layout = buildLayout(item, widthPx);

            float drawX;
            if (widthPx <= 0f) {
                TextPaint tp = new TextPaint(textPaint);
                tp.setTextSize(sp(item.textSizeSp));
                tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
                float tw = tp.measureText(item.text == null ? "" : item.text);
                if (item.align == TextItem.ALIGN_CENTER)      drawX = item.x - tw / 2f;
                else if (item.align == TextItem.ALIGN_RIGHT)  drawX = item.x - tw;
                else                                          drawX = item.x;
            } else {
                drawX = item.x;
            }

            TextPaint tpForBaseline = new TextPaint(textPaint);
            tpForBaseline.setTextSize(sp(item.textSizeSp));
            tpForBaseline.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            Paint.FontMetrics fm = tpForBaseline.getFontMetrics();
            float topY = item.y + fm.ascent;

            canvas.save();
            canvas.translate(drawX, topY);
            layout.draw(canvas);
            canvas.restore();

            if (i == selectedTextIndex) {
                Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(dp(BOX_STROKE_DP));
                boxPaint.setColor(0x99FFFFFF);

                float boxW = (widthPx > 0f) ? widthPx : layout.getWidth();
                float boxH = layout.getHeight();

                canvas.drawRect(drawX, topY, drawX + boxW, topY + boxH, boxPaint);

                float hs = dp(HANDLE_SIZE_DP);
                float hx = drawX + boxW - hs;
                float hy = topY + boxH - hs;

                Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                handlePaint.setStyle(Paint.Style.FILL);
                handlePaint.setColor(Color.WHITE);
                canvas.drawRect(hx, hy, hx + hs, hy + hs, handlePaint);

                handlePaint.setStyle(Paint.Style.STROKE);
                handlePaint.setColor(0xFF000000);
                handlePaint.setStrokeWidth(dp(1));
                canvas.drawRect(hx, hy, hx + hs, hy + hs, handlePaint);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        if (items.isEmpty()) return super.onTouchEvent(e);

        float x = e.getX(), y = e.getY();

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int idx = hitTestTextIndex(x, y);
                if (idx >= 0) {
                    selectedTextIndex = idx;

                    if (isInResizeHandle(idx, x, y)) {
                        resizingText = true;
                        draggingIndex = idx;
                        resizeStartX = x;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }

                    draggingIndex = idx;
                    moved = false;

                    TextItem it = items.get(draggingIndex);
                    dragDx = x - it.x;
                    dragDy = y - it.y;

                    getParent().requestDisallowInterceptTouchEvent(true);
                    invalidate();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (draggingIndex >= 0) {
                    TextItem it = items.get(draggingIndex);
                    if (resizingText) {
                        float width = it.boxWidth > 0 ? it.boxWidth : measureSingleLineWidth(it);
                        float delta = x - resizeStartX;
                        float newWidth = Math.max(dp(60), width + delta);
                        items.set(draggingIndex, it.withBoxWidth(newWidth));
                        resizeStartX = x;
                        invalidate();
                        return true;
                    } else {
                        float nx = x - dragDx, ny = y - dragDy;
                        items.set(draggingIndex, it.withPosition(nx, ny));
                        moved = true;
                        invalidate();
                        return true;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (draggingIndex >= 0) {
                    if (movedListener != null) {
                        movedListener.onTextMoved(draggingIndex, items.get(draggingIndex));
                    }
                }
                resizingText = false;
                draggingIndex = -1;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            }
        }
        return super.onTouchEvent(e);
    }

    private void recomputeImageMatrix() {
        if (baseOriginal == null || getWidth() == 0 || getHeight() == 0) return;

        srcRect.set(0, 0, baseOriginal.getWidth(), baseOriginal.getHeight());

        float l = getPaddingLeft();
        float t = getPaddingTop();
        float r = getWidth() - getPaddingRight();
        float b = getHeight() - getPaddingBottom() - contentBottomInsetPx;
        if (b < t) b = t;
        dstRect.set(l, t, r, b);

        imageMatrix.reset();
        imageMatrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER);

        float sx = dstRect.width()  / srcRect.width();
        float sy = dstRect.height() / srcRect.height();
        baseScale = Math.min(sx, sy);

        float drawW = baseOriginal.getWidth()  * baseScale;
        float drawH = baseOriginal.getHeight() * baseScale;

        baseOffsetX = l + (dstRect.width()  - drawW) * 0.5f;
        baseOffsetY = t + (dstRect.height() - drawH) * 0.5f;
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

    private Layout.Alignment mapAlign(int a) {
        if (a == TextItem.ALIGN_CENTER) return Layout.Alignment.ALIGN_CENTER;
        if (a == TextItem.ALIGN_RIGHT)  return Layout.Alignment.ALIGN_OPPOSITE;
        return Layout.Alignment.ALIGN_NORMAL;
    }

    private StaticLayout buildLayout(@NonNull TextItem item, float widthPx) {
        TextPaint tp = new TextPaint(textPaint);
        tp.setTextSize(sp(item.textSizeSp));
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        tp.setColor(item.color);

        CharSequence cs = item.text == null ? "" : item.text;
        int w = (int) Math.max(1, widthPx);

        if (widthPx > 0f) {
            if (Build.VERSION.SDK_INT >= 23) {
                return StaticLayout.Builder.obtain(cs, 0, cs.length(), tp, w)
                        .setAlignment(mapAlign(item.align))
                        .setIncludePad(false)
                        .setLineSpacing(0, 1f)
                        .build();
            } else {
                return new StaticLayout(cs, tp, w, mapAlign(item.align), 1f, 0f, false);
            }
        } else {
            int singleWidth = (int) Math.ceil(tp.measureText(cs, 0, cs.length()));
            if (Build.VERSION.SDK_INT >= 23) {
                return StaticLayout.Builder.obtain(cs, 0, cs.length(), tp, Math.max(1, singleWidth))
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build();
            } else {
                return new StaticLayout(cs, tp, Math.max(1, singleWidth),
                        Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
            }
        }
    }

    private float measureSingleLineWidth(@NonNull TextItem item) {
        TextPaint tp = new TextPaint(textPaint);
        tp.setTextSize(sp(item.textSizeSp));
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        CharSequence cs = item.text == null ? "" : item.text;
        return tp.measureText(cs, 0, cs.length());
    }

    private int hitTestTextIndex(float touchX, float touchY) {
        for (int i = items.size() - 1; i >= 0; i--) {
            TextItem item = items.get(i);

            float widthPx = item.boxWidth > 0 ? item.boxWidth : measureSingleLineWidth(item);
            StaticLayout layout = buildLayout(item, widthPx);

            float drawX;
            if (item.boxWidth > 0) {
                drawX = item.x;
            } else {
                TextPaint tp = new TextPaint(textPaint);
                tp.setTextSize(sp(item.textSizeSp));
                tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
                float tw = tp.measureText(item.text == null ? "" : item.text);
                if (item.align == TextItem.ALIGN_CENTER)      drawX = item.x - tw / 2f;
                else if (item.align == TextItem.ALIGN_RIGHT)  drawX = item.x - tw;
                else                                          drawX = item.x;
            }

            TextPaint tpForBaseline = new TextPaint(textPaint);
            tpForBaseline.setTextSize(sp(item.textSizeSp));
            tpForBaseline.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            Paint.FontMetrics fm = tpForBaseline.getFontMetrics();
            float topY = item.y + fm.ascent;

            float boxW = (item.boxWidth > 0 ? item.boxWidth : layout.getWidth());
            float boxH = layout.getHeight();

            if (touchX >= drawX && touchX <= drawX + boxW &&
                    touchY >= topY && touchY <= topY + boxH) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInResizeHandle(int idx, float touchX, float touchY) {
        TextItem item = items.get(idx);

        float widthPx = item.boxWidth > 0 ? item.boxWidth : measureSingleLineWidth(item);
        StaticLayout layout = buildLayout(item, widthPx);

        float drawX;
        if (item.boxWidth > 0) {
            drawX = item.x;
        } else {
            TextPaint tp = new TextPaint(textPaint);
            tp.setTextSize(sp(item.textSizeSp));
            tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            float tw = tp.measureText(item.text == null ? "" : item.text);
            if (item.align == TextItem.ALIGN_CENTER)      drawX = item.x - tw / 2f;
            else if (item.align == TextItem.ALIGN_RIGHT)  drawX = item.x - tw;
            else                                          drawX = item.x;
        }

        TextPaint tpForBaseline = new TextPaint(textPaint);
        tpForBaseline.setTextSize(sp(item.textSizeSp));
        tpForBaseline.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        Paint.FontMetrics fm = tpForBaseline.getFontMetrics();
        float topY = item.y + fm.ascent;

        float boxW = (item.boxWidth > 0 ? item.boxWidth : layout.getWidth());
        float boxH = layout.getHeight();

        float hs = dp(HANDLE_SIZE_DP);
        float hx = drawX + boxW - hs;
        float hy = topY + boxH - hs;

        return touchX >= hx && touchX <= hx + hs && touchY >= hy && touchY <= hy + hs;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                getResources().getDisplayMetrics());
    }
}
