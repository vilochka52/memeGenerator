package com.example.memegenerator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.OutputStream;

public class MemeRepository {
    public Uri saveToPictures(Context ctx, Bitmap bitmap, String displayName) throws Exception {
        ContentResolver resolver = ctx.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MemeGenerator");
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = resolver.insert(collection, values);
        if (item == null) return null;
        try (OutputStream os = resolver.openOutputStream(item)) {
            if (os == null) return null;
            boolean ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            if (!ok) return null;
        }
        return item;
    }
}
