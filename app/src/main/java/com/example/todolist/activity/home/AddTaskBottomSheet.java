package com.example.todolist.activity.home;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityAddTaskBottomSheetBinding;
import com.example.todolist.model.CategoryModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {
    private ActivityAddTaskBottomSheetBinding binding;
    private CategoryModel categoryModel;
    private String selectedDate = "";
    private String selectedTime = "";
    private String selectedPriority = "";
    private String selectedCategory = "";
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int selectedHour;
    private int selectedMinute;
    private int priority = 1;
    private String selectedCategoryColor = "";
    private String selectedCategoryIcon = "";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityAddTaskBottomSheetBinding.inflate(LayoutInflater.from(requireContext()));
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(binding.getRoot());
        binding.Timer.setOnClickListener(v -> openCalendar());
        binding.Category.setOnClickListener(v -> {
            ItemAdd_Category sheet = new ItemAdd_Category();
            sheet.show(requireActivity().getSupportFragmentManager(), "CategoryBottomSheet");
        });
        binding.Priority.setOnClickListener(v -> {
            DialogAddPriority prioritySheet = new DialogAddPriority();

            prioritySheet.setOnPrioritySelectedListener(selectedPriority -> {
                priority = selectedPriority;
            });
            prioritySheet.show(getChildFragmentManager(), "TaskPriorityBottomSheet");
        });
        binding.btnSave.setOnClickListener(v -> {
            String taskName = binding.edtTaskName.getText().toString().trim();
            String description = binding.edtDescription.getText().toString().trim();
            if (taskName.isEmpty()) {
                binding.edtTaskName.setError("Task name is required");
                return;
            }
            if (selectedYear > 0 && !selectedDate.isEmpty() && !selectedTime.isEmpty()) {
                Calendar now = Calendar.getInstance();
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.set(Calendar.YEAR, selectedYear);
                selectedCal.set(Calendar.MONTH, selectedMonth);
                selectedCal.set(Calendar.DAY_OF_MONTH, selectedDay);
                selectedCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                selectedCal.set(Calendar.MINUTE, selectedMinute);
                selectedCal.set(Calendar.SECOND, 0);

                if (selectedCal.before(now)) {
                    Toast.makeText(requireContext(), "Task schedule date and time cannot be in the past", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            String date = selectedDate;
            String time = selectedTime;
            String category = selectedCategory;
            String priority = String.valueOf(this.priority);
            saveTaskToSupabase(taskName, description, date, time, category, priority);
        });
        getParentFragmentManager().setFragmentResultListener(
                "category_selected",
                this,
                (requestKey, bundle) -> {
                    selectedCategory = bundle.getString("selectedCategory", "");
                    selectedCategoryColor = bundle.getString("selectedCategoryColor", "");
                    int iconRes = bundle.getInt("selectedCategoryIcon", 0);
                    if (iconRes != 0) {
                        selectedCategoryIcon = String.valueOf(iconRes);
                    } else {
                        selectedCategoryIcon = bundle.getString("selectedCategoryIcon", "");
                    }
                    Toast.makeText(requireContext(), "Category: " + selectedCategory, Toast.LENGTH_SHORT).show();
                }
        );

        setupFieldNavigation();

        return dialog;
    }

    private void setupFieldNavigation() {
        binding.edtTaskName.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        binding.edtTaskName.setSingleLine(true);
        binding.edtTaskName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.edtDescription.requestFocus();
                return true;
            }
            return false;
        });

        binding.edtTaskName.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                binding.edtDescription.requestFocus();
                return true;
            }
            return false;
        });

        binding.edtDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.edtDescription.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.btnSave.performClick();
                return true;
            }
            return false;
        });

        binding.edtDescription.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                binding.btnSave.performClick();
                return true;
            }
            return false;
        });
    }

    private void saveTaskToSupabase(String taskName, String description, String date, String time, String category, String priority) {
        SharedPrefManager prefManager = SharedPrefManager.getInstance(requireContext());
        String userId = prefManager.getString("user_id");
        if (userId == null || userId.isEmpty()) {
            userId = SupabaseAuth.getUserId(requireContext());
        }
        String accessToken = prefManager.getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(requireContext());
        }

        if (userId == null || userId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }
        String username = prefManager.getString("username");
        if (username.isEmpty()) {
            username = SupabaseAuth.getUsername(requireContext());
        }
        if (username.isEmpty()) {
            username = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE).getString("user_name", "");
        }
        final String finalUserId = userId;
        binding.btnSave.setEnabled(false);
        SupabaseRepository repository = new SupabaseRepository();
        repository.saveTask(finalUserId, username, accessToken, taskName, description, date, time, category, selectedCategoryColor, selectedCategoryIcon, priority, false, new SupabaseRepository.TaskCallback() {
            @Override
            public void onSuccess(String taskId) {
                Toast.makeText(requireContext(), "Task saved successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            }
            @Override
            public void onError(String error) {
                binding.btnSave.setEnabled(true);
                Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCalendar() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            openTimePicker();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();
        Calendar selectedCal = Calendar.getInstance();
        if (selectedYear > 0) {
            selectedCal.set(Calendar.YEAR, selectedYear);
            selectedCal.set(Calendar.MONTH, selectedMonth);
            selectedCal.set(Calendar.DAY_OF_MONTH, selectedDay);
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
            selectedCal.set(Calendar.HOUR_OF_DAY, hour);
            selectedCal.set(Calendar.MINUTE, minute);
            selectedCal.set(Calendar.SECOND, 0);

            if (selectedCal.before(now)) {
                Toast.makeText(requireContext(), "Cannot select a past time", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedHour = hour;
            selectedMinute = minute;
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            Toast.makeText(requireContext(), "Date: " + date + "\nTime: " + time, Toast.LENGTH_SHORT).show();
            selectedDate = date;
            selectedTime = time;
        },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }
    }
}