package com.example.memegenerator.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProjectDao {

    @Insert
    long insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    List<Project> getAllDesc();

    @Query("SELECT COUNT(*) FROM projects")
    int getCount();

    @Query("UPDATE projects SET projectName = :projectName WHERE id = :id")
    void updateProjectName(long id, String projectName);
}