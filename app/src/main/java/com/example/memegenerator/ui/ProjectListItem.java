package com.example.memegenerator.ui;

import com.example.memegenerator.data.Project;

public class ProjectListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_PROJECT = 1;

    public final int type;
    public final String headerTitle;
    public final Project project;

    private ProjectListItem(int type, String headerTitle, Project project) {
        this.type = type;
        this.headerTitle = headerTitle;
        this.project = project;
    }

    public static ProjectListItem header(String title) {
        return new ProjectListItem(TYPE_HEADER, title, null);
    }

    public static ProjectListItem project(Project project) {
        return new ProjectListItem(TYPE_PROJECT, null, project);
    }
}