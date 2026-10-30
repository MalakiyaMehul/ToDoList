package com.example.todolist.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.activity.EditTaskActivity;
import com.example.todolist.activity.TaskDetail;
import com.example.todolist.databinding.BottomSheetTaskActionsBinding;
import com.example.todolist.databinding.TaskCardBinding;
import com.example.todolist.model.TaskModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final ArrayList<TaskModel> taskModels;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel taskModel);
    }

    private final OnTaskClickListener listener;

    public TaskAdapter(ArrayList<TaskModel> taskModels) {
        this(taskModels, null);
    }

    public TaskAdapter(ArrayList<TaskModel> taskModels, OnTaskClickListener listener) {
        this.taskModels = taskModels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TaskCardBinding binding = TaskCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel taskModel = taskModels.get(position);
        TaskCardBinding binding = holder.binding;

        // Title
        binding.txtTitle.setText(taskModel.getTaskName() != null ? taskModel.getTaskName() : "Untitled Task");

        // Time
        String time = taskModel.getTime();
        binding.txtTime.setText(time != null && !time.isEmpty() ? time : "Scheduled");

        // Category
        binding.categoryInclude.txtCategory.setText(taskModel.getCategory() != null ? taskModel.getCategory() : "General");
        String categoryColor = taskModel.getCategoryColor();

        if (categoryColor != null && !categoryColor.trim().isEmpty()) {
            try {
                int color = Color.parseColor(categoryColor);
                binding.categoryInclude.bgcolor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            } catch (IllegalArgumentException e) {
                binding.categoryInclude.bgcolor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
            }
        }

        String categoryIcon = taskModel.getCategoryIcon();
        if (categoryIcon != null && !categoryIcon.trim().isEmpty()) {
            try {
                binding.categoryInclude.imgCategory.setImageResource(Integer.parseInt(categoryIcon));
            } catch (NumberFormatException e) {
                binding.categoryInclude.imgCategory.setImageResource(R.drawable.university);
            }
        }

        // Priority
        String priority = taskModel.getPriority();
        if (priority != null && !priority.isEmpty()) {
            binding.priorityInclude.priority.setText(priority);
        } else {
            binding.priorityInclude.priority.setText("Normal");
        }

        // Card margin
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) layoutParams;
            int spaceInDp = (int) (12 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            params.bottomMargin = spaceInDp;
            holder.itemView.setLayoutParams(params);
        }

        // Checkbox status toggle
        binding.radioCompleted.setOnCheckedChangeListener(null);
        binding.radioCompleted.setChecked(taskModel.isCompleted());
        binding.radioCompleted.setOnClickListener(v -> {
            boolean newStatus = !taskModel.isCompleted();
            binding.radioCompleted.setChecked(newStatus);
            taskModel.setCompleted(newStatus);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && taskModel.getTaskId() != null) {
                DatabaseReference taskRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(user.getUid())
                        .child("Tasks")
                        .child(taskModel.getTaskId());
                taskRef.child("completed").setValue(newStatus);
            }
        });

        // 1. Click on the entire TaskCard -> Open Task Details (TaskDetail)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(taskModel);
            } else {
                openTaskDetail(v.getContext(), taskModel);
            }
        });

        // 2. Click on Three Dots -> Open BottomSheet with Edit and Delete
        binding.btnTaskOptions.setOnClickListener(v -> {
            showTaskOptionsBottomSheet(v.getContext(), taskModel);
        });
    }

    private void showTaskOptionsBottomSheet(Context context, TaskModel taskModel) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        BottomSheetTaskActionsBinding sheetBinding = BottomSheetTaskActionsBinding.inflate(
                LayoutInflater.from(context)
        );
        bottomSheetDialog.setContentView(sheetBinding.getRoot());

        // Transparent dialog background to display custom rounded corners
        if (bottomSheetDialog.getWindow() != null) {
            View bottomSheetInternal = bottomSheetDialog.getWindow().findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        }

        // Set task titles
        String name = taskModel.getTaskName();
        sheetBinding.txtActionTaskTitle.setText(name != null && !name.trim().isEmpty() ? name : "Task Options");

        String category = taskModel.getCategory();
        String priority = taskModel.getPriority();
        sheetBinding.txtActionTaskSubtitle.setText(
                (category != null ? category : "Task") + " • " + (priority != null ? priority : "Normal Priority")
        );

        // Edit button click
        sheetBinding.btnActionEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openEditTask(context, taskModel);
        });

        // Delete button click
        sheetBinding.btnActionDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            confirmAndDeleteTask(context, taskModel);
        });

        bottomSheetDialog.show();
    }

    public static void openEditTask(Context context, TaskModel taskModel) {
        Intent intent = new Intent(context, EditTaskActivity.class);
        intent.putExtra("taskId", taskModel.getTaskId());
        intent.putExtra("taskName", taskModel.getTaskName());
        intent.putExtra("description", taskModel.getDescription());
        intent.putExtra("date", taskModel.getDate());
        intent.putExtra("time", taskModel.getTime());
        intent.putExtra("category", taskModel.getCategory());
        intent.putExtra("priority", taskModel.getPriority());
        intent.putExtra("categoryColor", taskModel.getCategoryColor());
        intent.putExtra("categoryIcon", taskModel.getCategoryIcon());
        intent.putExtra("completed", taskModel.isCompleted());
        context.startActivity(intent);
    }

    public static void openTaskDetail(Context context, TaskModel taskModel) {
        Intent intent = new Intent(context, TaskDetail.class);
        intent.putExtra("taskId", taskModel.getTaskId());
        intent.putExtra("taskName", taskModel.getTaskName());
        intent.putExtra("description", taskModel.getDescription());
        intent.putExtra("date", taskModel.getDate());
        intent.putExtra("time", taskModel.getTime());
        intent.putExtra("category", taskModel.getCategory());
        intent.putExtra("priority", taskModel.getPriority());
        intent.putExtra("categoryColor", taskModel.getCategoryColor());
        intent.putExtra("categoryIcon", taskModel.getCategoryIcon());
        intent.putExtra("completed", taskModel.isCompleted());
        context.startActivity(intent);
    }

    private void confirmAndDeleteTask(Context context, TaskModel taskModel) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + (taskModel.getTaskName() != null ? taskModel.getTaskName() : "this task") + "\"?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && taskModel.getTaskId() != null) {
                        FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(user.getUid())
                                .child("Tasks")
                                .child(taskModel.getTaskId())
                                .removeValue()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(context, "Task deleted successfully", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return taskModels.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TaskCardBinding binding;

        public TaskViewHolder(TaskCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
