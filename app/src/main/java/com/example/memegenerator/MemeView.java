package com.example.memegenerator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class MemeView extends AppCompatImageView {

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<TextBlock> textBlocks = new ArrayList<>();
    private TextBlock activeBlock = null;
    private float dX, dY;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private int[] fonts = {R.font.impact, R.font.roboto_bold};

    public MemeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(64f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    for (TextBlock block : textBlocks) {
                        if (block.contains(event.getX(), event.getY(), textPaint)) {
                            activeBlock = block;
                            dX = event.getX() - block.x;
                            dY = event.getY() - block.y;
                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (activeBlock != null && !scaleDetector.isInProgress()) {
                        activeBlock.x = event.getX() - dX;
                        activeBlock.y = event.getY() - dY;
                        invalidate();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    activeBlock = null;
                    break;
            }
            return true;
        });
    }

    public void setImageUri(Uri uri) {
        setImageURI(uri);
    }

    public void addTextBlock(String text) {
        textBlocks.add(new TextBlock(text, getWidth() / 2f, getHeight() / 2f, textPaint.getTextSize(), textPaint.getTypeface()));
        invalidate();
    }

    public void setFontByIndex(int index) {
        if (activeBlock != null) {
            Typeface typeface = ResourcesCompat.getFont(getContext(), fonts[index]);
            activeBlock.typeface = typeface;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (TextBlock block : textBlocks) {
            textPaint.setTextSize(block.size);
            textPaint.setTypeface(block.typeface);
            canvas.drawText(block.text, block.x, block.y, textPaint);

            if (block == activeBlock) {
                RectF rect = block.getBounds(textPaint);
                Paint border = new Paint();
                border.setColor(Color.WHITE);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3f);
                canvas.drawRect(rect, border);
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (activeBlock != null) {
                activeBlock.size *= detector.getScaleFactor();
                invalidate();
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override public boolean onDown(MotionEvent e) { return true; }
        @Override public boolean onDoubleTap(MotionEvent e) {
            for (TextBlock block : textBlocks) {
                if (block.contains(e.getX(), e.getY(), textPaint)) {
                    activeBlock = block;
                    invalidate();
                    break;
                }
            }
            return true;
        }
    }

    private static class TextBlock {
        String text;
        float x, y, size;
        Typeface typeface;

        TextBlock(String text, float x, float y, float size, Typeface typeface) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.size = size;
            this.typeface = typeface;
        }

        RectF getBounds(Paint paint) {
            float w = paint.measureText(text);
            float h = paint.descent() - paint.ascent();
            return new RectF(x - w / 2 - 20, y + paint.ascent() - 20, x + w / 2 + 20, y + paint.descent() + 20);
        }

        boolean contains(float tx, float ty, Paint paint) {
            return getBounds(paint).contains(tx, ty);
        }
    }
}
