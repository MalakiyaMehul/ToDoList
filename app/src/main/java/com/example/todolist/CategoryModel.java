package com.example.todolist.model;

public class CategoryModel {

    private String categoryName;
    private String icon;
    private String color;



    public CategoryModel(String categoryName, String icon, String color) {
        this.categoryName = categoryName;
        this.icon = icon;
        this.color = color;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }
}