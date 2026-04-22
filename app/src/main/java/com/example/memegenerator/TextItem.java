package com.example.memegenerator;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class TextItem implements Parcelable {
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    @NonNull
    public final String text;
    public final float textSizeSp;
    public final float x;
    public final float y;
    public final int typefaceStyle;

    @ColorInt
    public final int color;

    public final int align;
    public final float boxWidth;

    public final boolean visible;
    public final float alpha;

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle) {
        this(text, textSizeSp, x, y, typefaceStyle, Color.WHITE, ALIGN_CENTER, 0f, true, 1f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color) {
        this(text, textSizeSp, x, y, typefaceStyle, color, ALIGN_CENTER, 0f, true, 1f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color,
                    int align) {
        this(text, textSizeSp, x, y, typefaceStyle, color, align, 0f, true, 1f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color,
                    int align,
                    float boxWidth) {
        this(text, textSizeSp, x, y, typefaceStyle, color, align, boxWidth, true, 1f);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color,
                    int align,
                    float boxWidth,
                    boolean visible,
                    float alpha) {
        this.text = text;
        this.textSizeSp = textSizeSp;
        this.x = x;
        this.y = y;
        this.typefaceStyle = typefaceStyle;
        this.color = color;
        this.align = align;
        this.boxWidth = Math.max(0f, boxWidth);
        this.visible = visible;
        this.alpha = Math.max(0f, Math.min(1f, alpha));
    }

    protected TextItem(Parcel in) {
        text = in.readString();
        textSizeSp = in.readFloat();
        x = in.readFloat();
        y = in.readFloat();
        typefaceStyle = in.readInt();
        color = in.readInt();
        align = in.readInt();
        boxWidth = in.readFloat();
        visible = in.readByte() != 0;
        alpha = in.readFloat();
    }

    public static final Creator<TextItem> CREATOR = new Creator<TextItem>() {
        @Override
        public TextItem createFromParcel(Parcel in) {
            return new TextItem(in);
        }

        @Override
        public TextItem[] newArray(int size) {
            return new TextItem[size];
        }
    };

    public TextItem withPosition(float nx, float ny) {
        return new TextItem(text, textSizeSp, nx, ny, typefaceStyle, color, align, boxWidth, visible, alpha);
    }

    public TextItem withText(@NonNull String newText) {
        return new TextItem(newText, textSizeSp, x, y, typefaceStyle, color, align, boxWidth, visible, alpha);
    }

    public TextItem withSize(float newSize) {
        return new TextItem(text, newSize, x, y, typefaceStyle, color, align, boxWidth, visible, alpha);
    }

    public TextItem withColor(@ColorInt int newColor) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, newColor, align, boxWidth, visible, alpha);
    }

    public TextItem withAlign(int newAlign) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, newAlign, boxWidth, visible, alpha);
    }

    public TextItem withBoxWidth(float newBoxWidth) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, align, Math.max(0f, newBoxWidth), visible, alpha);
    }

    public TextItem withVisible(boolean newVisible) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, align, boxWidth, newVisible, alpha);
    }

    public TextItem withAlpha(float newAlpha) {
        return new TextItem(text, textSizeSp, x, y, typefaceStyle, color, align, boxWidth, visible, Math.max(0f, Math.min(1f, newAlpha)));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeFloat(textSizeSp);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeInt(typefaceStyle);
        dest.writeInt(color);
        dest.writeInt(align);
        dest.writeFloat(boxWidth);
        dest.writeByte((byte) (visible ? 1 : 0));
        dest.writeFloat(alpha);
    }
}