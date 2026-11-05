package com.example.todolist.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityTaskDetailBinding;
import com.example.todolist.model.TaskModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskDetail extends BaseActivity {

    private ActivityTaskDetailBinding binding;
    private String taskId;
    private String taskName = "";
    private String description = "";
    private String date = "";
    private String time = "";
    private String category = "Design";
    private String priority = "High";
    private String categoryColor;
    private String categoryIcon = "";
    private boolean completed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        extractIntentData();
        setupUI();
        loadSavedAvatar();
        setupListeners();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        taskId = intent.getStringExtra("taskId");
        taskName = intent.getStringExtra("taskName");
        description = intent.getStringExtra("description");
        date = intent.getStringExtra("date");
        time = intent.getStringExtra("time");
        category = intent.getStringExtra("category");
        priority = intent.getStringExtra("priority");
        categoryColor = intent.getStringExtra("categoryColor");
        categoryIcon = intent.getStringExtra("categoryIcon");
        completed = intent.getBooleanExtra("completed", false);
    }

    private void setupUI() {
        // Task Title
        if (taskName != null && !taskName.trim().isEmpty()) {
            binding.txtTaskName.setText(taskName);
        } else {
            binding.txtTaskName.setText("Untitled Task");
        }

        // Category Chip
        if (category != null && !category.trim().isEmpty()) {
            binding.txtCategory.setText(category);
        } else {
            binding.txtCategory.setText("Design");
        }

        if (categoryColor != null && !categoryColor.trim().isEmpty()) {
            try {
                int c = Color.parseColor(categoryColor.trim());
                binding.layoutCategoryChip.setBackgroundTintList(ColorStateList.valueOf(c));
            } catch (Exception ignored) {}
        }

        if (categoryIcon != null && !categoryIcon.trim().isEmpty()) {
            try {
                binding.imgCategoryIcon.setImageResource(Integer.parseInt(categoryIcon.trim()));
            } catch (Exception ignored) {
                binding.imgCategoryIcon.setImageResource(R.drawable.ic_design_custom);
            }
        } else {
            binding.imgCategoryIcon.setImageResource(R.drawable.ic_design_custom);
        }

        // Priority Chip
        updatePriorityUI(priority);

        // Status Chip
        updateStatusUI();

        // Description
        if (description != null && !description.trim().isEmpty()) {
            binding.txtDescription.setText(description);
        } else {
            binding.txtDescription.setText("No additional details provided for this task.");
        }

        // Date Cards
        setupDateUI(date);

        // Time Cards
        setupTimeUI(time);

        // Completion Status
        updateCompletionUI();
    }

    private void updatePriorityUI(String prio) {
        if (prio == null) prio = "High";
        priority = prio;

        String label;
        int textColor;
        int bgTint;

        if (prio.equalsIgnoreCase("High") || prio.equals("1")) {
            label = "High Priority";
            textColor = Color.parseColor("#F43F5E");
            bgTint = Color.parseColor("#381A24");
        } else if (prio.equalsIgnoreCase("Low") || prio.equals("3")) {
            label = "Low Priority";
            textColor = Color.parseColor("#94A3B8");
            bgTint = Color.parseColor("#1F2430");
        } else {
            label = "Medium Priority";
            textColor = Color.parseColor("#C084FC");
            bgTint = Color.parseColor("#291C3D");
        }

        binding.txtPriority.setText(label);
        binding.txtPriority.setTextColor(textColor);
        binding.layoutPriorityChip.setBackgroundTintList(ColorStateList.valueOf(bgTint));
    }

    private void updateStatusUI() {
        if (completed) {
            binding.txtStatus.setText("Completed");
            binding.txtStatus.setTextColor(Color.parseColor("#10B981"));
            binding.imgStatusIcon.setColorFilter(Color.parseColor("#10B981"));
        } else {
            binding.txtStatus.setText("In Progress");
            binding.txtStatus.setTextColor(Color.parseColor("#8E93A0"));
            binding.imgStatusIcon.setColorFilter(Color.parseColor("#8E93A0"));
        }
    }

    private void setupDateUI(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            binding.txtDueDateDay.setText("Today");
            binding.txtDueDateFull.setText(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date()));
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = sdf.parse(dateStr);
            if (d != null) {
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTime(d);

                Calendar today = Calendar.getInstance();
                if (taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    binding.txtDueDateDay.setText("Today");
                } else {
                    binding.txtDueDateDay.setText(new SimpleDateFormat("EEE", Locale.ENGLISH).format(d));
                }
                binding.txtDueDateFull.setText(new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(d));
                return;
            }
        } catch (Exception ignored) {}

        binding.txtDueDateDay.setText("Due Date");
        binding.txtDueDateFull.setText(dateStr);
    }

    private void setupTimeUI(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            binding.txtTargetTime.setText("02:30 PM");
            binding.txtTargetTimeStatus.setText("Scheduled");
            return;
        }

        String cleaned = timeStr;
        if (cleaned.contains("•")) {
            cleaned = cleaned.substring(0, cleaned.indexOf("•")).trim();
        }
        if (cleaned.contains("(")) {
            cleaned = cleaned.substring(0, cleaned.indexOf("(")).trim();
        }

        binding.txtTargetTime.setText(cleaned);
        binding.txtTargetTimeStatus.setText(completed ? "Done" : "Scheduled");
    }

    private void updateCompletionUI() {
        if (completed) {
            binding.chkObjectiveIndicator.setBackgroundResource(R.drawable.bg_timeline_checkbox_checked);
            binding.imgObjectiveIndicator.setColorFilter(Color.WHITE);
            binding.txtTaskName.setPaintFlags(binding.txtTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            binding.txtMarkCompleted.setText("Mark as Incomplete");
            binding.imgCompleteIcon.setImageResource(R.drawable.ic_circle_reload);
        } else {
            binding.chkObjectiveIndicator.setBackgroundResource(R.drawable.bg_timeline_checkbox);
            binding.imgObjectiveIndicator.setColorFilter(Color.parseColor("#4B4D5E"));
            binding.txtTaskName.setPaintFlags(binding.txtTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            binding.txtMarkCompleted.setText("Mark as Completed");
            binding.imgCompleteIcon.setImageResource(R.drawable.ic_check_simple);
        }
        updateStatusUI();
    }

    private void setupListeners() {
        // Back Navigation
        binding.btnBack.setOnClickListener(v -> finish());

        // More options
        binding.btnMoreOptions.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, "Edit Task");
            popup.getMenu().add(0, 2, 1, "Delete Task");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    openEditTaskActivity();
                    return true;
                } else if (item.getItemId() == 2) {
                    confirmAndDeleteTask();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // Edit Objective / Pen
        binding.btnEditObjective.setOnClickListener(v -> openEditTaskActivity());

        // Edit Task Button
        binding.btnEditTask.setOnClickListener(v -> openEditTaskActivity());

        binding.btnMarkCompleted.setOnClickListener(v -> toggleCompletedState());
        binding.chkObjectiveIndicator.setOnClickListener(v -> toggleCompletedState());

        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.txtReminderTitle.setText(isChecked ? "Pulse Reminder: Enabled" : "Pulse Reminder: Disabled");
        });

        binding.btnDeleteTask.setOnClickListener(v -> confirmAndDeleteTask());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTaskDetails();
    }

    private void loadTaskDetails() {
        String uid = SharedPrefManager.getInstance(this).getString("user_id");
        if (uid.isEmpty()) {
            uid = SupabaseAuth.getUserId(this);
        }
        String token = SharedPrefManager.getInstance(this).getString("access_token");
        if (token.isEmpty()) {
            token = SupabaseAuth.getAccessToken(this);
        }
        if (uid == null || uid.isEmpty() || taskId == null || taskId.trim().isEmpty()) return;

        new SupabaseRepository().getTasks(uid, token, new SupabaseRepository.GetTasksCallback() {
            @Override
            public void onSuccess(java.util.List<TaskModel> tasks) {
                for (TaskModel model : tasks) {
                    if (taskId.equals(model.getTaskId())) {
                        taskName = model.getTaskName();
                        description = model.getDescription();
                        date = model.getDate();
                        time = model.getTime();
                        category = model.getCategory();
                        priority = model.getPriority();
                        categoryColor = model.getCategoryColor();
                        categoryIcon = model.getCategoryIcon();
                        completed = model.isCompleted();
                        setupUI();
                        break;
                    }
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void toggleCompletedState() {
        completed = !completed;
        updateCompletionUI();

        if (taskId != null && !taskId.trim().isEmpty()) {
            String token = SharedPrefManager.getInstance(this).getString("access_token");
            if (token.isEmpty()) {
                token = SupabaseAuth.getAccessToken(this);
            }
            new SupabaseRepository().updateTaskCompleted(taskId, token, completed, null);
        }
        Toast.makeText(this, completed ? "Task completed! 🎉" : "Task marked active", Toast.LENGTH_SHORT).show();
    }

    private void openEditTaskActivity() {
        Intent intent = new Intent(this, EditTaskActivity.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("taskName", taskName);
        intent.putExtra("description", description);
        intent.putExtra("date", date);
        intent.putExtra("time", time);
        intent.putExtra("category", category);
        intent.putExtra("priority", priority);
        intent.putExtra("categoryColor", categoryColor);
        intent.putExtra("categoryIcon", categoryIcon);
        intent.putExtra("completed", completed);
        startActivity(intent);
    }

    private void confirmAndDeleteTask() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (taskId != null && !taskId.trim().isEmpty()) {
                        String token = SharedPrefManager.getInstance(this).getString("access_token");
                        if (token.isEmpty()) {
                            token = SupabaseAuth.getAccessToken(this);
                        }
                        new SupabaseRepository().deleteTask(taskId, token, new SupabaseRepository.TaskCallback() {
                            @Override
                            public void onSuccess(String taskId) {
                                Toast.makeText(TaskDetail.this, "Task deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(TaskDetail.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .show();
    }
    private void loadSavedAvatar() {
        String savedAvatarUri = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            try {
                File file = new File(savedAvatarUri);
                if (file.exists()) {
                    binding.ivTopAvatar.setImageURI(null);
                    binding.ivTopAvatar.setImageURI(Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
    }
}

