package com.example.todolist.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.todolist.R;
import com.example.todolist.activity.home.DialogAddPriority;
import com.example.todolist.activity.home.ItemAdd_Category;
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

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int selectedHour;
    private int selectedMinute;

    private boolean[] subtaskDone = new boolean[]{true, true, false, false};

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
            String shortId = taskId.length() > 6 ? taskId.substring(0, 6) : taskId;
            binding.txtTaskIdBadge.setText("ID #" + shortId.toUpperCase(Locale.ENGLISH));
        }

        // Category
        binding.txtCategory.setText(selectedCategory);
        if (categoryColor != null && !categoryColor.trim().isEmpty()) {
            try {
                int c = Color.parseColor(categoryColor.trim());
                binding.dotCategory.setTextColor(c);
                binding.txtCategory.setTextColor(c);
            } catch (Exception ignored) {}
        }

        // Priority
        updatePriorityUI(selectedPriority);

        // Date & Time
        if (selectedDate != null && !selectedDate.isEmpty()) {
            binding.txtDueDate.setText(formatDisplayDate(selectedDate));
        } else {
            binding.txtDueDate.setText("Today, " + new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).format(new Date()));
        }

        if (selectedTime != null && !selectedTime.isEmpty()) {
            binding.txtTaskTime.setText(formatDisplayTime(selectedTime));
        }

        // Completion status
        updateCompletionUI();

        // Subtasks initial setup
        updateSubtasksState();
    }

    private void updatePriorityUI(String priority) {
        selectedPriority = priority;
        String displayPrio;
        int color;
        int bgTint;

        if (priority.equalsIgnoreCase("High") || priority.equals("1")) {
            displayPrio = "High Priority";
            color = Color.parseColor("#F43F5E");
            bgTint = Color.parseColor("#381A24");
        } else if (priority.equalsIgnoreCase("Low") || priority.equals("3")) {
            displayPrio = "Low Priority";
            color = Color.parseColor("#94A3B8");
            bgTint = Color.parseColor("#1F2430");
        } else {
            displayPrio = "Medium Priority";
            color = Color.parseColor("#C084FC");
            bgTint = Color.parseColor("#291C3D");
        }

        binding.txtTaskPriority.setText(displayPrio);
        binding.txtTaskPriority.setTextColor(color);
        binding.btnCategory.setBackgroundTintList(ColorStateList.valueOf(bgTint));
    }

    private void updateCompletionUI() {
        if (completed) {
            binding.btnRadioCompleted.setBackgroundResource(R.drawable.bg_timeline_checkbox_checked);
            binding.imgCheckCompleted.setColorFilter(Color.WHITE);
            binding.edtTaskName.setPaintFlags(binding.edtTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            binding.btnRadioCompleted.setBackgroundResource(R.drawable.bg_timeline_checkbox);
            binding.imgCheckCompleted.setColorFilter(Color.parseColor("#4B4D5E"));
            binding.edtTaskName.setPaintFlags(binding.edtTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    private void setupListeners() {
        binding.btnClose.setOnClickListener(v -> finish());

        // Header Menu / More Options
        binding.btnMoreOptions.setOnClickListener(v ->
                Toast.makeText(this, "Task options", Toast.LENGTH_SHORT).show()
        );

        // Edit Title Button
        binding.btnEditTitle.setOnClickListener(v -> {
            binding.edtTaskName.requestFocus();
            binding.edtTaskName.selectAll();
        });

        // Share Button
        binding.btnShare.setOnClickListener(v -> {
            String title = binding.edtTaskName.getText().toString().trim();
            String desc = binding.edtDescription.getText().toString().trim();
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, title + "\n\n" + desc);
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Task"));
        });

        // Completion Checkbox
        binding.btnRadioCompleted.setOnClickListener(v -> {
            completed = !completed;
            updateCompletionUI();
            syncCompletionToFirebase(completed);
        });

        // Copy Description Button
        binding.btnCopyDesc.setOnClickListener(v -> {
            String desc = binding.edtDescription.getText().toString().trim();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Task Description", desc);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Description copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Priority Picker
        binding.btnCategory.setOnClickListener(v -> {
            DialogAddPriority prioritySheet = new DialogAddPriority();
            prioritySheet.setOnPrioritySelectedListener(selectedPriorityVal -> {
                String prioText;
                if (selectedPriorityVal == 1) prioText = "High";
                else if (selectedPriorityVal == 3) prioText = "Low";
                else prioText = "Medium";
                updatePriorityUI(prioText);
                updateTaskInFirebase(false);
            });
            prioritySheet.show(getSupportFragmentManager(), "DialogAddPriority");
        });

        // Category Picker
        binding.layoutCategoryChip.setOnClickListener(v -> {
            ItemAdd_Category sheet = new ItemAdd_Category();
            sheet.show(getSupportFragmentManager(), "CategoryBottomSheet");
        });

        getSupportFragmentManager().setFragmentResultListener("category_selected", this, (requestKey, bundle) -> {
            String categoryName = bundle.getString("selectedCategory");
            String color = bundle.getString("selectedCategoryColor");
            int icon = bundle.getInt("selectedCategoryIcon", 0);

            if (categoryName != null && !categoryName.isEmpty()) {
                selectedCategory = categoryName;
                binding.txtCategory.setText(selectedCategory);
            }
            if (color != null && !color.isEmpty()) {
                categoryColor = color;
                try {
                    int c = Color.parseColor(categoryColor);
                    binding.dotCategory.setTextColor(c);
                    binding.txtCategory.setTextColor(c);
                } catch (Exception ignored) {}
            }
            if (icon != 0) {
                categoryIcon = String.valueOf(icon);
            }
            updateTaskInFirebase(false);
        });

        // Date Picker
        binding.cardDueDate.setOnClickListener(v -> openCalendarPicker());

        // Time Picker
        binding.cardTargetTime.setOnClickListener(v -> openTimePicker());

        // Subtasks checkboxes
        binding.chkSubtask1.setOnClickListener(v -> toggleSubtask(0));
        binding.layoutSubtask1.setOnClickListener(v -> toggleSubtask(0));

        binding.chkSubtask2.setOnClickListener(v -> toggleSubtask(1));
        binding.layoutSubtask2.setOnClickListener(v -> toggleSubtask(1));

        binding.chkSubtask3.setOnClickListener(v -> toggleSubtask(2));
        binding.layoutSubtask3.setOnClickListener(v -> toggleSubtask(2));

        binding.chkSubtask4.setOnClickListener(v -> toggleSubtask(3));
        binding.layoutSubtask4.setOnClickListener(v -> toggleSubtask(3));

        // Add Subtask
        binding.btnAddSubTask.setOnClickListener(v -> showAddSubtaskDialog());

        // Add Team Member
        binding.btnAddMember.setOnClickListener(v ->
                Toast.makeText(this, "Syncing team members...", Toast.LENGTH_SHORT).show()
        );

        // Delete Task
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        // Primary Action: Start Focus Session / Save Task
        binding.btnEditTask.setOnClickListener(v -> {
            updateTaskInFirebase(true);
        });
    }

    private void toggleSubtask(int index) {
        subtaskDone[index] = !subtaskDone[index];
        updateSubtasksState();
    }

    private void updateSubtasksState() {
        int completedCount = 0;
        TextView[] texts = new TextView[]{binding.txtSubtask1, binding.txtSubtask2, binding.txtSubtask3, binding.txtSubtask4};
        FrameLayout[] checks = new FrameLayout[]{binding.chkSubtask1, binding.chkSubtask2, binding.chkSubtask3, binding.chkSubtask4};
        ImageView[] checkImgs = new ImageView[]{binding.imgSubtask1, binding.imgSubtask2, binding.imgSubtask3, binding.imgSubtask4};

        for (int i = 0; i < 4; i++) {
            if (subtaskDone[i]) {
                completedCount++;
                checks[i].setBackgroundResource(R.drawable.bg_timeline_checkbox_checked);
                checkImgs[i].setColorFilter(Color.WHITE);
                texts[i].setTextColor(Color.parseColor("#71717A"));
                texts[i].setPaintFlags(texts[i].getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                checks[i].setBackgroundResource(R.drawable.bg_timeline_checkbox);
                checkImgs[i].setColorFilter(Color.parseColor("#4B4D5E"));
                texts[i].setTextColor(Color.WHITE);
                texts[i].setPaintFlags(texts[i].getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }

        binding.txtSubtasksCount.setText(completedCount + " of 4 Completed");
        binding.progressBarSubtasks.setProgress((completedCount * 100) / 4);
    }

    private void showAddSubtaskDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter subtask description...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Add Subtask")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String sub = input.getText().toString().trim();
                    if (!sub.isEmpty()) {
                        binding.txtSubtask4.setText(sub);
                        subtaskDone[3] = false;
                        updateSubtasksState();
                        Toast.makeText(this, "Subtask added", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openCalendarPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;

            Calendar pickedCal = Calendar.getInstance();
            pickedCal.set(year, month, day);

            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            binding.txtDueDate.setText(formatDisplayDate(selectedDate));
            updateTaskInFirebase(false);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour, minute) -> {
            selectedHour = hour;
            selectedMinute = minute;

            String amPm = hour >= 12 ? "PM" : "AM";
            int hourIn12 = hour % 12;
            if (hourIn12 == 0) hourIn12 = 12;

            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12, minute, amPm);
            binding.txtTaskTime.setText(selectedTime + " (90 mins)");
            updateTaskInFirebase(false);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private String formatDisplayDate(String dateStr) {
        try {
            SimpleDateFormat parseFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date d = parseFormat.parse(dateStr);
            if (d != null) {
                return new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH).format(d);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    private String formatDisplayTime(String timeStr) {
        if (!timeStr.contains("mins") && !timeStr.contains("–")) {
            return timeStr + " (90 mins)";
        }
        return timeStr;
    }

    private void syncCompletionToFirebase(boolean isCompleted) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && taskId != null && !taskId.trim().isEmpty()) {
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(user.getUid())
                    .child("Tasks")
                    .child(taskId)
                    .child("completed")
                    .setValue(isCompleted);
        }
    }

    private void updateTaskInFirebase(boolean finishAfterSave) {
        String taskName = binding.edtTaskName.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();

        if (taskName.isEmpty()) {
            binding.edtTaskName.setError("Task name cannot be empty");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Focus session starting...", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
