// TextItem.java
package com.example.memegenerator;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class TextItem implements Parcelable {
    public static final int ALIGN_LEFT   = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT  = 2;

    @NonNull public final String text;
    public final float textSizeSp;
    public final float x;
    public final float y;
    public final int typefaceStyle;
    @ColorInt public final int color;
    public final int align;

    // НОВОЕ: ширина текстового блока в px (для переносов)
    public final float boxWidth;

    // По умолчанию boxWidth = 0 → рисуем одной строкой (как сейчас)
    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle) {
        this(text, textSizeSp, x, y, typefaceStyle, Color.WHITE, ALIGN_LEFT, 0f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color) {
        this(text, textSizeSp, x, y, typefaceStyle, color, ALIGN_LEFT, 0f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color,
                    int align) {
        this(text, textSizeSp, x, y, typefaceStyle, color, align, 0f);
    }

    // Новый полный конструктор
    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color,
                    int align,
                    float boxWidth) {
        this.text = text;
        this.textSizeSp = textSizeSp;
        this.x = x;
        this.y = y;
        this.typefaceStyle = typefaceStyle;
        this.color = color;
        this.align = align;
        this.boxWidth = boxWidth;
    }

    protected TextItem(Parcel in) {
        text = in.readString();
        textSizeSp = in.readFloat();
        x = in.readFloat();
        y = in.readFloat();
        typefaceStyle = in.readInt();
        color = in.readInt();
        align = in.readInt();
        boxWidth = in.readFloat(); // НОВОЕ
    }

    public static final Creator<TextItem> CREATOR = new Creator<TextItem>() {
        @Override public TextItem createFromParcel(Parcel in) { return new TextItem(in); }
        @Override public TextItem[] newArray(int size) { return new TextItem[size]; }
    };

    public TextItem withPosition(float nx, float ny) {
        return new TextItem(text, textSizeSp, nx, ny, typefaceStyle, color, align, boxWidth);
    }

    public TextItem withAlign(int newAlign) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, newAlign, boxWidth);
    }

    public TextItem withBoxWidth(float newBoxWidth) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, align, Math.max(0f, newBoxWidth));
    }

    @Override public int describeContents() { return 0; }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeFloat(textSizeSp);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeInt(typefaceStyle);
        dest.writeInt(color);
        dest.writeInt(align);
        dest.writeFloat(boxWidth); // НОВОЕ
    }
}
