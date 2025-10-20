package com.example.memegenerator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.OutputStream;

public final class MemeRepository {

    private MemeRepository() {}

    @Nullable
    public static Uri saveBitmapToGallery(@NonNull Context ctx,
                                          @NonNull Bitmap bmp,
                                          @NonNull String fileName) {
        ContentResolver cr = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MemeGenerator");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return null;

        try (OutputStream out = cr.openOutputStream(uri)) {
            if (out == null) return null;
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            ContentValues fin = new ContentValues();
            fin.put(MediaStore.Images.Media.IS_PENDING, 0);
            cr.update(uri, fin, null, null);
        }
        return uri;
    }
}
