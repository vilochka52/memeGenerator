package com.example.memegenerator;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ImageFilterUtils {

    @Nullable
    public static Bitmap applyFilters(@Nullable Bitmap source, @NonNull FilterState state) {
        if (source == null) return null;

        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        int width = result.getWidth();
        int height = result.getHeight();

        int[] pixels = new int[width * height];
        result.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];

            int a = Color.alpha(c);
            float r = Color.red(c);
            float g = Color.green(c);
            float b = Color.blue(c);

            r += state.brightness;
            g += state.brightness;
            b += state.brightness;

            r = ((r - 128f) * state.contrast) + 128f;
            g = ((g - 128f) * state.contrast) + 128f;
            b = ((b - 128f) * state.contrast) + 128f;

            float gray = (r + g + b) / 3f;
            r = gray + (r - gray) * state.saturation;
            g = gray + (g - gray) * state.saturation;
            b = gray + (b - gray) * state.saturation;

            r += state.warmth;
            b -= state.warmth;

            float[] hsv = new float[3];
            Color.RGBToHSV(clampToInt(r), clampToInt(g), clampToInt(b), hsv);
            hsv[0] = (hsv[0] + state.hue) % 360f;
            if (hsv[0] < 0) hsv[0] += 360f;

            pixels[i] = Color.HSVToColor(a, hsv);
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private static int clampToInt(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}