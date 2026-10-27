package com.example.todolist.activity;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todolist.R;
import com.example.todolist.activity.home.AddTaskBottomSheet;
import com.example.todolist.activity.home.DialogAddPriority;
import com.example.todolist.activity.home.ItemAdd_Category;
import com.example.todolist.databinding.ActivityEditTaskBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditTaskActivity extends BaseActivity {

    private ActivityEditTaskBinding binding;
    private String taskId;
    private String categoryColor;
    private String selectedDate = "";
    private int priority = 1;
    private String selectedcategoryname = "";
    private String categoryIcon = "";
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private String selectedTime = "";
    private int selectedHour;
    private int selectedMinute;
    private String editTaskName;
    private String editDescription;
    private boolean completed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        taskId = getIntent().getStringExtra("taskId");

        binding.edtTaskName.setText(getIntent().getStringExtra("taskName"));
        binding.edtDescription.setText(getIntent().getStringExtra("description"));
        binding.txtTaskTime.setText(getIntent().getStringExtra("time"));
        binding.txtCategory.setText(getIntent().getStringExtra("category"));
        categoryIcon = getIntent().getStringExtra("categoryIcon");

        if (categoryIcon != null && !categoryIcon.isEmpty()) {
            try {
                binding.categoryicon.setImageResource(Integer.parseInt(categoryIcon));
            } catch (NumberFormatException e) {
                int iconResource = getResources().getIdentifier(
                        categoryIcon,
                        "drawable",
                        getPackageName()
                );

                if (iconResource != 0) {
                    binding.categoryicon.setImageResource(iconResource);
                }
            }
        }
        completed = getIntent().getBooleanExtra("completed", false);
        binding.txtTaskPriority.setText(getIntent().getStringExtra("priority"));
        binding.btnClose.setOnClickListener(v -> {
            finish();
        });
        binding.btnEditTitle.setOnClickListener(v -> {
            String taskName = binding.edtTaskName.getText().toString().trim();
            String description = binding.edtDescription.getText().toString().trim();
            EditTaskBottomSheet bottomSheet = EditTaskBottomSheet.newInstance(taskName, description);
            bottomSheet.setOnTaskEditListener((newTaskName, newDescription) -> {
                editTaskName = newTaskName;
                editDescription = newDescription;
                binding.edtTaskName.setText(editTaskName);
                binding.edtDescription.setText(editDescription);
            });
            bottomSheet.show(getSupportFragmentManager(), "EditTaskBottomSheet");
        });
        binding.txtTaskTime.setOnClickListener(v -> {
            openCalendar();
        });
        binding.txtCategory.setOnClickListener(v -> {
            ItemAdd_Category sheet = new ItemAdd_Category();
            sheet.show(getSupportFragmentManager(), "CategoryBottomSheet");
        });
        getSupportFragmentManager().setFragmentResultListener("category_selected", this, (requestKey, bundle) -> {
            String categoryName = bundle.getString("selectedCategory");
            String color = bundle.getString("selectedCategoryColor");
            int icon = bundle.getInt("selectedCategoryIcon", 0);
            if (categoryName != null && !categoryName.isEmpty()) {
                selectedcategoryname = categoryName;
                binding.txtCategory.setText(selectedcategoryname);
            }
            if (color != null && !color.isEmpty()) {
                categoryColor = color;
                binding.txtCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(categoryColor)));
            }
            if (icon != 0) {
                categoryIcon = String.valueOf(icon);
                binding.categoryicon.setImageResource(icon);
            }
        });
        binding.btnCategory.setOnClickListener(v -> {
            DialogAddPriority prioritySheet = new DialogAddPriority();

            prioritySheet.setOnPrioritySelectedListener(selectedPriority -> {
                priority = selectedPriority;
                binding.txtTaskPriority.setText(String.valueOf(selectedPriority));
            });

            prioritySheet.show(getSupportFragmentManager(), "DialogAddPriority");
        });
        binding.btnDeleteTask.setOnClickListener(v -> {
            deleteTask();
        });
        binding.btnEditTask.setOnClickListener(v -> {
            updateTask();
        });
    }

    private void openCalendar() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, year, month, day) -> {
            selectedYear = year;
            selectedMonth = month;
            selectedDay = day;
            openTimePicker();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, hour, minute) -> {
            selectedHour = hour;
            selectedMinute = minute;
            String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
            String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            Toast.makeText(this, "Date: " + date + "\nTime: " + time, Toast.LENGTH_LONG).show();
            selectedDate = date;
            selectedTime = time;
            binding.txtTaskTime.setText(selectedTime);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void updateTask() {
        String taskName = binding.edtTaskName.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String date = selectedDate;
        String time = binding.txtTaskTime.getText().toString().trim();
        String category = binding.txtCategory.getText().toString().trim();
        String priority = binding.txtTaskPriority.getText().toString().trim();
        if (taskName.isEmpty()) {
            binding.edtTaskName.setError("Enter task name");
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Task ID not found", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference taskRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid()).child("Tasks").child(taskId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("taskId", taskId);
        updates.put("taskName", taskName);
        updates.put("description", description);
        updates.put("date", date);
        updates.put("time", time);
        updates.put("category", category);
        updates.put("priority", priority);
        updates.put("categoryColor", categoryColor);
        updates.put("categoryIcon", categoryIcon);
        updates.put("completed", completed);
        taskRef.updateChildren(updates).addOnSuccessListener(unused -> {
            Toast.makeText(EditTaskActivity.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(EditTaskActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void deleteTask() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
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
        taskRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}