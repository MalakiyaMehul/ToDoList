package com.example.todolist.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;

import com.example.todolist.R;
import com.example.todolist.databinding.ActivityEditTaskBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTaskActivity extends BaseActivity {

    private ActivityEditTaskBinding binding;
    private String taskId;
    private String categoryColor;
    private String categoryIcon = "";
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedCategory = "Work";
    private String selectedPriority = "High";
    private boolean completed = false;
    private int selectedAlertOffsetMinutes = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        extractIntentData();
        setupUI();
        loadSavedAvatar();
        setupListeners();
    }

    private void extractIntentData() {
        taskId = getIntent().getStringExtra("taskId");

        String taskName = getIntent().getStringExtra("taskName");
        if (taskName != null && !taskName.trim().isEmpty()) {
            binding.edtTaskName.setText(taskName);
        }

        String description = getIntent().getStringExtra("description");
        if (description != null && !description.trim().isEmpty()) {
            binding.edtDescription.setText(description);
        }

        selectedTime = getIntent().getStringExtra("time");
        selectedDate = getIntent().getStringExtra("date");

        String cat = getIntent().getStringExtra("category");
        if (cat != null && !cat.trim().isEmpty()) {
            selectedCategory = cat;
        }

        categoryColor = getIntent().getStringExtra("categoryColor");
        categoryIcon = getIntent().getStringExtra("categoryIcon");

        String prio = getIntent().getStringExtra("priority");
        if (prio != null && !prio.trim().isEmpty()) {
            selectedPriority = prio;
        }

        completed = getIntent().getBooleanExtra("completed", false);
    }

    private void setupUI() {
        // ID badge
        if (taskId != null && !taskId.isEmpty()) {
            String shortId = taskId.length() > 4 ? taskId.substring(0, 4) : taskId;
            binding.txtTaskIdBadge.setText("TASK #" + shortId.toUpperCase(Locale.ENGLISH));
        } else {
            binding.txtTaskIdBadge.setText("TASK #4029");
        }

        // Category UI
        updateCategoryUI(selectedCategory);

        // Priority UI
        updatePriorityUI(selectedPriority);

        // Date & Time UI
        setupDateUI(selectedDate);
        setupTimeUI(selectedTime);

        // Alert offset initial state
        updateAlertOffsetUI(selectedAlertOffsetMinutes);
    }

    private void setupDateUI(String dateStr) {
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            binding.txtDueDate.setText(formatDisplayDate(dateStr));
            binding.txtDueCountdown.setText("Ends in 6h 40m");
        } else {
            String todayStr = new SimpleDateFormat("MMM d", Locale.ENGLISH).format(new Date());
            binding.txtDueDate.setText("Today, " + todayStr);
            binding.txtDueCountdown.setText("Ends in 6h 40m");
        }
    }

    private void setupTimeUI(String timeStr) {
        if (timeStr != null && !timeStr.trim().isEmpty()) {
            binding.txtTaskTime.setText(formatDisplayTime(timeStr));
            binding.txtTimeDuration.setText("45m slot allocated");
        } else {
            binding.txtTaskTime.setText("10:00 AM");
            binding.txtTimeDuration.setText("45m slot allocated");
        }
    }

    private void updateCategoryUI(String category) {
        if (category == null || category.trim().isEmpty()) {
            category = "Work";
        }
        selectedCategory = category;
        binding.txtSelectedCategoryLabel.setText("Selected: " + category);

        // Reset all 4 cards to unselected state
        binding.cardCategoryWork.setBackgroundResource(R.drawable.bg_category_card_unselected);
        binding.imgWorkCheck.setVisibility(View.GONE);
        binding.txtSubWork.setTextColor(Color.parseColor("#8E93A0"));

        binding.cardCategoryPersonal.setBackgroundResource(R.drawable.bg_category_card_unselected);
        binding.imgPersonalCheck.setVisibility(View.GONE);
        binding.txtSubPersonal.setTextColor(Color.parseColor("#8E93A0"));

        binding.cardCategoryDesign.setBackgroundResource(R.drawable.bg_category_card_unselected);
        binding.imgDesignCheck.setVisibility(View.GONE);
        binding.txtSubDesign.setTextColor(Color.parseColor("#8E93A0"));

        binding.cardCategoryHealth.setBackgroundResource(R.drawable.bg_category_card_unselected);
        binding.imgHealthCheck.setVisibility(View.GONE);
        binding.txtSubHealth.setTextColor(Color.parseColor("#8E93A0"));

        // Highlight selected card
        if (category.equalsIgnoreCase("Work")) {
            binding.cardCategoryWork.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.imgWorkCheck.setVisibility(View.VISIBLE);
            binding.txtSubWork.setTextColor(Color.parseColor("#E0E7FF"));
            categoryColor = "#7C3AED";
        } else if (category.equalsIgnoreCase("Personal")) {
            binding.cardCategoryPersonal.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.imgPersonalCheck.setVisibility(View.VISIBLE);
            binding.txtSubPersonal.setTextColor(Color.parseColor("#E0E7FF"));
            categoryColor = "#2563EB";
        } else if (category.equalsIgnoreCase("Design")) {
            binding.cardCategoryDesign.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.imgDesignCheck.setVisibility(View.VISIBLE);
            binding.txtSubDesign.setTextColor(Color.parseColor("#E0E7FF"));
            categoryColor = "#A855F7";
        } else if (category.equalsIgnoreCase("Health")) {
            binding.cardCategoryHealth.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.imgHealthCheck.setVisibility(View.VISIBLE);
            binding.txtSubHealth.setTextColor(Color.parseColor("#E0E7FF"));
            categoryColor = "#10B981";
        } else {
            // Default to Work if unknown
            binding.cardCategoryWork.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.imgWorkCheck.setVisibility(View.VISIBLE);
            binding.txtSubWork.setTextColor(Color.parseColor("#E0E7FF"));
        }
    }

    private void updatePriorityUI(String priority) {
        if (priority == null || priority.trim().isEmpty()) {
            priority = "High";
        }
        selectedPriority = priority;

        // Reset all priority chips to unselected
        binding.chipPriorityLow.setBackgroundResource(R.drawable.bg_priority_chip_unselected);
        binding.dotPriorityLow.setTextColor(Color.parseColor("#8E93A0"));
        binding.txtPriorityLow.setTextColor(Color.WHITE);
        binding.subPriorityLow.setTextColor(Color.parseColor("#8E93A0"));

        binding.chipPriorityMedium.setBackgroundResource(R.drawable.bg_priority_chip_unselected);
        binding.dotPriorityMedium.setTextColor(Color.parseColor("#C084FC"));
        binding.txtPriorityMedium.setTextColor(Color.WHITE);
        binding.subPriorityMedium.setTextColor(Color.parseColor("#8E93A0"));

        binding.chipPriorityHigh.setBackgroundResource(R.drawable.bg_priority_chip_unselected);
        binding.dotPriorityHigh.setTextColor(Color.parseColor("#F43F5E"));
        binding.txtPriorityHigh.setTextColor(Color.WHITE);
        binding.subPriorityHigh.setTextColor(Color.parseColor("#8E93A0"));

        if (priority.equalsIgnoreCase("High") || priority.equals("1")) {
            binding.chipPriorityHigh.setBackgroundResource(R.drawable.bg_priority_chip_high_selected);
            binding.dotPriorityHigh.setTextColor(Color.WHITE);
            binding.txtPriorityHigh.setTextColor(Color.WHITE);
            binding.subPriorityHigh.setTextColor(Color.WHITE);

            binding.txtPriorityStatusLabel.setText("Urgent attention");
            binding.txtPriorityStatusLabel.setTextColor(Color.parseColor("#F43F5E"));
        } else if (priority.equalsIgnoreCase("Low") || priority.equals("3")) {
            binding.chipPriorityLow.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.dotPriorityLow.setTextColor(Color.WHITE);
            binding.txtPriorityLow.setTextColor(Color.WHITE);
            binding.subPriorityLow.setTextColor(Color.WHITE);

            binding.txtPriorityStatusLabel.setText("Low Priority");
            binding.txtPriorityStatusLabel.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            // Medium
            binding.chipPriorityMedium.setBackgroundResource(R.drawable.bg_category_card_selected);
            binding.dotPriorityMedium.setTextColor(Color.WHITE);
            binding.txtPriorityMedium.setTextColor(Color.WHITE);
            binding.subPriorityMedium.setTextColor(Color.WHITE);

            binding.txtPriorityStatusLabel.setText("Normal attention");
            binding.txtPriorityStatusLabel.setTextColor(Color.parseColor("#C084FC"));
        }
    }

    private void updateAlertOffsetUI(int minutes) {
        selectedAlertOffsetMinutes = minutes;

        binding.pillOffset15m.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset15m.setTextColor(Color.parseColor("#8E93A0"));

        binding.pillOffset30m.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset30m.setTextColor(Color.parseColor("#8E93A0"));

        binding.pillOffset1h.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset1h.setTextColor(Color.parseColor("#8E93A0"));

        if (minutes == 15) {
            binding.pillOffset15m.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset15m.setTextColor(Color.WHITE);
        } else if (minutes == 30) {
            binding.pillOffset30m.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset30m.setTextColor(Color.WHITE);
        } else if (minutes == 60) {
            binding.pillOffset1h.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset1h.setTextColor(Color.WHITE);
        }
    }

    private void setupListeners() {
        // Close Buttons
        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnHeaderClose.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                updateTaskInFirebase(false);
                finish();
            }
        });

        // Clear Task Title
        binding.btnClearTitle.setOnClickListener(v -> {
            binding.edtTaskName.setText("");
            binding.edtTaskName.requestFocus();
        });

        // Top Save & Bottom Save
        binding.btnTopSave.setOnClickListener(v -> updateTaskInFirebase(true));
        binding.btnEditTask.setOnClickListener(v -> updateTaskInFirebase(true));

        // Trash Delete at top and bottom
        binding.btnHeaderDelete.setOnClickListener(v -> deleteTask());
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        // Category Cards Selection
        binding.cardCategoryWork.setOnClickListener(v -> {
            updateCategoryUI("Work");
            updateTaskInFirebase(false);
        });

        binding.cardCategoryPersonal.setOnClickListener(v -> {
            updateCategoryUI("Personal");
            updateTaskInFirebase(false);
        });

        binding.cardCategoryDesign.setOnClickListener(v -> {
            updateCategoryUI("Design");
            updateTaskInFirebase(false);
        });

        binding.cardCategoryHealth.setOnClickListener(v -> {
            updateCategoryUI("Health");
            updateTaskInFirebase(false);
        });

        // Priority Chips Selection
        binding.chipPriorityLow.setOnClickListener(v -> {
            updatePriorityUI("Low");
            updateTaskInFirebase(false);
        });

        binding.chipPriorityMedium.setOnClickListener(v -> {
            updatePriorityUI("Medium");
            updateTaskInFirebase(false);
        });

        binding.chipPriorityHigh.setOnClickListener(v -> {
            updatePriorityUI("High");
            updateTaskInFirebase(false);
        });

        // Schedule: Due Date Picker
        binding.cardDueDate.setOnClickListener(v -> openCalendarPicker());

        // Schedule: Target Time Picker
        binding.cardTargetTime.setOnClickListener(v -> openTimePicker());

        // Pulse Reminder Switch
        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.txtReminderTitle.setText(isChecked ? "Pulse Reminder: Enabled" : "Pulse Reminder: Disabled");
        });

        // Alert Offset Pills
        binding.pillOffset15m.setOnClickListener(v -> updateAlertOffsetUI(15));
        binding.pillOffset30m.setOnClickListener(v -> updateAlertOffsetUI(30));
        binding.pillOffset1h.setOnClickListener(v -> updateAlertOffsetUI(60));

        // Add Collaborator
        binding.btnAddCollaborator.setOnClickListener(v ->
                Toast.makeText(this, "Collaborator synced", Toast.LENGTH_SHORT).show()
        );

        // Header Menu Options
        binding.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 1, 0, "Delete Task");
            popup.getMenu().add(0, 2, 1, "Clear Fields");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    deleteTask();
                    return true;
                } else if (item.getItemId() == 2) {
                    binding.edtTaskName.setText("");
                    binding.edtDescription.setText("");
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void openCalendarPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            Calendar pickedCal = Calendar.getInstance();
            pickedCal.set(year, month, day);

            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            setupDateUI(selectedDate);
            updateTaskInFirebase(false);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour, minute) -> {
            String amPm = hour >= 12 ? "PM" : "AM";
            int hourIn12 = hour % 12;
            if (hourIn12 == 0) hourIn12 = 12;

            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12, minute, amPm);
            binding.txtTaskTime.setText(selectedTime);
            updateTaskInFirebase(false);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat parseFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = parseFormat.parse(dateStr);
            if (d != null) {
                Calendar today = Calendar.getInstance();
                Calendar target = Calendar.getInstance();
                target.setTime(d);

                if (today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
                    return "Today, " + new SimpleDateFormat("MMM d", Locale.ENGLISH).format(d);
                }
                return new SimpleDateFormat("EEE, MMM d", Locale.ENGLISH).format(d);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    private String formatDisplayTime(String timeStr) {
        if (timeStr == null) return "10:00 AM";
        if (timeStr.contains("(") || timeStr.contains("•")) {
            return timeStr.split("[•(]")[0].trim();
        }
        return timeStr;
    }

    private void updateTaskInFirebase(boolean finishAfterSave) {
        String taskName = binding.edtTaskName.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();

        if (taskName.isEmpty()) {
            if (finishAfterSave) {
                binding.edtTaskName.setError("Task title cannot be empty");
            }
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (finishAfterSave) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (taskId == null || taskId.trim().isEmpty()) {
            if (finishAfterSave) {
                Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference taskRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("Tasks")
                .child(taskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("taskId", taskId);
        updates.put("taskName", taskName);
        updates.put("description", description);
        if (!TextUtils.isEmpty(selectedDate)) {
            updates.put("date", selectedDate);
        }
        if (!TextUtils.isEmpty(selectedTime)) {
            updates.put("time", selectedTime);
        }
        updates.put("category", selectedCategory);
        updates.put("priority", selectedPriority);
        if (categoryColor != null) {
            updates.put("categoryColor", categoryColor);
        }
        if (categoryIcon != null) {
            updates.put("categoryIcon", categoryIcon);
        }
        updates.put("completed", completed);

        taskRef.updateChildren(updates).addOnSuccessListener(unused -> {
            if (finishAfterSave) {
                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            if (finishAfterSave) {
                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void deleteTask() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    DatabaseReference taskRef = FirebaseDatabase.getInstance()
                            .getReference("Users")
                            .child(user.getUid())
                            .child("Tasks")
                            .child(taskId);

                    taskRef.removeValue()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
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
