package com.example.todolist.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityCreateTaskBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateTaskActivity extends BaseActivity {

    private ActivityCreateTaskBinding binding;

    private String selectedCategory = "Work";
    private String selectedPriority = "High";
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedCategoryColor = "#8B5CF6";
    private String selectedCategoryIcon = String.valueOf(R.drawable.briefcase);

    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreateTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        calendar = Calendar.getInstance();
        initDefaultDateTime();
        setupListeners();
        setupCategorySelection();
        setupPrioritySelection();
    }

    private void initDefaultDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        selectedDate = dateFormat.format(calendar.getTime());
        selectedTime = timeFormat.format(calendar.getTime());

        // In the design, default display matches screenshot:
        binding.txtDueDateValue.setText("Today, " + new SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.getTime()));
        binding.txtTargetTimeValue.setText("02:30 PM");
        selectedTime = "02:30 PM";
    }

    private void setupListeners() {
        // Navigation / Close
        binding.btnBack.setOnClickListener(v -> finish());

        // Schedule Pickers
        binding.cardDueDate.setOnClickListener(v -> showDatePicker());
        binding.cardTargetTime.setOnClickListener(v -> showTimePicker());

        // Bottom action row in details
        binding.btnAttach.setOnClickListener(v -> {
            insertTextAtCursor("[attachment](https://)");
            Toast.makeText(this, "Attachment link inserted", Toast.LENGTH_SHORT).show();
        });

        binding.btnChecklist.setOnClickListener(v -> {
            insertTextAtCursor("\n- [ ] ");
            Toast.makeText(this, "Checklist item added", Toast.LENGTH_SHORT).show();
        });

        binding.btnMention.setOnClickListener(v -> {
            insertTextAtCursor("@");
        });


        // Submit Button
        binding.btnCreateTask.setOnClickListener(v -> createTask());

        // Setup switch tinting programmatically for reliability across Android versions
        ColorStateList thumbStates = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        ContextCompat.getColor(this, R.color.brand_primary_dark),
                        ContextCompat.getColor(this, R.color.white)
                }
        );
        ColorStateList trackStates = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        ContextCompat.getColor(this, R.color.brand_primary_light),
                        ContextCompat.getColor(this, R.color.drag_handle_color)
                }
        );
        binding.switchPulseReminder.setThumbTintList(thumbStates);
        binding.switchPulseReminder.setTrackTintList(trackStates);
    }

    private void insertTextAtCursor(String textToInsert) {
        int start = Math.max(binding.edtTaskDetails.getSelectionStart(), 0);
        int end = Math.max(binding.edtTaskDetails.getSelectionEnd(), 0);
        binding.edtTaskDetails.getText().replace(
                Math.min(start, end),
                Math.max(start, end),
                textToInsert,
                0,
                textToInsert.length()
        );
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    SimpleDateFormat storageFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDate = storageFormat.format(calendar.getTime());
                    binding.txtDueDateValue.setText(displayFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    SimpleDateFormat displayFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    selectedTime = displayFormat.format(calendar.getTime());
                    binding.txtTargetTimeValue.setText(selectedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void setupCategorySelection() {
        binding.btnCatWork.setOnClickListener(v -> selectCategory("Work"));
        binding.btnCatPersonal.setOnClickListener(v -> selectCategory("Personal"));
        binding.btnCatDesign.setOnClickListener(v -> selectCategory("Design"));
        binding.btnCatHealth.setOnClickListener(v -> selectCategory("Health"));
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        updateCategoryButton(binding.btnCatWork, binding.iconCatWork, binding.txtCatWork, "Work".equals(category));
        updateCategoryButton(binding.btnCatPersonal, binding.iconCatPersonal, binding.txtCatPersonal, "Personal".equals(category));
        updateCategoryButton(binding.btnCatDesign, binding.iconCatDesign, binding.txtCatDesign, "Design".equals(category));
        updateCategoryButton(binding.btnCatHealth, binding.iconCatHealth, binding.txtCatHealth, "Health".equals(category));
        
        switch (category) {
            case "Work":
                selectedCategoryColor = "#FF9680";
                selectedCategoryIcon = String.valueOf(R.drawable.briefcase);
                break;
            case "Personal":
                selectedCategoryColor = "#06B6D4";
                selectedCategoryIcon = String.valueOf(R.drawable.sport);
                break;
            case "Design":
                selectedCategoryColor = "#80FFD9";
                selectedCategoryIcon = String.valueOf(R.drawable.design);
                break;
            case "Health":
                selectedCategoryColor = "#80FFA3";
                selectedCategoryIcon = String.valueOf(R.drawable.heart);
                break;
        }
    }

    private void updateCategoryButton(View container, ImageView icon, TextView text, boolean isSelected) {
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        int mutedColor = ContextCompat.getColor(this, R.color.text_muted);

        if (isSelected) {
            container.setBackgroundResource(R.drawable.bg_category_selected);
            icon.setImageTintList(ColorStateList.valueOf(whiteColor));
            text.setTextColor(whiteColor);
            text.setTypeface(null, Typeface.BOLD);
        } else {
            container.setBackgroundResource(R.drawable.bg_category_unselected);
            icon.setImageTintList(ColorStateList.valueOf(mutedColor));
            text.setTextColor(mutedColor);
            text.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void setupPrioritySelection() {
        binding.btnPriorityLow.setOnClickListener(v -> selectPriority("Low"));
        binding.btnPriorityMedium.setOnClickListener(v -> selectPriority("Medium"));
        binding.btnPriorityHigh.setOnClickListener(v -> selectPriority("High"));
    }

    private void selectPriority(String priority) {
        selectedPriority = priority;

        int whiteColor = ContextCompat.getColor(this, R.color.white);
        int mutedColor = ContextCompat.getColor(this, R.color.text_muted);
        int dotLowColor = ContextCompat.getColor(this, R.color.dot_low);
        int dotMediumColor = ContextCompat.getColor(this, R.color.dot_medium);

        // Reset all
        binding.btnPriorityLow.setBackground(null);
        binding.dotPriorityLow.setTextColor(dotLowColor);
        binding.txtPriorityLow.setTextColor(mutedColor);

        binding.btnPriorityMedium.setBackground(null);
        binding.dotPriorityMedium.setTextColor(dotMediumColor);
        binding.txtPriorityMedium.setTextColor(mutedColor);

        binding.btnPriorityHigh.setBackground(null);
        binding.dotPriorityHigh.setTextColor(whiteColor);
        binding.txtPriorityHigh.setTextColor(mutedColor);

        if ("High".equals(priority)) {
            binding.btnPriorityHigh.setBackgroundResource(R.drawable.bg_priority_high_selected);
            binding.dotPriorityHigh.setTextColor(whiteColor);
            binding.txtPriorityHigh.setTextColor(whiteColor);
            binding.txtPriorityHigh.setTypeface(null, Typeface.BOLD);
        } else if ("Medium".equals(priority)) {
            binding.btnPriorityMedium.setBackgroundResource(R.drawable.bg_priority_medium_selected);
            binding.dotPriorityMedium.setTextColor(whiteColor);
            binding.txtPriorityMedium.setTextColor(whiteColor);
            binding.txtPriorityMedium.setTypeface(null, Typeface.BOLD);
        } else if ("Low".equals(priority)) {
            binding.btnPriorityLow.setBackgroundResource(R.drawable.bg_priority_low_selected);
            binding.dotPriorityLow.setTextColor(whiteColor);
            binding.txtPriorityLow.setTextColor(whiteColor);
            binding.txtPriorityLow.setTypeface(null, Typeface.BOLD);
        }
    }

    private void createTask() {
        String title = binding.edtTaskTitle.getText().toString().trim();
        String details = binding.edtTaskDetails.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            binding.edtTaskTitle.setError("Task title is required");
            binding.edtTaskTitle.requestFocus();
            return;
        }

        SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
        String userId = prefManager.getString("user_id");
        if (userId.isEmpty()) {
            userId = SupabaseAuth.getUserId(this);
        }
        String accessToken = prefManager.getString("access_token");
        if (accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(this);
        }

        if (userId == null || userId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = prefManager.getString("username");
        if (username.isEmpty()) {
            username = SupabaseAuth.getUsername(this);
        }
        if (username.isEmpty()) {
            username = getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE).getString("user_name", "");
        }

        binding.btnCreateTask.setEnabled(false);

        SupabaseRepository repository = new SupabaseRepository();
        String finalUserId = userId;
        repository.saveTask(
                finalUserId,
                username,
                accessToken,
                title,
                details,
                selectedDate,
                selectedTime,
                selectedCategory,
                selectedCategoryColor,
                selectedCategoryIcon,
                selectedPriority,
                false,
                new SupabaseRepository.TaskCallback() {
                    @Override
                    public void onSuccess(String taskId) {
                        Toast.makeText(CreateTaskActivity.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        binding.btnCreateTask.setEnabled(true);
                        Toast.makeText(CreateTaskActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}

