package com.example.todolist.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.Dialog.SubTaskBottomSheet;
import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.EditTaskActivity;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.activity.TaskDetail;
import com.example.todolist.databinding.BottomSheetTaskActionsBinding;
import com.example.todolist.databinding.TaskCardBinding;
import com.example.todolist.model.TaskModel;
import com.example.todolist.service.TaskReminderService;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final ArrayList<TaskModel> taskModels;
    private String searchQuery = "";

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

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.trim() : "";
        notifyDataSetChanged();
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
                binding.categoryInclude.bgcolor.setBackgroundTintList(ColorStateList.valueOf(color));
            } catch (IllegalArgumentException e) {
                binding.categoryInclude.bgcolor.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            }
        }

        String categoryIcon = taskModel.getCategoryIcon();
        String categoryName = taskModel.getCategory();
        Context context = holder.itemView.getContext();
        int iconRes = getCategoryIconDrawable(context, categoryIcon, categoryName);
        binding.categoryInclude.imgCategory.setImageResource(iconRes);
        binding.categoryInclude.imgCategory.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        String priority = taskModel.getPriority();
        if (priority != null && !priority.isEmpty()) {
            binding.priorityInclude.priority.setText(priority);
        } else {
            binding.priorityInclude.priority.setText("Normal");
        }

        // Highlight card if matching search query
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String q = searchQuery.toLowerCase();
            String name = taskModel.getTaskName() != null ? taskModel.getTaskName().toLowerCase() : "";
            String desc = taskModel.getDescription() != null ? taskModel.getDescription().toLowerCase() : "";
            String cat = taskModel.getCategory() != null ? taskModel.getCategory().toLowerCase() : "";
            if (name.contains(q) || desc.contains(q) || cat.contains(q)) {
                holder.itemView.setBackgroundResource(R.drawable.bg_task_card_highlighted);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_task_card);
            }
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_task_card);
        }

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
            if (taskModel.getTaskId() != null) {
                String token = SharedPrefManager.getInstance(v.getContext()).getString("access_token");
                if (token.isEmpty()) {
                    token = SupabaseAuth.getAccessToken(v.getContext());
                }
                new SupabaseRepository().updateTaskCompleted(taskModel.getTaskId(), token, newStatus, null);
                if (newStatus) {
                    TaskReminderService.stopIfMatchingTask(v.getContext(), taskModel.getTaskId());
                }
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
        sheetBinding.btnActionSubTask.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            SubTaskBottomSheet.show(context, taskModel, updatedSubTaskJson -> {
                taskModel.setSubTask(updatedSubTaskJson);
            });
        });
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
        intent.putExtra("subTask", taskModel.getSubTask());
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
        intent.putExtra("subTask", taskModel.getSubTask());
        context.startActivity(intent);
    }

    private void confirmAndDeleteTask(Context context, TaskModel taskModel) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + (taskModel.getTaskName() != null ? taskModel.getTaskName() : "this task") + "\"?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (taskModel.getTaskId() != null) {
                        String token = SharedPrefManager.getInstance(context).getString("access_token");
                        if (token.isEmpty()) {
                            token = SupabaseAuth.getAccessToken(context);
                        }
                        int pos = taskModels.indexOf(taskModel);
                        new SupabaseRepository().deleteTask(taskModel.getTaskId(), token, new SupabaseRepository.TaskCallback() {
                            @Override
                            public void onSuccess(String taskId) {
                                if (pos != -1 && pos < taskModels.size()) {
                                    taskModels.remove(pos);
                                    notifyItemRemoved(pos);
                                }
                                Toast.makeText(context, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(context, "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return taskModels.size();
    }

    public static int getCategoryIconDrawable(Context context, String categoryIcon, String categoryName) {
        if (categoryIcon != null && !categoryIcon.trim().isEmpty()) {
            String cleanIcon = categoryIcon.trim();
            try {
                int resId = Integer.parseInt(cleanIcon);
                if (context != null) {
                    context.getResources().getResourceName(resId);
                }
                return resId;
            } catch (Exception ignored) {}

            if (context != null) {
                try {
                    int nameResId = context.getResources().getIdentifier(cleanIcon, "drawable", context.getPackageName());
                    if (nameResId != 0) {
                        return nameResId;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (categoryName != null) {
            String cat = categoryName.trim().toLowerCase(Locale.ROOT);
            if (cat.contains("work")) return R.drawable.ic_briefcase_custom;
            if (cat.contains("personal")) return R.drawable.ic_user_custom;
            if (cat.contains("design")) return R.drawable.ic_design_custom;
            if (cat.contains("health")) return R.drawable.ic_health_custom;
            if (cat.contains("edu") || cat.contains("study") || cat.contains("school")) return R.drawable.university;
            if (cat.contains("finance") || cat.contains("money") || cat.contains("pay")) return R.drawable.tag;
            if (cat.contains("shop") || cat.contains("grocery")) return R.drawable.checklist_rafiki;
            if (cat.contains("fit") || cat.contains("sport") || cat.contains("gym")) return R.drawable.sport;
            if (cat.contains("home") || cat.contains("house")) return R.drawable.home;
            if (cat.contains("music") || cat.contains("song")) return R.drawable.music;
            if (cat.contains("camera") || cat.contains("photo")) return R.drawable.camera;
        }

        return R.drawable.ic_briefcase_custom;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TaskCardBinding binding;

        public TaskViewHolder(TaskCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
