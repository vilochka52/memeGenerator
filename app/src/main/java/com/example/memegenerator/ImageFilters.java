package com.example.memegenerator;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageFilters {

    @Nullable
    public static Bitmap apply(@Nullable Bitmap source, @NonNull FilterState state) {
        if (source == null || source.isRecycled()) return null;

        Bitmap out = Bitmap.createBitmap(
                source.getWidth(),
                source.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        ColorMatrix matrix = new ColorMatrix();

        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(state.saturation);
        matrix.postConcat(saturationMatrix);

        ColorMatrix contrastBrightnessMatrix = new ColorMatrix(new float[]{
                state.contrast, 0, 0, 0, state.brightness,
                0, state.contrast, 0, 0, state.brightness,
                0, 0, state.contrast, 0, state.brightness,
                0, 0, 0, 1, 0
        });
        matrix.postConcat(contrastBrightnessMatrix);

        if (state.warmth != 0f) {
            float w = state.warmth;
            float r = 1f + (w > 0 ? w / 150f : 0f);
            float b = 1f + (w < 0 ? -w / 150f : 0f);

            ColorMatrix warmthMatrix = new ColorMatrix(new float[]{
                    r, 0, 0, 0, 0,
                    0, 1f, 0, 0, 0,
                    0, 0, b, 0, 0,
                    0, 0, 0, 1f, 0
            });
            matrix.postConcat(warmthMatrix);
        }

        if (state.hue != 0f) {
            ColorMatrix hueMatrix = createHueRotationMatrix(state.hue);
            matrix.postConcat(hueMatrix);
        }

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(source, 0f, 0f, paint);

        return out;
    }

    @NonNull
    private static ColorMatrix createHueRotationMatrix(float degrees) {
        double rad = Math.toRadians(degrees);
        float cosVal = (float) Math.cos(rad);
        float sinVal = (float) Math.sin(rad);

        final float lumR = 0.213f;
        final float lumG = 0.715f;
        final float lumB = 0.072f;

        return new ColorMatrix(new float[]{
                lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
                lumG + cosVal * (-lumG) + sinVal * (-lumG),
                lumB + cosVal * (-lumB) + sinVal * (1 - lumB),
                0, 0,

                lumR + cosVal * (-lumR) + sinVal * 0.143f,
                lumG + cosVal * (1 - lumG) + sinVal * 0.140f,
                lumB + cosVal * (-lumB) + sinVal * -0.283f,
                0, 0,

                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
                lumG + cosVal * (-lumG) + sinVal * lumG,
                lumB + cosVal * (1 - lumB) + sinVal * lumB,
                0, 0,

                0, 0, 0, 1, 0
        });
    }
}