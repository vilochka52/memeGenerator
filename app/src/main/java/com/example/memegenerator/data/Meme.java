package com.example.memegenerator.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "memes")
public class Meme {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String imagePath;   // абсолютный путь к сохранённой картинке

    public String topText;     // верхний текст (если используешь)
    public String bottomText;  // нижний текст (если используешь)
    public long createdAt;     // System.currentTimeMillis()

    public Meme(@NonNull String imagePath, String topText, String bottomText, long createdAt) {
        this.imagePath = imagePath;
        this.topText = topText;
        this.bottomText = bottomText;
        this.createdAt = createdAt;
    }
}
