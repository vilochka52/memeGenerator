package com.example.memegenerator;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class TextItem implements Parcelable {
    @NonNull public final String text;
    public final float textSizeSp;
    public final float x;
    public final float y;
    public final int typefaceStyle;
    @ColorInt public final int color;

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle) {
        this(text, textSizeSp, x, y, typefaceStyle, Color.WHITE);
    }

    public TextItem(@NonNull String text,
                    float textSizeSp,
                    float x,
                    float y,
                    int typefaceStyle,
                    int color) {
        this.text = text;
        this.textSizeSp = textSizeSp;
        this.x = x;
        this.y = y;
        this.typefaceStyle = typefaceStyle;
        this.color = color;
    }

    protected TextItem(Parcel in) {
        text = in.readString();
        textSizeSp = in.readFloat();
        x = in.readFloat();
        y = in.readFloat();
        typefaceStyle = in.readInt();
        color = in.readInt();
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
        return new TextItem(text, textSizeSp, nx, ny, typefaceStyle, color);
    }

    @Override public int describeContents() { return 0; }
    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeFloat(textSizeSp);
        dest.writeFloat(x);
        dest.writeFloat(y);
        dest.writeInt(typefaceStyle);
        dest.writeInt(color);
    }
}
