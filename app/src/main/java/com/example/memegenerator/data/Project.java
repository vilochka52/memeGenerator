package com.example.memegenerator.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String projectName;
    public String originalImagePath;
    public String previewImagePath;
    public String textItemsJson;
    public long createdAt;

    public Project(String projectName,
                   String originalImagePath,
                   String previewImagePath,
                   String textItemsJson,
                   long createdAt) {
        this.projectName = projectName;
        this.originalImagePath = originalImagePath;
        this.previewImagePath = previewImagePath;
        this.textItemsJson = textItemsJson;
        this.createdAt = createdAt;
    }
}