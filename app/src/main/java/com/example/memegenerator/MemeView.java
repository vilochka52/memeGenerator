package com.example.memegenerator;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;


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


public class MemeView extends View {
    private int selectedTextIndex = -1;
    private boolean resizingText = false;
    private float resizeStartX;
    private final float HANDLE_SIZE_DP = 16f;
    private final float BOX_STROKE_DP = 1.5f;

    @Nullable private Bitmap bg;
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<TextItem> items = Collections.emptyList();

    private final Rect bgSrc = new Rect();
    private final Rect bgDst = new Rect();

    private int draggingIndex = -1;
    private float dragDx = 0f, dragDy = 0f;
    private boolean moved = false;
    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private Layout.Alignment mapAlign(int a) {
        if (a == TextItem.ALIGN_CENTER) return Layout.Alignment.ALIGN_CENTER;
        if (a == TextItem.ALIGN_RIGHT)  return Layout.Alignment.ALIGN_OPPOSITE;
        return Layout.Alignment.ALIGN_NORMAL;
    }

    private StaticLayout buildLayout(TextItem item, float widthPx) {
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
                // Deprecated, но рабочий на старых API
                return new StaticLayout(cs, tp, w, mapAlign(item.align), 1f, 0f, false);
            }
        } else {
            // Ширина не задана → рисуем одной строкой: сделаем расчёт на ширину текста
            int singleWidth = (int) Math.ceil(tp.measureText(cs, 0, cs.length()));
            if (Build.VERSION.SDK_INT >= 23) {
                return StaticLayout.Builder.obtain(cs, 0, cs.length(), tp, Math.max(1, singleWidth))
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL) // управим руками по X
                        .setIncludePad(false)
                        .build();
            } else {
                return new StaticLayout(cs, tp, Math.max(1, singleWidth), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
            }
        }
    }

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
                int idx = hitTestTextIndex(e.getX(), e.getY());
                if (idx >= 0 && editListener != null) { editListener.onRequestEdit(idx, items.get(idx)); return true; }
                return false;
            }
            @Override public boolean onDown(MotionEvent e) { return true; }
        });
    }

    public void setOnTextEditRequestListener(OnTextEditRequestListener l) { this.editListener = l; }
    public void setOnTextMovedListener(OnTextMovedListener l) { this.movedListener = l; }

    private static class ImageItem {
        Bitmap bmp;
        float x, y;
        ImageItem(Bitmap b, float x, float y) { this.bmp = b; this.x = x; this.y = y; }
    }
    private final List<ImageItem> images = new ArrayList<>();
    private boolean draggingImage = false;

    public void addImageBitmap(@Nullable Bitmap bmp) {
        if (bmp == null) return;

        // Если View ещё не измерен — откладываем добавление до следующего кадра
        if (getWidth() <= 0) {
            post(() -> addImageBitmap(bmp));
            return;
        }

        int vw = getWidth();
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();

        if (bw <= 0 || bh <= 0) return;

        // Масштабируем по ширине View, сохраняем пропорции
        float scale = vw / (float) bw;
        int sw = vw;
        int sh = Math.max(1, Math.round(bh * scale));

        Bitmap scaled = Bitmap.createScaledBitmap(bmp, sw, sh, true);

        // Ставим по центру вертикали, по X от левого края
        float x = 0f;
        float y = (getHeight() > 0) ? Math.max(0f, (getHeight() - sh) * 0.5f) : 0f;

        images.add(new ImageItem(scaled, x, y));
        invalidate();
    }


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
        if (selectedTextIndex >= items.size()) selectedTextIndex = -1;
        if (draggingIndex >= items.size()) draggingIndex = -1;


        // 1) Фон (если был)
        if (bg != null) {
            if (bgDst.width() == 0 || bgDst.height() == 0) computeBgRects(getWidth(), getHeight());
            canvas.drawBitmap(bg, bgSrc, bgDst, null);
        }

        // 2) Фото-слои (добавленные изображения)
        for (ImageItem ii : images) {
            if (ii.bmp != null && !ii.bmp.isRecycled()) {
                canvas.drawBitmap(ii.bmp, ii.x, ii.y, null);
            }
        }

        // 3) Тексты (StaticLayout, с учётом выравнивания и базовой линии!)
        for (int i = 0; i < items.size(); i++) {
            TextItem item = items.get(i);

            float widthPx = item.boxWidth > 0 ? item.boxWidth : 0f;
            StaticLayout layout = buildLayout(item, widthPx);

            // Вычисляем X
            float drawX;
            if (widthPx <= 0f) {
                // Однострочный: смещаем по align вручную
                TextPaint tp = new TextPaint(textPaint);
                tp.setTextSize(sp(item.textSizeSp));
                tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
                float tw = tp.measureText(item.text == null ? "" : item.text);
                if (item.align == TextItem.ALIGN_CENTER)      drawX = item.x - tw / 2f;
                else if (item.align == TextItem.ALIGN_RIGHT)  drawX = item.x - tw;
                else                                          drawX = item.x;
            } else {
                // Многострочный: левый край — это item.x, выравнивание внутри layout
                drawX = item.x;
            }

            // Корректно ставим по вертикали: item.y — это БАЗОВАЯ ЛИНИЯ первой строки
            TextPaint tpForBaseline = new TextPaint(textPaint);
            tpForBaseline.setTextSize(sp(item.textSizeSp));
            tpForBaseline.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
            Paint.FontMetrics fm = tpForBaseline.getFontMetrics();
            float topY = item.y + fm.ascent; // ascent < 0, так что сдвигаем вверх

            // Рисуем текст
            canvas.save();
            canvas.translate(drawX, topY);
            layout.draw(canvas);
            canvas.restore();

            // Хитбокс и ручка — только для выбранного
            if (i == selectedTextIndex) {
                Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                boxPaint.setStyle(Paint.Style.STROKE);
                boxPaint.setStrokeWidth(dp(BOX_STROKE_DP));
                boxPaint.setColor(0x99FFFFFF);

                float boxW = (widthPx > 0f) ? widthPx : layout.getWidth();
                float boxH = layout.getHeight();

                // рамка
                canvas.drawRect(drawX, topY, drawX + boxW, topY + boxH, boxPaint);

                // ручка в правом нижнем углу
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


    private int findClosestImageIndex(float touchX, float touchY) {
        int idx = -1; float best = Float.MAX_VALUE;
        for (int i = 0; i < images.size(); i++) {
            ImageItem it = images.get(i);
            float cx = it.x + (it.bmp != null ? it.bmp.getWidth()/2f : 0f);
            float cy = it.y + (it.bmp != null ? it.bmp.getHeight()/2f : 0f);
            float dx = cx - touchX, dy = cy - touchY, d = dx*dx + dy*dy;
            if (d < best) { best = d; idx = i; }
        }
        return idx;
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
            case MotionEvent.ACTION_DOWN: {
                // Выбор текста
                int idx = hitTestTextIndex(x, y);
                if (idx >= 0) {
                    selectedTextIndex = idx;

                    // Проверим попадание в ручку ресайза
                    if (isInResizeHandle(idx, x, y)) {
                        resizingText = true;
                        draggingIndex = idx;
                        resizeStartX = x;
                        getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }

                    // Иначе — обычное перетаскивание текста
                    draggingIndex = idx;
                    draggingImage = false; // на всякий, мы картинки не двигаем
                    moved = false;

                    // Для dragDx/Dy нужно реальное смещение от текущей опорной точки (item.x, item.y)
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
                        // изменяем ширину блока
                        float width = it.boxWidth > 0 ? it.boxWidth : measureSingleLineWidth(it);
                        float delta = x - resizeStartX;
                        float newWidth = Math.max(dp(60), width + delta); // минимальная ширина ~60dp
                        items.set(draggingIndex, it.withBoxWidth(newWidth));
                        resizeStartX = x;
                        invalidate();
                        return true;
                    } else {
                        // перетаскиваем как раньше
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
                    if (!resizingText && moved && movedListener != null) {
                        movedListener.onTextMoved(draggingIndex, items.get(draggingIndex));
                    } else if (resizingText && movedListener != null) {
                        // можно уведомить VM, что размер блока изменился
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
    private boolean isInResizeHandle(int idx, float touchX, float touchY) {
        TextItem item = items.get(idx);

        float widthPx = item.boxWidth > 0 ? item.boxWidth : measureSingleLineWidth(item);
        StaticLayout layout = buildLayout(item, widthPx);

        // X как в onDraw
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

        // Y как в onDraw (по базовой линии)
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

        return (touchX >= hx && touchX <= hx + hs && touchY >= hy && touchY <= hy + hs);
    }


    private float measureSingleLineWidth(TextItem item) {
        TextPaint tp = new TextPaint(textPaint);
        tp.setTextSize(sp(item.textSizeSp));
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        CharSequence cs = item.text == null ? "" : item.text;
        return tp.measureText(cs, 0, cs.length());
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
    private int hitTestTextIndex(float touchX, float touchY) {
        // Идём с конца — последний (верхний) нарисованный ловит тап первым
        for (int i = items.size() - 1; i >= 0; i--) {
            TextItem item = items.get(i);

            float widthPx = item.boxWidth > 0 ? item.boxWidth : measureSingleLineWidth(item);
            StaticLayout layout = buildLayout(item, widthPx);

            // X как в onDraw
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

            // Y как в onDraw (item.y — базовая линия первой строки)
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

}
