package com.example.todolist.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityCategoryBottomSheetBinding;
import com.example.todolist.databinding.ActivityCreateTaskBinding;
import com.example.todolist.model.CategoryModel;
import com.example.todolist.model.TaskModel;
import com.example.todolist.receiver.TaskReminderScheduler;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateTaskActivity extends BaseActivity {

    private ActivityCreateTaskBinding binding;

    private String selectedCategory = "Work";
    private String selectedPriority = "High";
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedCategoryColor = "#8B5CF6";
    private String selectedCategoryIcon = String.valueOf(R.drawable.briefcase);
    private int selectedAlertOffsetMinutes = 15;

    private String customSlot1Category = "Work";
    private int customSlot1Icon = R.drawable.ic_briefcase_custom;
    private String customSlot1Color = "#FF9680";

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

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
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

        setupFieldNavigation();

        // Pulse Reminder Switch & Offset Pills
        binding.switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.txtReminderTitle.setText(isChecked ? "Pulse Reminder: Enabled" : "Pulse Reminder: Disabled");
        });

        binding.pillOffset15m.setOnClickListener(v -> updateAlertOffsetUI(15));
        binding.pillOffset30m.setOnClickListener(v -> updateAlertOffsetUI(30));
        binding.pillOffset1h.setOnClickListener(v -> updateAlertOffsetUI(60));

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
        binding.switchReminder.setThumbTintList(thumbStates);
        binding.switchReminder.setTrackTintList(trackStates);
    }

    private void updateAlertOffsetUI(int minutes) {
        selectedAlertOffsetMinutes = minutes;

        binding.pillOffset15m.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset15m.setTextColor(ContextCompat.getColor(this, R.color.text_muted));

        binding.pillOffset30m.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset30m.setTextColor(ContextCompat.getColor(this, R.color.text_muted));

        binding.pillOffset1h.setBackgroundResource(R.drawable.bg_alert_offset_unselected);
        binding.pillOffset1h.setTextColor(ContextCompat.getColor(this, R.color.text_muted));

        if (minutes == 15) {
            binding.pillOffset15m.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset15m.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (minutes == 30) {
            binding.pillOffset30m.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset30m.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else if (minutes == 60) {
            binding.pillOffset1h.setBackgroundResource(R.drawable.bg_alert_offset_selected);
            binding.pillOffset1h.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void setupFieldNavigation() {
        binding.edtTaskTitle.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        binding.edtTaskTitle.setSingleLine(true);
        binding.edtTaskTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.edtTaskDetails.requestFocus();
                return true;
            }
            return false;
        });

        binding.edtTaskTitle.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                binding.edtTaskDetails.requestFocus();
                return true;
            }
            return false;
        });

        binding.edtTaskDetails.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.edtTaskDetails.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) {
                createTask();
                return true;
            }
            return false;
        });

        binding.edtTaskDetails.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP && !event.isShiftPressed()) {
                createTask();
                return true;
            }
            return false;
        });
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
        Calendar now = Calendar.getInstance();
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
        datePickerDialog.getDatePicker().setMinDate(now.getTimeInMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    Calendar tempCal = (Calendar) calendar.clone();
                    tempCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    tempCal.set(Calendar.MINUTE, minute);
                    tempCal.set(Calendar.SECOND, 0);

                    if (tempCal.before(now)) {
                        Toast.makeText(this, "Cannot select a past time", Toast.LENGTH_SHORT).show();
                        return;
                    }

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
        binding.btnCatWork.setOnClickListener(v -> selectSlot(1));
        binding.btnCatPersonal.setOnClickListener(v -> selectSlot(2));
        binding.btnCatDesign.setOnClickListener(v -> selectSlot(3));
        binding.btnCatHealth.setOnClickListener(v -> selectSlot(4));
        binding.btnCatMore.setOnClickListener(v -> showCategoryBottomSheet());
    }

    private void selectCustomCategory(CategoryModel category) {
        customSlot1Category = category.getCategoryName();
        customSlot1Icon = category.getIcon();
        customSlot1Color = category.getColor();

        binding.txtCatWork.setText(customSlot1Category);
        binding.iconCatWork.setImageResource(customSlot1Icon);

        selectSlot(1);
    }

    private void selectSlot(int slot) {
        updateCategoryButton(binding.btnCatWork, binding.iconCatWork, binding.txtCatWork, slot == 1);
        updateCategoryButton(binding.btnCatPersonal, binding.iconCatPersonal, binding.txtCatPersonal, slot == 2);
        updateCategoryButton(binding.btnCatDesign, binding.iconCatDesign, binding.txtCatDesign, slot == 3);
        updateCategoryButton(binding.btnCatHealth, binding.iconCatHealth, binding.txtCatHealth, slot == 4);
        updateCategoryButton(binding.btnCatMore, binding.iconCatMore, binding.txtCatMore, false);

        switch (slot) {
            case 1:
                selectedCategory = customSlot1Category;
                selectedCategoryColor = customSlot1Color;
                selectedCategoryIcon = String.valueOf(customSlot1Icon);
                break;
            case 2:
                selectedCategory = "Personal";
                selectedCategoryColor = "#06B6D4";
                selectedCategoryIcon = String.valueOf(R.drawable.sport);
                break;
            case 3:
                selectedCategory = "Design";
                selectedCategoryColor = "#80FFD9";
                selectedCategoryIcon = String.valueOf(R.drawable.design);
                break;
            case 4:
                selectedCategory = "Health";
                selectedCategoryColor = "#80FFA3";
                selectedCategoryIcon = String.valueOf(R.drawable.heart);
                break;
        }
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

        Calendar now = Calendar.getInstance();
        if (calendar.before(now)) {
            Toast.makeText(this, "Task date and time cannot be in the past", Toast.LENGTH_SHORT).show();
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
            username = getSharedPreferences("UserProfile", Context.MODE_PRIVATE).getString("user_name", "");
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
                        TaskModel model = new TaskModel(
                                taskId,
                                title,
                                details,
                                selectedDate,
                                selectedTime,
                                selectedCategory,
                                selectedCategoryColor,
                                selectedCategoryIcon,
                                selectedPriority,
                                false
                        );
                        TaskReminderScheduler.scheduleTaskReminder(
                                CreateTaskActivity.this,
                                model,
                                selectedAlertOffsetMinutes,
                                binding.switchReminder.isChecked()
                        );
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

