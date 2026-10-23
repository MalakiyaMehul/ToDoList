package com.example.todolist.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.activity.activity_create_category;
import com.example.todolist.model.CategoryModel;

import java.util.ArrayList;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private final ArrayList<CategoryModel> categoryList;
    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_ADD = 1;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryModel category);
    }

    public CategoryAdapter(ArrayList<CategoryModel> categoryList, OnCategoryClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryAdapter.CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ADD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_category, parent, false);

            return new CategoryViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);

        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.CategoryViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            return;
        }
        CategoryModel category = categoryList.get(position);

        holder.txtCategoryName.setText(category.getCategoryName());
        Context context = holder.itemView.getContext();

        int iconResId = context.getResources().getIdentifier(String.valueOf(category.getIcon()), "drawable", context.getPackageName());
        if (iconResId != 0) {
            holder.txtCategoryIcon.setImageResource(iconResId);
        } else {
            holder.txtCategoryIcon.setImageResource(
                    R.drawable.university
            );
        }
        try {
            float density = holder.itemView.getResources()
                    .getDisplayMetrics().density;

            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(8 * holder.itemView.getResources().getDisplayMetrics().density);
            background.setColor(Color.parseColor(category.getColor()));
            holder.txtCategoryIcon.setBackground(background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (80 * density), (int) (80 * density));
            holder.txtCategoryIcon.setLayoutParams(params);
            holder.txtCategoryIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } catch (Exception e) {
            holder.txtCategoryIcon.setBackgroundColor(Color.GRAY);
        }
        holder.itemView.setOnClickListener(v -> {
            listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (position == categoryList.size()) {
            return TYPE_ADD;
        }
        return TYPE_CATEGORY;
    }
    @Override
    public int getItemCount() {
        return categoryList.size() + 1;
    }
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategoryName;
        ImageView txtCategoryIcon;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryIcon = itemView.findViewById(R.id.txtCategoryIcon);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), activity_create_category.class);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}
