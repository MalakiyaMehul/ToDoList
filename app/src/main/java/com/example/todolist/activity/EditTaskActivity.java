package com.example.todolist.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.Dialog.SubTaskBottomSheet;
import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityCategoryBottomSheetBinding;
import com.example.todolist.databinding.ActivityEditTaskBinding;
import com.example.todolist.model.CategoryModel;
import com.example.todolist.model.TaskModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private String subTask = "";

    private String customSlot1Category = "Work";
    private int customSlot1Icon = R.drawable.ic_briefcase_custom;
    private String customSlot1Color = "#7C3AED";

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
            customSlot1Category = cat;
        }

        categoryColor = getIntent().getStringExtra("categoryColor");
        categoryIcon = getIntent().getStringExtra("categoryIcon");

        String prio = getIntent().getStringExtra("priority");
        if (prio != null && !prio.trim().isEmpty()) {
            selectedPriority = prio;
        }

        completed = getIntent().getBooleanExtra("completed", false);
        subTask = getIntent().getStringExtra("subTask");
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

        // Subtasks UI
        renderSubTasks();
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

        if (!category.equalsIgnoreCase("Personal") &&
            !category.equalsIgnoreCase("Design") &&
            !category.equalsIgnoreCase("Health")) {
            customSlot1Category = category;
            binding.txtTitleWork.setText(customSlot1Category);
            selectSlot(1);
        } else if (category.equalsIgnoreCase("Personal")) {
            selectSlot(2);
        } else if (category.equalsIgnoreCase("Design")) {
            selectSlot(3);
        } else if (category.equalsIgnoreCase("Health")) {
            selectSlot(4);
        }
    }

    private void selectSlot(int slot) {
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

        binding.cardCategoryMore.setBackgroundResource(R.drawable.bg_category_card_unselected);
        binding.imgMoreCheck.setVisibility(View.GONE);
        binding.txtSubMore.setTextColor(Color.parseColor("#8E93A0"));

        switch (slot) {
            case 1:
                selectedCategory = customSlot1Category;
                categoryColor = customSlot1Color;
                categoryIcon = String.valueOf(customSlot1Icon);
                binding.cardCategoryWork.setBackgroundResource(R.drawable.bg_category_card_selected);
                binding.imgWorkCheck.setVisibility(View.VISIBLE);
                binding.txtSubWork.setTextColor(Color.parseColor("#E0E7FF"));
                break;
            case 2:
                selectedCategory = "Personal";
                categoryColor = "#2563EB";
                categoryIcon = String.valueOf(R.drawable.sport);
                binding.cardCategoryPersonal.setBackgroundResource(R.drawable.bg_category_card_selected);
                binding.imgPersonalCheck.setVisibility(View.VISIBLE);
                binding.txtSubPersonal.setTextColor(Color.parseColor("#E0E7FF"));
                break;
            case 3:
                selectedCategory = "Design";
                categoryColor = "#A855F7";
                categoryIcon = String.valueOf(R.drawable.design);
                binding.cardCategoryDesign.setBackgroundResource(R.drawable.bg_category_card_selected);
                binding.imgDesignCheck.setVisibility(View.VISIBLE);
                binding.txtSubDesign.setTextColor(Color.parseColor("#E0E7FF"));
                break;
            case 4:
                selectedCategory = "Health";
                categoryColor = "#10B981";
                categoryIcon = String.valueOf(R.drawable.heart);
                binding.cardCategoryHealth.setBackgroundResource(R.drawable.bg_category_card_selected);
                binding.imgHealthCheck.setVisibility(View.VISIBLE);
                binding.txtSubHealth.setTextColor(Color.parseColor("#E0E7FF"));
                break;
        }

        binding.txtSelectedCategoryLabel.setText("Selected: " + selectedCategory);
    }

    private void selectCustomCategory(CategoryModel category) {
        customSlot1Category = category.getCategoryName();
        customSlot1Icon = category.getIcon();
        customSlot1Color = category.getColor();

        binding.txtTitleWork.setText(customSlot1Category);

        selectSlot(1);
    }

    private void showCategoryBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        ActivityCategoryBottomSheetBinding sheetBinding = ActivityCategoryBottomSheetBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(sheetBinding.getRoot());

        if (bottomSheetDialog.getWindow() != null) {
            View bottomSheetInternal = bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        }

        List<CategoryModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryModel("Work", R.drawable.briefcase, "#FF9680"));
        categoryList.add(new CategoryModel("Personal", R.drawable.sport, "#06B6D4"));
        categoryList.add(new CategoryModel("Design", R.drawable.design, "#80FFD9"));
        categoryList.add(new CategoryModel("Health", R.drawable.heart, "#80FFA3"));
        categoryList.add(new CategoryModel("Education", R.drawable.university, "#F59E0B"));
        categoryList.add(new CategoryModel("Finance", R.drawable.tag, "#10B981"));
        categoryList.add(new CategoryModel("Shopping", R.drawable.checklist_rafiki, "#EC4899"));
        categoryList.add(new CategoryModel("Fitness", R.drawable.sport, "#EF4444"));
        categoryList.add(new CategoryModel("Home", R.drawable.home, "#14B8A6"));
        categoryList.add(new CategoryModel("Music", R.drawable.music, "#A855F7"));
        categoryList.add(new CategoryModel("Camera", R.drawable.camera, "#3B82F6"));

        sheetBinding.recyclerCategories.setLayoutManager(new GridLayoutManager(this, 3));
        CategoryGridAdapter adapter = new CategoryGridAdapter(categoryList, category -> {
            bottomSheetDialog.dismiss();
            selectCustomCategory(category);
            updateTaskInSupabase(false);
        });
        sheetBinding.recyclerCategories.setAdapter(adapter);

        bottomSheetDialog.show();
    }

    private static class CategoryGridAdapter extends RecyclerView.Adapter<CategoryGridAdapter.ViewHolder> {
        private final List<CategoryModel> categories;
        private final OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onCategoryClick(CategoryModel category);
        }

        CategoryGridAdapter(List<CategoryModel> categories, OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryModel item = categories.get(position);
            holder.txtName.setText(item.getCategoryName());
            holder.imgIcon.setImageResource(item.getIcon());
            holder.imgIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.white)));
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(item));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName;
            ImageView imgIcon;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtName = itemView.findViewById(R.id.txtCategoryName);
                imgIcon = itemView.findViewById(R.id.txtCategoryIcon);
            }
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                updateTaskInSupabase(false);
                finish();
            }
        });

        binding.btnClearTitle.setOnClickListener(v -> {
            binding.edtTaskName.setText("");
            binding.edtTaskName.requestFocus();
        });

        binding.btnTopSave.setOnClickListener(v -> updateTaskInSupabase(true));
        binding.btnEditTask.setOnClickListener(v -> updateTaskInSupabase(true));

        binding.btnHeaderDelete.setOnClickListener(v -> deleteTask());
        binding.btnDeleteTask.setOnClickListener(v -> deleteTask());

        binding.cardCategoryWork.setOnClickListener(v -> {
            selectSlot(1);
            updateTaskInSupabase(false);
        });

        binding.cardCategoryPersonal.setOnClickListener(v -> {
            selectSlot(2);
            updateTaskInSupabase(false);
        });

        binding.cardCategoryDesign.setOnClickListener(v -> {
            selectSlot(3);
            updateTaskInSupabase(false);
        });

        binding.cardCategoryHealth.setOnClickListener(v -> {
            selectSlot(4);
            updateTaskInSupabase(false);
        });

        binding.cardCategoryMore.setOnClickListener(v -> showCategoryBottomSheet());

        binding.chipPriorityLow.setOnClickListener(v -> {
            updatePriorityUI("Low");
            updateTaskInSupabase(false);
        });

        binding.chipPriorityMedium.setOnClickListener(v -> {
            updatePriorityUI("Medium");
            updateTaskInSupabase(false);
        });

        binding.chipPriorityHigh.setOnClickListener(v -> {
            updatePriorityUI("High");
            updateTaskInSupabase(false);
        });

        // Subtasks Card & Edit Link
        binding.btnEditSubTasks.setOnClickListener(v -> openSubTaskBottomSheet());
        binding.cardSubTasks.setOnClickListener(v -> openSubTaskBottomSheet());

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
    }

    private void openSubTaskBottomSheet() {
        TaskModel model = new TaskModel();
        model.setTaskId(taskId);
        model.setTaskName(binding.edtTaskName.getText().toString().trim());
        model.setSubTask(subTask);

        SubTaskBottomSheet.show(this, model, updatedJson -> {
            subTask = updatedJson;
            renderSubTasks();
        });
    }

    private void renderSubTasks() {
        if (binding == null) return;
        binding.layoutEditSubTasksContainer.removeAllViews();

        if (subTask == null || subTask.trim().isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No subtasks added yet. Tap Edit to add.");
            emptyTv.setTextColor(Color.parseColor("#8E93A0"));
            emptyTv.setTextSize(12);
            binding.layoutEditSubTasksContainer.addView(emptyTv);
            return;
        }

        try {
            JSONArray array = new JSONArray(subTask.trim());
            if (array.length() == 0) {
                TextView emptyTv = new TextView(this);
                emptyTv.setText("No subtasks added yet. Tap Edit to add.");
                emptyTv.setTextColor(Color.parseColor("#8E93A0"));
                emptyTv.setTextSize(12);
                binding.layoutEditSubTasksContainer.addView(emptyTv);
                return;
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "");
                boolean isDone = obj.optBoolean("done", false);

                if (!title.isEmpty()) {
                    View itemView = getLayoutInflater().inflate(R.layout.item_sub_task, binding.layoutEditSubTasksContainer, false);
                    RadioButton radio = itemView.findViewById(R.id.radioSubTaskCompleted);
                    TextView txtTitle = itemView.findViewById(R.id.txtSubTaskTitle);
                    ImageView btnDelete = itemView.findViewById(R.id.btnDeleteSubTask);

                    txtTitle.setText(title);
                    radio.setChecked(isDone);

                    if (isDone) {
                        txtTitle.setPaintFlags(txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        txtTitle.setAlpha(0.6f);
                    } else {
                        txtTitle.setPaintFlags(txtTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        txtTitle.setAlpha(1.0f);
                    }

                    radio.setOnClickListener(v -> {
                        try {
                            obj.put("done", !isDone);
                            subTask = array.toString();
                            renderSubTasks();

                            if (taskId != null && !taskId.isEmpty()) {
                                String token = SharedPrefManager.getInstance(EditTaskActivity.this).getString("access_token");
                                if (token.isEmpty()) token = SupabaseAuth.getAccessToken(EditTaskActivity.this);
                                new SupabaseRepository().updateTaskSubTask(taskId, token, subTask, null);
                            }
                        } catch (Exception ignored) {}
                    });

                    btnDelete.setOnClickListener(v -> openSubTaskBottomSheet());
                    binding.layoutEditSubTasksContainer.addView(itemView);
                }
            }
        } catch (Exception e) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(subTask);
            emptyTv.setTextColor(Color.WHITE);
            emptyTv.setTextSize(13);
            binding.layoutEditSubTasksContainer.addView(emptyTv);
        }
    }

    private void openCalendarPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            setupDateUI(selectedDate);
            updateTaskInSupabase(false);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour, minute) -> {
            Calendar pickedCal = Calendar.getInstance();
            if (selectedDate != null && !selectedDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    Date d = sdf.parse(selectedDate);
                    if (d != null) pickedCal.setTime(d);
                } catch (Exception ignored) {}
            }
            pickedCal.set(Calendar.HOUR_OF_DAY, hour);
            pickedCal.set(Calendar.MINUTE, minute);
            pickedCal.set(Calendar.SECOND, 0);

            if (pickedCal.before(now)) {
                Toast.makeText(this, "Cannot select a past time", Toast.LENGTH_SHORT).show();
                return;
            }

            String amPm = hour >= 12 ? "PM" : "AM";
            int hourIn12 = hour % 12;
            if (hourIn12 == 0) hourIn12 = 12;

            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourIn12, minute, amPm);
            binding.txtTaskTime.setText(selectedTime);
            updateTaskInSupabase(false);
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
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

    private void updateTaskInSupabase(boolean finishAfterSave) {
        String taskName = binding.edtTaskName.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();

        if (taskName.isEmpty()) {
            if (finishAfterSave) {
                binding.edtTaskName.setError("Task title cannot be empty");
            }
            return;
        }

        if (taskId == null || taskId.trim().isEmpty()) {
            if (finishAfterSave) {
                Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String accessToken = SharedPrefManager.getInstance(this).getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(this);
        }

        new SupabaseRepository().updateTask(
                taskId,
                accessToken,
                taskName,
                description,
                selectedDate,
                selectedTime,
                selectedCategory,
                categoryColor,
                categoryIcon,
                selectedPriority,
                completed,
                new SupabaseRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String taskId) {
                        if (finishAfterSave) {
                            Toast.makeText(EditTaskActivity.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (finishAfterSave) {
                            Toast.makeText(EditTaskActivity.this, "Update failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void deleteTask() {
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Delete", (dialog, which) -> {
                    String accessToken = SharedPrefManager.getInstance(this).getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(this);
        }
                    new SupabaseRepository().deleteTask(taskId, accessToken, new SupabaseRepository.TaskCallback() {
                        @Override
                        public void onSuccess(String taskId) {
                            Toast.makeText(EditTaskActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(EditTaskActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
                        }
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
