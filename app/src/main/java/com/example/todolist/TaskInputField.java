package com.example.todolist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Adapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.annotations.Nullable;

public class TaskInputField extends ConstraintLayout {
    private RecyclerView recyclerView;
    private TextView txtLabel;

    public TaskInputField(@NonNull Context context) {
        super(context);
        init(context);
    }
    public TaskInputField(
            @NonNull Context context,
            @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public TaskInputField(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    @SuppressLint("WrongViewCast")
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.task_input_field,this,true);
        txtLabel = findViewById(R.id.txt_task);
        recyclerView = findViewById(R.id.TaskRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(false);
    }
    public void setLabel(String label) {
        txtLabel.setText(label);
    }
    public void setRecyclerViewAdapter(
            RecyclerView.Adapter<?> adapter) {
        recyclerView.setAdapter(adapter);
    }
    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
