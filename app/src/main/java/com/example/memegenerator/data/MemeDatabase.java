package com.example.memegenerator.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Meme.class}, version = 1, exportSchema = false)
public abstract class MemeDatabase extends RoomDatabase {
    private static volatile MemeDatabase INSTANCE;

    public abstract MemeDao memeDao();

    public static MemeDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MemeDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    MemeDatabase.class,
                                    "meme_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
