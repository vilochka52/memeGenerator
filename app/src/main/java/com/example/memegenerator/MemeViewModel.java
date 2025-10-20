package com.example.memegenerator;

import android.graphics.Color;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MemeViewModel extends ViewModel {

    private final MutableLiveData<List<TextItem>> textItems = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<TextItem>> getTextItems() { return textItems; }

    public void addText(@NonNull String text, float sizeSp) {
        List<TextItem> cur = new ArrayList<>(Objects.requireNonNull(textItems.getValue()));
        cur.add(new TextItem(text, sizeSp, 80f, 120f, Typeface.BOLD, Color.WHITE));
        textItems.setValue(cur);
    }

    /** Добавляет текст в заданные координаты (обычно центр view). */
    public void addTextCentered(@NonNull String text, float sizeSp, float x, float y) {
        List<TextItem> cur = new ArrayList<>(Objects.requireNonNull(textItems.getValue()));
        cur.add(new TextItem(text, sizeSp, x, y, Typeface.BOLD, Color.WHITE));
        textItems.setValue(cur);
    }

    public void updateItem(int index, @NonNull TextItem item) {
        List<TextItem> cur = new ArrayList<>(Objects.requireNonNull(textItems.getValue()));
        if (index >= 0 && index < cur.size()) {
            cur.set(index, item);
            textItems.setValue(cur);
        }
    }

    public void replaceAll(@NonNull List<TextItem> newItems) {
        textItems.setValue(new ArrayList<>(newItems));
    }
}
