package com.example.memegenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
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
    private final Matrix inverseImageMatrix = new Matrix();
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

    private boolean baseImageVisible = true;
    private float baseImageAlpha = 1f;

    @Nullable
    private TextItem dragStartItem = null;

    private final GestureDetector gestureDetector;

    private boolean cropMode = false;
    private final RectF cropRect = new RectF();
    private final Paint cropOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cropBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cropHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int CROP_NONE = 0;
    private static final int CROP_MOVE = 1;
    private static final int CROP_TOP_LEFT = 2;
    private static final int CROP_TOP_RIGHT = 3;
    private static final int CROP_BOTTOM_LEFT = 4;
    private static final int CROP_BOTTOM_RIGHT = 5;

    private int cropTouchMode = CROP_NONE;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;

    @Nullable
    private Bitmap drawBitmap = null;
    @Nullable
    private Canvas drawCanvas = null;

    private boolean drawLayerVisible = true;
    private float drawLayerAlpha = 1f;

    private boolean drawMode = false;
    private boolean eraserMode = false;

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private float lastDrawX = 0f;
    private float lastDrawY = 0f;

    private int currentBrushColor = Color.WHITE;
    private float currentBrushSizePx = 18f;

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

        cropOverlayPaint.setColor(0xAA000000);

        cropBorderPaint.setStyle(Paint.Style.STROKE);
        cropBorderPaint.setStrokeWidth(dp(2));
        cropBorderPaint.setColor(Color.WHITE);

        cropHandlePaint.setStyle(Paint.Style.FILL);
        cropHandlePaint.setColor(Color.WHITE);
    }

    private GestureDetector createGestureDetector(Context ctx) {
        return new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (cropMode || drawMode) return false;

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

        if (selectedTextIndex >= items.size()) selectedTextIndex = -1;
        if (draggingIndex >= items.size()) draggingIndex = -1;

        invalidate();
    }

    public void addImageBitmap(@Nullable Bitmap bmp) {
        if (bmp == null) return;
        baseOriginal = bmp;
        drawBitmap = null;
        drawCanvas = null;
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

    public boolean isBaseImageVisible() {
        return baseImageVisible;
    }

    public void setBaseImageVisible(boolean visible) {
        this.baseImageVisible = visible;
        invalidate();
    }

    public float getBaseImageAlpha() {
        return baseImageAlpha;
    }

    public void setBaseImageAlpha(float alpha) {
        this.baseImageAlpha = clamp01(alpha);
        invalidate();
    }

    @Nullable
    public Bitmap getBaseThumbnail(int sizePx) {
        if (baseOriginal == null || baseOriginal.isRecycled()) return null;
        return Bitmap.createScaledBitmap(baseOriginal, sizePx, sizePx, true);
    }

    public void clearBaseImage() {
        baseOriginal = null;
        drawBitmap = null;
        drawCanvas = null;
        selectedTextIndex = -1;
        draggingIndex = -1;
        dragStartItem = null;
        cropMode = false;
        cropTouchMode = CROP_NONE;
        drawMode = false;
        invalidate();
    }

    public void startCropMode() {
        if (!hasImage()) return;

        cropMode = true;
        cropTouchMode = CROP_NONE;
        selectedTextIndex = -1;
        draggingIndex = -1;
        dragStartItem = null;

        float insetX = dstRect.width() * 0.1f;
        float insetY = dstRect.height() * 0.1f;

        cropRect.set(
                dstRect.left + insetX,
                dstRect.top + insetY,
                dstRect.right - insetX,
                dstRect.bottom - insetY
        );

        invalidate();
    }

    public void cancelCropMode() {
        cropMode = false;
        cropTouchMode = CROP_NONE;
        invalidate();
    }

    public boolean isCropMode() {
        return cropMode;
    }

    public boolean applyCropAndReplaceBase() {
        if (!cropMode || !hasImage()) return false;

        RectF bitmapCrop = mapViewRectToBitmap(cropRect);
        if (bitmapCrop.width() < 2 || bitmapCrop.height() < 2) return false;

        int left = Math.max(0, Math.round(bitmapCrop.left));
        int top = Math.max(0, Math.round(bitmapCrop.top));
        int right = Math.min(baseOriginal.getWidth(), Math.round(bitmapCrop.right));
        int bottom = Math.min(baseOriginal.getHeight(), Math.round(bitmapCrop.bottom));

        int width = right - left;
        int height = bottom - top;

        if (width <= 1 || height <= 1) return false;

        Bitmap croppedBase = Bitmap.createBitmap(baseOriginal, left, top, width, height);
        baseOriginal = croppedBase;

        if (drawBitmap != null && !drawBitmap.isRecycled()) {
            Bitmap newDraw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(newDraw);

            Rect src = new Rect(left, top, right, bottom);
            Rect dst = new Rect(0, 0, width, height);
            c.drawBitmap(drawBitmap, src, dst, null);

            drawBitmap = newDraw;
            drawCanvas = new Canvas(drawBitmap);
        }

        cropMode = false;
        cropTouchMode = CROP_NONE;

        recomputeImageMatrix();
        invalidate();
        return true;
    }

    public boolean hasDrawLayer() {
        return drawBitmap != null && !drawBitmap.isRecycled();
    }

    public boolean isDrawLayerVisible() {
        return drawLayerVisible;
    }

    public void setDrawLayerVisible(boolean visible) {
        drawLayerVisible = visible;
        invalidate();
    }

    public float getDrawLayerAlpha() {
        return drawLayerAlpha;
    }

    public void setDrawLayerAlpha(float alpha) {
        drawLayerAlpha = clamp01(alpha);
        invalidate();
    }

    public void setDrawMode(boolean enabled) {
        drawMode = enabled;
        if (enabled) {
            cropMode = false;
        }
        invalidate();
    }

    public boolean isDrawMode() {
        return drawMode;
    }

    public void setEraserMode(boolean enabled) {
        eraserMode = enabled;
    }

    public boolean isEraserMode() {
        return eraserMode;
    }

    public void setBrushColor(int color) {
        currentBrushColor = color;
    }

    public int getBrushColor() {
        return currentBrushColor;
    }

    public void setBrushSizeDp(float sizeDp) {
        currentBrushSizePx = dp(sizeDp);
    }

    public float getBrushSizeDp() {
        return currentBrushSizePx / getResources().getDisplayMetrics().density;
    }

    public void clearDrawLayer() {
        if (drawBitmap == null || drawCanvas == null) return;
        drawBitmap.eraseColor(Color.TRANSPARENT);
        invalidate();
    }

    @Nullable
    public Bitmap getDrawBitmapCopy() {
        if (drawBitmap == null || drawBitmap.isRecycled()) return null;
        return drawBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    public void replaceDrawBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            drawBitmap = null;
            drawCanvas = null;
            invalidate();
            return;
        }

        drawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        drawCanvas = new Canvas(drawBitmap);
        invalidate();
    }

    private void ensureDrawLayer() {
        if (!hasImage()) return;

        if (drawBitmap == null || drawBitmap.isRecycled()
                || drawBitmap.getWidth() != baseOriginal.getWidth()
                || drawBitmap.getHeight() != baseOriginal.getHeight()) {

            drawBitmap = Bitmap.createBitmap(
                    baseOriginal.getWidth(),
                    baseOriginal.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            drawCanvas = new Canvas(drawBitmap);
        }
    }

    public Bitmap exportBaseImageAtOriginal() {
        if (baseOriginal == null || baseOriginal.isRecycled() || !baseImageVisible) {
            return null;
        }

        Bitmap out = Bitmap.createBitmap(
                baseOriginal.getWidth(),
                baseOriginal.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setAlpha(Math.round(255f * baseImageAlpha));
        canvas.drawBitmap(baseOriginal, 0f, 0f, p);
        return out;
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

        if (baseImageVisible) {
            Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            basePaint.setAlpha(Math.round(255f * baseImageAlpha));
            canvas.drawBitmap(baseOriginal, 0f, 0f, basePaint);
        }

        if (hasDrawLayer() && drawLayerVisible) {
            Paint drawExportPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            drawExportPaint.setAlpha(Math.round(255f * drawLayerAlpha));
            canvas.drawBitmap(drawBitmap, 0f, 0f, drawExportPaint);
        }

        float scaleX = dstRect.width() / srcRect.width();
        float scaleY = dstRect.height() / srcRect.height();
        float drawScale = Math.min(scaleX, scaleY);

        float drawnWidth = srcRect.width() * drawScale;
        float drawnHeight = srcRect.height() * drawScale;

        float offsetX = dstRect.left + (dstRect.width() - drawnWidth) / 2f;
        float offsetY = dstRect.top + (dstRect.height() - drawnHeight) / 2f;

        for (TextItem item : items) {
            if (!item.visible) continue;

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

        if (hasImage() && baseImageVisible) {
            imgPaint.setAlpha(Math.round(255f * baseImageAlpha));
            canvas.drawBitmap(baseOriginal, imageMatrix, imgPaint);
        }

        if (hasDrawLayer() && drawLayerVisible) {
            bitmapPaint.setAlpha(Math.round(255f * drawLayerAlpha));
            canvas.drawBitmap(drawBitmap, imageMatrix, bitmapPaint);
        }

        for (int i = 0; i < items.size(); i++) {
            TextItem item = items.get(i);
            if (!item.visible) continue;
            drawTextItem(canvas, item, i == selectedTextIndex && !cropMode && !drawMode);
        }

        if (cropMode) {
            drawCropOverlay(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cropMode) {
            return handleCropTouch(event);
        }

        if (drawMode) {
            return handleDrawTouch(event);
        }

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

                    if (!item.visible) {
                        draggingIndex = -1;
                        dragStartItem = null;
                        invalidate();
                        return true;
                    }

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

    private boolean handleDrawTouch(MotionEvent event) {
        if (!hasImage()) return false;

        ensureDrawLayer();

        float[] pts = new float[]{event.getX(), event.getY()};
        imageMatrix.invert(inverseImageMatrix);
        inverseImageMatrix.mapPoints(pts);

        float x = clamp(pts[0], 0, baseOriginal.getWidth());
        float y = clamp(pts[1], 0, baseOriginal.getHeight());

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(currentBrushSizePx / getImageScaleForExportAware());
        p.setColor(eraserMode ? Color.TRANSPARENT : currentBrushColor);

        if (eraserMode) {
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastDrawX = x;
                lastDrawY = y;
                if (drawCanvas != null) {
                    drawCanvas.drawPoint(x, y, p);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (drawCanvas != null) {
                    drawCanvas.drawLine(lastDrawX, lastDrawY, x, y, p);
                }
                lastDrawX = x;
                lastDrawY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (drawCanvas != null) {
                    drawCanvas.drawLine(lastDrawX, lastDrawY, x, y, p);
                }
                invalidate();
                return true;
        }

        return false;
    }

    private float getImageScaleForExportAware() {
        if (srcRect.width() <= 0f || dstRect.width() <= 0f) return 1f;

        float scaleX = dstRect.width() / srcRect.width();
        float scaleY = dstRect.height() / srcRect.height();
        return Math.max(0.0001f, Math.min(scaleX, scaleY));
    }

    private boolean handleCropTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cropTouchMode = detectCropTouchMode(x, y);
                lastTouchX = x;
                lastTouchY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                updateCropRect(dx, dy);

                lastTouchX = x;
                lastTouchY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cropTouchMode = CROP_NONE;
                return true;
        }

        return false;
    }

    private int detectCropTouchMode(float x, float y) {
        float r = dp(22);

        if (distance(x, y, cropRect.left, cropRect.top) <= r) return CROP_TOP_LEFT;
        if (distance(x, y, cropRect.right, cropRect.top) <= r) return CROP_TOP_RIGHT;
        if (distance(x, y, cropRect.left, cropRect.bottom) <= r) return CROP_BOTTOM_LEFT;
        if (distance(x, y, cropRect.right, cropRect.bottom) <= r) return CROP_BOTTOM_RIGHT;
        if (cropRect.contains(x, y)) return CROP_MOVE;
        return CROP_NONE;
    }

    private void updateCropRect(float dx, float dy) {
        if (cropTouchMode == CROP_NONE) return;

        RectF bounds = new RectF(dstRect);
        float minSize = dp(72);

        switch (cropTouchMode) {
            case CROP_MOVE:
                cropRect.offset(dx, dy);

                if (cropRect.left < bounds.left) cropRect.offset(bounds.left - cropRect.left, 0);
                if (cropRect.top < bounds.top) cropRect.offset(0, bounds.top - cropRect.top);
                if (cropRect.right > bounds.right) cropRect.offset(bounds.right - cropRect.right, 0);
                if (cropRect.bottom > bounds.bottom) cropRect.offset(0, bounds.bottom - cropRect.bottom);
                break;

            case CROP_TOP_LEFT:
                cropRect.left += dx;
                cropRect.top += dy;
                break;

            case CROP_TOP_RIGHT:
                cropRect.right += dx;
                cropRect.top += dy;
                break;

            case CROP_BOTTOM_LEFT:
                cropRect.left += dx;
                cropRect.bottom += dy;
                break;

            case CROP_BOTTOM_RIGHT:
                cropRect.right += dx;
                cropRect.bottom += dy;
                break;
        }

        if (cropRect.width() < minSize) {
            if (cropTouchMode == CROP_TOP_LEFT || cropTouchMode == CROP_BOTTOM_LEFT) {
                cropRect.left = cropRect.right - minSize;
            } else if (cropTouchMode == CROP_TOP_RIGHT || cropTouchMode == CROP_BOTTOM_RIGHT) {
                cropRect.right = cropRect.left + minSize;
            }
        }

        if (cropRect.height() < minSize) {
            if (cropTouchMode == CROP_TOP_LEFT || cropTouchMode == CROP_TOP_RIGHT) {
                cropRect.top = cropRect.bottom - minSize;
            } else if (cropTouchMode == CROP_BOTTOM_LEFT || cropTouchMode == CROP_BOTTOM_RIGHT) {
                cropRect.bottom = cropRect.top + minSize;
            }
        }

        cropRect.left = Math.max(bounds.left, cropRect.left);
        cropRect.top = Math.max(bounds.top, cropRect.top);
        cropRect.right = Math.min(bounds.right, cropRect.right);
        cropRect.bottom = Math.min(bounds.bottom, cropRect.bottom);
    }

    private void drawCropOverlay(Canvas canvas) {
        Path path = new Path();
        path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        path.addRect(cropRect, Path.Direction.CCW);
        canvas.drawPath(path, cropOverlayPaint);

        canvas.drawRoundRect(cropRect, dp(12), dp(12), cropBorderPaint);

        float handleR = dp(7);
        canvas.drawCircle(cropRect.left, cropRect.top, handleR, cropHandlePaint);
        canvas.drawCircle(cropRect.right, cropRect.top, handleR, cropHandlePaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleR, cropHandlePaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleR, cropHandlePaint);
    }

    private RectF mapViewRectToBitmap(RectF viewRect) {
        float[] pts = new float[]{
                viewRect.left, viewRect.top,
                viewRect.right, viewRect.bottom
        };

        imageMatrix.invert(inverseImageMatrix);
        inverseImageMatrix.mapPoints(pts);

        RectF out = new RectF(
                pts[0],
                pts[1],
                pts[2],
                pts[3]
        );

        out.left = clamp(out.left, 0, baseOriginal.getWidth());
        out.top = clamp(out.top, 0, baseOriginal.getHeight());
        out.right = clamp(out.right, 0, baseOriginal.getWidth());
        out.bottom = clamp(out.bottom, 0, baseOriginal.getHeight());

        return out;
    }

    private void drawTextItem(Canvas canvas, TextItem item, boolean selected) {
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
        tp.setTextSize(sp(item.textSizeSp));
        tp.setTypeface(Typeface.create(Typeface.DEFAULT, item.typefaceStyle));
        tp.setColor(item.color);

        int originalAlpha = Color.alpha(item.color);
        int finalAlpha = Math.round(originalAlpha * clamp01(item.alpha));
        tp.setAlpha(finalAlpha);

        return tp;
    }

    private int hitTestTextIndex(float touchX, float touchY) {
        for (int i = items.size() - 1; i >= 0; i--) {
            TextItem item = items.get(i);
            if (!item.visible) continue;

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

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
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

    @Nullable
    public Bitmap getBaseBitmapCopy() {
        if (baseOriginal == null || baseOriginal.isRecycled()) {
            return null;
        }
        return baseOriginal.copy(Bitmap.Config.ARGB_8888, true);
    }

    public void replaceBaseBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) return;
        baseOriginal = bitmap;
        cropMode = false;
        cropTouchMode = CROP_NONE;
        recomputeImageMatrix();
        invalidate();
    }
}