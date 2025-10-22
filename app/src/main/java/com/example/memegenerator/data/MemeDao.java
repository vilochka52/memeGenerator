package com.example.memegenerator.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MemeDao {
    @Insert
    long insert(Meme meme);

    @Query("SELECT * FROM memes ORDER BY createdAt DESC")
    List<Meme> getAllDesc();

    @Query("DELETE FROM memes")
    void clearAll();
}
