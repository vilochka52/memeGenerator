package com.example.memegenerator;

import android.graphics.Color;
import android.graphics.Typeface;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditorViewModel extends ViewModel {

    private final MutableLiveData<List<TextItem>> textItems =
            new MutableLiveData<>(new ArrayList<>());

    private final Stack<List<TextItem>> undoStack = new Stack<>();
    private final Stack<List<TextItem>> redoStack = new Stack<>();

    public LiveData<List<TextItem>> getTextItems() {
        return textItems;
    }

    private List<TextItem> copy(List<TextItem> src) {
        if (src == null) return new ArrayList<>();
        return new ArrayList<>(src);
    }

    private void saveState() {
        undoStack.push(copy(textItems.getValue()));
        redoStack.clear();
    }

    public void clearAll() {
        textItems.postValue(new ArrayList<>());
        undoStack.clear();
        redoStack.clear();
    }

    public void setItems(List<TextItem> items) {
        textItems.postValue(copy(items));
        undoStack.clear();
        redoStack.clear();
    }

    public void addTextCentered(String text, float size, float x, float y) {
        saveState();

        List<TextItem> list = copy(textItems.getValue());
        list.add(new TextItem(
                text,
                size,
                x,
                y,
                Typeface.NORMAL,
                Color.WHITE,
                TextItem.ALIGN_CENTER,
                0f
        ));
        textItems.setValue(list);
    }

    public void updateItem(int index, TextItem item) {
        List<TextItem> current = copy(textItems.getValue());
        if (index < 0 || index >= current.size()) return;

        saveState();
        current.set(index, item);
        textItems.setValue(current);
    }

    public void removeItem(int index) {
        List<TextItem> current = copy(textItems.getValue());
        if (index < 0 || index >= current.size()) return;

        saveState();
        current.remove(index);
        textItems.setValue(current);
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(copy(textItems.getValue()));
            textItems.setValue(undoStack.pop());
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(copy(textItems.getValue()));
            textItems.setValue(redoStack.pop());
        }
    }

    public String exportToJson() {
        List<TextItem> list = textItems.getValue();
        if (list == null) {
            list = new ArrayList<>();
        }
        return new com.google.gson.Gson().toJson(list);
    }
}