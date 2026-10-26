
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
    private String categoryColor;
    private String categoryIcon;

    // Required by Firebase
    public TaskModel() {
    }

    public TaskModel(String taskId, String taskName, String description, String date, String time, String category, String categoryColor, String categoryIcon, String priority, boolean completed) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.date = date;
        this.time = time;
        this.category = category;
        this.categoryColor = categoryColor;
        this.categoryIcon = categoryIcon;
        this.priority = priority;
        this.completed = completed;
    }

    public String getDescription() {
        return description;
    }
    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
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

    public String getCategoryColor() {
        return categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public boolean isCompleted() {
        return completed;
    }
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
