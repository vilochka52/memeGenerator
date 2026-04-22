package com.example.memegenerator.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {Project.class},
        version = 7,
        exportSchema = false
)
public abstract class ProjectDatabase extends RoomDatabase {

    private static volatile ProjectDatabase INSTANCE;

    public abstract ProjectDao projectDao();

    public static ProjectDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProjectDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ProjectDatabase.class,
                                    "project_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}