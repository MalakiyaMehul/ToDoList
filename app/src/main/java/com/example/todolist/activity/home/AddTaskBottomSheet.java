package com.example.todolist.activity.home;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.databinding.ActivityAddTaskBottomSheetBinding;
import com.example.todolist.model.CategoryModel;
import com.example.todolist.model.TaskModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
            prioritySheet.show(getChildFragmentManager(), "TaskPriorityBottomSheet"
            );
        });
        binding.btnSave.setOnClickListener(v -> {
            String taskName = binding.edtTaskName.getText().toString().trim();
            String description = binding.edtDescription.getText().toString().trim();
            if (taskName.isEmpty()) {
                binding.edtTaskName.setError("Task name is required");
                return;
            }
            String date = selectedDate;
            String time = selectedTime;
            String category = selectedCategory;
            String priority = String.valueOf(this.priority);
            saveTaskToFirebase(taskName, description, date, time, category, priority);
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
        return dialog;
    }

    private void saveTaskToFirebase(String taskName, String description, String date, String time, String category, String priority) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Please login using Firebase Authentication", Toast.LENGTH_LONG).show();
            return;
        }
        String uid = user.getUid();
        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Tasks");
        String taskId = tasksRef.push().getKey();
        if (taskId == null) {
            return;
        }
        TaskModel task = new TaskModel(taskId, taskName, description, date, time, category,selectedCategoryColor, selectedCategoryIcon, priority, false);
        tasksRef.child(taskId).setValue(task).addOnSuccessListener(unused -> {
            Toast.makeText(requireContext(), "Task saved successfully", Toast.LENGTH_SHORT).show();
            dismiss();
        }).addOnFailureListener(error -> Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void openCalendar() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (datePicker, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            openTimePicker();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timePicker, hour, minute) -> {
            selectedHour = hour;
            selectedMinute = minute;
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            Toast.makeText(requireContext(), "Date: " + date + "\nTime: " + time, Toast.LENGTH_LONG).show();
            selectedDate = date;
            selectedTime = time;
        },
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
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