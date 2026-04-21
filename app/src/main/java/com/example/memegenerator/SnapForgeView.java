package com.example.memegenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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

public class SnapForgeView extends View {

    @Nullable
    private Bitmap baseOriginal = null;

    private final Matrix imageMatrix = new Matrix();
    private final RectF srcRect = new RectF();
    private final RectF dstRect = new RectF();
    private final Paint imgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TextItem> items = Collections.emptyList();

    private int contentBottomInsetPx = 0;

    private int selectedTextIndex = -1;
    private int draggingIndex = -1;
    private float dragDx = 0f;
    private float dragDy = 0f;

    @Nullable
    private TextItem dragStartItem = null;

    private final GestureDetector gestureDetector;

    public interface OnTextEditRequestListener {
        void onRequestEdit(int index, TextItem item);
    }

    public interface OnTextMoveFinishedListener {
        void onMoveFinished(int index, TextItem oldItem, TextItem newItem);
    }

    private OnTextEditRequestListener editListener;
    private OnTextMoveFinishedListener moveFinishedListener;

    public SnapForgeView(Context context) {
        super(context);
        gestureDetector = createGestureDetector(context);
        init();
    }

    public SnapForgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = createGestureDetector(context);
        init();
    }

    public SnapForgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureDetector = createGestureDetector(context);
        init();
    }

    private void init() {
        textPaint.setSubpixelText(true);
        setWillNotDraw(false);
    }

    private GestureDetector createGestureDetector(Context ctx) {
        return new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int idx = hitTestTextIndex(e.getX(), e.getY());
                if (idx >= 0 && editListener != null) {
                    editListener.onRequestEdit(idx, items.get(idx));
                    return true;
                }
                return false;
            }
        });
    }

    public void setOnTextEditRequestListener(OnTextEditRequestListener listener) {
        this.editListener = listener;
    }

    public void setOnTextMoveFinishedListener(OnTextMoveFinishedListener listener) {
        this.moveFinishedListener = listener;
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
        contentBottomInsetPx = Math.max(0, px);
        recomputeImageMatrix();
        invalidate();
    }

    public Bitmap exportToBitmapAtOriginal() {
        if (!hasImage()) {
            Bitmap empty = Bitmap.createBitmap(
                    Math.max(1, getWidth()),
                    Math.max(1, getHeight()),
                    Bitmap.Config.ARGB_8888
            );
            Canvas c = new Canvas(empty);
            draw(c);
            return empty;
        }

        Bitmap out = Bitmap.createBitmap(
                baseOriginal.getWidth(),
                baseOriginal.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(out);
        canvas.drawBitmap(baseOriginal, 0f, 0f, null);

        float scaleX = dstRect.width() / srcRect.width();
        float scaleY = dstRect.height() / srcRect.height();
        float drawScale = Math.min(scaleX, scaleY);

        float drawnWidth = srcRect.width() * drawScale;
        float drawnHeight = srcRect.height() * drawScale;

        float offsetX = dstRect.left + (dstRect.width() - drawnWidth) / 2f;
        float offsetY = dstRect.top + (dstRect.height() - drawnHeight) / 2f;

        for (TextItem item : items) {
            TextPaint tp = buildTextPaint(item);
            float exportSize = item.textSizeSp / drawScale;
            tp.setTextSize(sp(exportSize));

            String text = item.text == null ? "" : item.text;

            float exportX = (item.x - offsetX) / drawScale;
            float exportY = (item.y - offsetY) / drawScale;

            float textWidth = tp.measureText(text);

            float drawX;
            if (item.align == TextItem.ALIGN_CENTER) {
                drawX = exportX - textWidth / 2f;
            } else if (item.align == TextItem.ALIGN_RIGHT) {
                drawX = exportX - textWidth;
            } else {
                drawX = exportX;
            }

            canvas.drawText(text, drawX, exportY, tp);
        }

        return out;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeImageMatrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (hasImage()) {
            canvas.drawBitmap(baseOriginal, imageMatrix, imgPaint);
        }

        for (int i = 0; i < items.size(); i++) {
            TextItem item = items.get(i);
            drawTextItem(canvas, item, i == selectedTextIndex);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int idx = hitTestTextIndex(x, y);
                selectedTextIndex = idx;

                if (idx >= 0) {
                    draggingIndex = idx;
                    TextItem item = items.get(idx);
                    dragDx = x - item.x;
                    dragDy = y - item.y;
                    dragStartItem = item;
                    invalidate();
                    return true;
                }

                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (draggingIndex >= 0 && draggingIndex < items.size()) {
                    TextItem item = items.get(draggingIndex);
                    TextItem moved = item.withPosition(x - dragDx, y - dragDy);

                    List<TextItem> list = new ArrayList<>(items);
                    list.set(draggingIndex, moved);
                    items = list;
                    invalidate();
                    return true;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (draggingIndex >= 0 && draggingIndex < items.size() && dragStartItem != null) {
                    TextItem newItem = items.get(draggingIndex);
                    if (moveFinishedListener != null &&
                            (dragStartItem.x != newItem.x || dragStartItem.y != newItem.y)) {
                        moveFinishedListener.onMoveFinished(draggingIndex, dragStartItem, newItem);
                    }
                }

                draggingIndex = -1;
                dragStartItem = null;
                return true;
            }
        }

        return true;
    }

    private void drawTextItem(Canvas canvas, TextItem item, boolean selected) {
        TextPaint tp = buildTextPaint(item);
        String text = item.text == null ? "" : item.text;

        float textWidth = tp.measureText(text);
        Paint.FontMetrics fm = tp.getFontMetrics();
        float textHeight = fm.bottom - fm.top;

        float drawX;
        if (item.align == TextItem.ALIGN_CENTER) {
            drawX = item.x - textWidth / 2f;
        } else if (item.align == TextItem.ALIGN_RIGHT) {
            drawX = item.x - textWidth;
        } else {
            drawX = item.x;
        }

        canvas.drawText(text, drawX, item.y, tp);

        if (selected) {
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(1.5f));
            border.setColor(0x99FFFFFF);

            float left = drawX - dp(8);
            float top = item.y + fm.top - dp(6);
            float right = drawX + textWidth + dp(8);
            float bottom = item.y + fm.bottom + dp(6);

            canvas.drawRoundRect(left, top, right, bottom, dp(8), dp(8), border);
        }
    }

    private TextPaint buildTextPaint(TextItem item) {
        TextPaint tp = new TextPaint(textPaint);
        tp.setColor(item.color);
        tp.setTextSize(sp(item.textSizeSp));
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        return tp;
    }

    private int hitTestTextIndex(float touchX, float touchY) {
        for (int i = items.size() - 1; i >= 0; i--) {
            TextItem item = items.get(i);
            TextPaint tp = buildTextPaint(item);
            String text = item.text == null ? "" : item.text;

            float textWidth = tp.measureText(text);
            Paint.FontMetrics fm = tp.getFontMetrics();

            float drawX;
            if (item.align == TextItem.ALIGN_CENTER) {
                drawX = item.x - textWidth / 2f;
            } else if (item.align == TextItem.ALIGN_RIGHT) {
                drawX = item.x - textWidth;
            } else {
                drawX = item.x;
            }

            float left = drawX - dp(16);
            float top = item.y + fm.top - dp(16);
            float right = drawX + textWidth + dp(16);
            float bottom = item.y + fm.bottom + dp(16);

            if (touchX >= left && touchX <= right && touchY >= top && touchY <= bottom) {
                return i;
            }
        }
        return -1;
    }

    private void recomputeImageMatrix() {
        if (baseOriginal == null || getWidth() == 0 || getHeight() == 0) return;

        srcRect.set(0, 0, baseOriginal.getWidth(), baseOriginal.getHeight());

        float left = getPaddingLeft();
        float top = getPaddingTop();
        float right = getWidth() - getPaddingRight();
        float bottom = getHeight() - getPaddingBottom() - contentBottomInsetPx;

        dstRect.set(left, top, right, bottom);

        imageMatrix.reset();
        imageMatrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    public Bitmap exportBaseImageAtOriginal() {
        if (baseOriginal == null || baseOriginal.isRecycled()) {
            return null;
        }
        return baseOriginal.copy(Bitmap.Config.ARGB_8888, false);
    }
}