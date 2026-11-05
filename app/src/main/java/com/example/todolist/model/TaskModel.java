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
    private String username;

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

    public TaskModel(String taskId, String username, String taskName, String description, String date, String time, String category, String categoryColor, String categoryIcon, String priority, boolean completed) {
        this.taskId = taskId;
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
