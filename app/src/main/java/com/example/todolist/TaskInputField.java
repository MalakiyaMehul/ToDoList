package com.example.todolist;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TaskInputField extends FrameLayout {
    private RecyclerView recyclerView;
    private TextView txtLabel;
    private LinearLayout btnTask;
    private ImageButton btnArrow;

    public TaskInputField(@NonNull Context context) {
        super(context);
        init(context);
    }

    public TaskInputField(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TaskInputField(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.task_input_field, this, true);
        txtLabel = findViewById(R.id.txt_task);
        btnTask = findViewById(R.id.btn_task);
        btnArrow = findViewById(R.id.btn_arrow);

        recyclerView = findViewById(R.id.TaskRecycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setHasFixedSize(false);
        recyclerView.setNestedScrollingEnabled(false);

        if (btnTask != null) {
            btnTask.setOnClickListener(v -> {
                if (recyclerView.getVisibility() == View.VISIBLE) {
                    recyclerView.setVisibility(View.GONE);
                    if (btnArrow != null) {
                        btnArrow.animate().rotation(-90f).setDuration(200).start();
                    }
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (btnArrow != null) {
                        btnArrow.animate().rotation(0f).setDuration(200).start();
                    }
                }
            });
        }
    }

    public void setLabel(String label) {
        if (txtLabel != null) {
            txtLabel.setText(label);
        }
    }

    public void setRecyclerViewAdapter(RecyclerView.Adapter<?> adapter) {
        recyclerView.setAdapter(adapter);
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
