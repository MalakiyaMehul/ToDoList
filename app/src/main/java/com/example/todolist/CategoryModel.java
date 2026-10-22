package com.example.todolist.model;

public class CategoryModel {

    private String categoryName;
    private int icon;
    private String color;



    public CategoryModel(String categoryName, int icon, String color) {
        this.categoryName = categoryName;
        this.icon = icon;
        this.color = color;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }
}