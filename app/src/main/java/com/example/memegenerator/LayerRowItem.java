package com.example.memegenerator;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

public class LayerRowItem {

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_DRAW = 2;

    public final int type;
    public final int textIndex;
    public final String title;
    public final boolean visible;
    public final float alpha;

    @Nullable
    public final Bitmap thumbnail;

    public boolean isDrawLayer() {
        return type == TYPE_DRAW;
    }

    public LayerRowItem(int type,
                        int textIndex,
                        String title,
                        boolean visible,
                        float alpha,
                        @Nullable Bitmap thumbnail) {
        this.type = type;
        this.textIndex = textIndex;
        this.title = title;
        this.visible = visible;
        this.alpha = alpha;
        this.thumbnail = thumbnail;
    }

    public boolean isImageLayer() {
        return type == TYPE_IMAGE;
    }

    public boolean isTextLayer() {
        return type == TYPE_TEXT;
    }
}