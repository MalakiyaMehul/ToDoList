package com.example.todolist.activity;

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
import com.example.todolist.databinding.ActivityEditTaskBinding;
import com.example.todolist.databinding.ActivityEditTaskBottomSheetBinding;
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

public class EditTaskBottomSheet extends BottomSheetDialogFragment {
    private ActivityEditTaskBottomSheetBinding binding;
    public interface OnTaskEditListener {
        void onTaskEdit(String taskName, String description);
    }

    private OnTaskEditListener listener;

    public void setOnTaskEditListener(OnTaskEditListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = ActivityEditTaskBottomSheetBinding.inflate(LayoutInflater.from(requireContext()));

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(binding.getRoot());
        Bundle bundle = getArguments();

        if (bundle != null) {
            binding.edtTaskName.setText(bundle.getString("taskName", ""));
            binding.edtDescription.setText(bundle.getString("description", ""));
        }
        binding.btnEditTask.setOnClickListener(v -> {

            String taskName = binding.edtTaskName.getText().toString().trim();
            String description = binding.edtDescription.getText().toString().trim();

            if (taskName.isEmpty()) {
                binding.edtTaskName.setError("Task name is required");
                return;
            }

            if (listener != null) {
                listener.onTaskEdit(taskName, description);
            }

            dismiss();
        });
        return dialog;
    }
    public static EditTaskBottomSheet newInstance(String taskName, String description) {

        EditTaskBottomSheet bottomSheet = new EditTaskBottomSheet();

        Bundle bundle = new Bundle();
        bundle.putString("taskName", taskName);
        bundle.putString("description", description);

        bottomSheet.setArguments(bundle);

        return bottomSheet;
    }
}