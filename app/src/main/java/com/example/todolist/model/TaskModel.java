package com.example.todolist.model;

public class TaskModel {

    private String taskId;
    private String taskName;
    private String description;
    private String date;
    private String time;
    private String priority;
    private String category;
    private boolean completed;

    public TaskModel(String taskId, String taskName, String description, String date, String time, String category, int priority, boolean completed) {
    }

    public TaskModel(String taskId, String taskName, String description, String date, String time, String priority, String category, boolean completed) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.date = date;
        this.time = time;
        this.priority = category;
        this.category = priority;
        this.completed = completed;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public boolean isCompleted() {
        return completed;
    }
}