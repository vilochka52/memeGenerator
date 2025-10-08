package com.example.memegenerator;

import android.graphics.Paint;
import android.graphics.RectF;

public class TextItem {
    public String text;
    public float x;
    public float y;
    public float textSize;
    public String fontName;
    public Paint.Align align;
    public boolean isSelected;

    public TextItem(String text, float x, float y, float textSize, String fontName, Paint.Align align) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.textSize = textSize;
        this.fontName = fontName;
        this.align = align;
        this.isSelected = false;
    }

    public RectF getBounds(Paint p) {
        float width = p.measureText(text);
        Paint.FontMetrics fm = p.getFontMetrics();
        float top = y + fm.ascent;
        float bottom = y + fm.descent;
        float left;
        if (align == Paint.Align.LEFT) left = x;
        else if (align == Paint.Align.CENTER) left = x - width / 2f;
        else left = x - width;
        return new RectF(left, top, left + width, bottom);
    }

    public boolean contains(float tx, float ty, Paint p) {
        return getBounds(p).contains(tx, ty);
    }
}
