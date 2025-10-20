package com.example.memegenerator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.os.Build;

import androidx.annotation.Nullable;

import java.io.InputStream;

/** Декодируем всегда в SOFTWARE (mutable), чтобы избежать HARDWARE bitmap и крэшей. */
public final class BitmapUtils {
    private BitmapUtils(){}

    @Nullable
    public static Bitmap decodeStreamSafe(InputStream in) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.Source src = ImageDecoder.createSource(in.readAllBytes());
                return ImageDecoder.decodeBitmap(src, (decoder, info, source) -> {
                    decoder.setMutableRequired(true);                  // mutable
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE); // SOFTWARE
                });
            } else {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inMutable = true; // mutable
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
