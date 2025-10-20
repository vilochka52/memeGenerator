package com.example.memegenerator;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.content.res.AssetFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {
    private ImageLoader(){}

    /**
     * Загружает bitmap из Uri:
     * - масштабирует под targetW/targetH,
     * - делает SOFTWARE + mutable,
     * - учитывает EXIF-ориентацию.
     */
    @Nullable
    public static Bitmap loadBitmapFromUri(@NonNull Context ctx,
                                           @NonNull Uri uri,
                                           int targetW,
                                           int targetH) {
        ContentResolver cr = ctx.getContentResolver();

        // 1) EXIF-ориентация
        int orientation = ExifInterface.ORIENTATION_UNDEFINED;
        try (AssetFileDescriptor afd = cr.openAssetFileDescriptor(uri, "r")) {
            if (afd != null) {
                ExifInterface exif = new ExifInterface(afd.getFileDescriptor());
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (IOException ignore) {
            // если EXIF не прочитался — не страшно, просто не поворачиваем
        }

        Bitmap bmp;

        try {
            if (Build.VERSION.SDK_INT >= 28) {
                // 2a) API 28+: декодер из Uri (правильно!)
                ImageDecoder.Source src = ImageDecoder.createSource(cr, uri);
                bmp = ImageDecoder.decodeBitmap(src, (decoder, info, source) -> {
                    // таргетный семплинг под размеры вьюхи (с запасом x2 для резкости)
                    int srcW = info.getSize().getWidth();
                    int srcH = info.getSize().getHeight();
                    int sample = 1;
                    while ((srcW / sample) > targetW * 2 || (srcH / sample) > targetH * 2) {
                        sample *= 2;
                    }
                    decoder.setTargetSampleSize(Math.max(1, sample));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    decoder.setMutableRequired(true);
                });
            } else {
                // 2b) API < 28: дважды открываем stream — на bounds и на сам decode
                int srcW, srcH;
                try (InputStream inBounds = cr.openInputStream(uri)) {
                    if (inBounds == null) return null;
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inBounds, null, bounds);
                    srcW = bounds.outWidth;
                    srcH = bounds.outHeight;
                }

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                opts.inMutable = true;
                opts.inSampleSize = calcInSampleSize(srcW, srcH, targetW * 2, targetH * 2);

                try (InputStream in = cr.openInputStream(uri)) {
                    if (in == null) return null;
                    bmp = BitmapFactory.decodeStream(in, null, opts);
                }

                if (bmp != null && !bmp.isMutable()) {
                    bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (bmp == null) return null;

        // 3) Поворот/зеркало по EXIF
        Matrix m = exifMatrix(orientation);
        if (!m.isIdentity()) {
            Bitmap rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (rotated != bmp) {
                bmp.recycle();
                bmp = rotated;
            }
        }
        return bmp;
    }

    private static int calcInSampleSize(int srcW, int srcH, int reqW, int reqH) {
        int inSampleSize = 1;
        if (srcH > reqH || srcW > reqW) {
            final int halfH = srcH / 2;
            final int halfW = srcW / 2;
            while ((halfH / inSampleSize) >= reqH && (halfW / inSampleSize) >= reqW) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private static Matrix exifMatrix(int orientation) {
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:   m.postRotate(90);  break;
            case ExifInterface.ORIENTATION_ROTATE_180:  m.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270:  m.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.preScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:   m.preScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE:       m.preScale(-1, 1); m.postRotate(270); break;
            case ExifInterface.ORIENTATION_TRANSVERSE:      m.preScale(-1, 1); m.postRotate(90);  break;
            default: // normal / undefined
        }
        return m;
    }
}
