package com.example.memegenerator;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.io.InputStream;

public class MemeViewModel extends ViewModel {
    public final MutableLiveData<Bitmap> previewBitmap = new MutableLiveData<>();
    public final MutableLiveData<String> toastEvent = new MutableLiveData<>();
    private Bitmap sourceBitmap;
    private final MemeRepository repository = new MemeRepository();

    public void loadSourceBitmap(ContentResolver resolver, Uri uri) {
        try (InputStream in = resolver.openInputStream(uri)) {
            Bitmap decoded = BitmapFactory.decodeStream(in);
            sourceBitmap = decoded;
            previewBitmap.postValue(decoded);
            toastEvent.postValue("Изображение загружено");
        } catch (Exception e) {
            toastEvent.postValue("Не удалось загрузить изображение");
        }
    }

    public void saveMeme(Context ctx, Bitmap bmp) {
        String name = "meme_" + System.currentTimeMillis() + ".png";
        try {
            Uri uri = repository.saveToPictures(ctx, bmp, name);
            if (uri != null) toastEvent.postValue("Сохранено: Pictures/MemeGenerator/" + name);
            else toastEvent.postValue("Не удалось сохранить");
        } catch (Exception e) {
            toastEvent.postValue("Ошибка сохранения");
        }
    }
}
