package com.example.memegenerator.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MemeDao {

    @Insert
    long insert(Meme meme);

    @Update
    void update(Meme meme);

    @Delete
    void delete(Meme meme);

    @Query("SELECT * FROM memes ORDER BY createdAt DESC")
    List<Meme> getAllDesc();

    @Query("SELECT COUNT(*) FROM memes")
    int getCount();

    @Query("UPDATE memes SET projectName = :projectName WHERE id = :id")
    void updateProjectName(long id, String projectName);
}