package com.example.todolist.Fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.TaskInputField;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.databinding.FragmentIndexBinding;
import com.example.todolist.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;


public class IndexFragment extends Fragment {

    private FragmentIndexBinding binding;
    private DatabaseReference databaseReference;
    private TaskInputField taskInputField;
    private TaskInputField taskCompletedInputField;
    private final ArrayList<TaskModel> todayTasks = new ArrayList<>();
    private final ArrayList<TaskModel> completedTasks = new ArrayList<>();
    private TaskAdapter todayAdapter;
    private TaskAdapter completedAdapter;
    private ValueEventListener tasksListener;

    public IndexFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIndexBinding.inflate(inflater, container, false);
        taskInputField = binding.Task;
        taskCompletedInputField = binding.TaskCompleted;

        todayAdapter = new TaskAdapter(todayTasks);
        completedAdapter = new TaskAdapter(completedTasks);

        taskInputField.setLabel("Today");
        taskInputField.setRecyclerViewAdapter(todayAdapter);

        taskCompletedInputField.setLabel("Completed");
        taskCompletedInputField.setRecyclerViewAdapter(completedAdapter);

        loadTasksFromFirebase();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasksFromFirebase();
    }

    private void loadTasksFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String uId = user.getUid();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uId)
                .child("Tasks");

        if (tasksListener != null) {
            databaseReference.removeEventListener(tasksListener);
        }

        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) {
                    return;
                }
                todayTasks.clear();
                completedTasks.clear();
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TaskModel taskModel = dataSnapshot.getValue(TaskModel.class);
                    if (taskModel != null) {
                        if (taskModel.isCompleted()) {
                            completedTasks.add(taskModel);
                        } else {
                            if (todayDate.equals(taskModel.getDate())) {
                                todayTasks.add(taskModel);
                            }
                        }
                    }
                }
                todayAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load tasks: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        databaseReference.addValueEventListener(tasksListener);
    }

    public void onDestroyView() {
        super.onDestroyView();

        if (databaseReference != null && tasksListener != null) {
            databaseReference.removeEventListener(tasksListener);
        }

        binding = null;
    }
}