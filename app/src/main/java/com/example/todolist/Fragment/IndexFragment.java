package com.example.todolist.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.TaskInputField;
import com.example.todolist.activity.EditTaskActivity;
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
    private final ArrayList<TaskModel> upcomingTasks = new ArrayList<>();
    private final ArrayList<TaskModel> allTasks = new ArrayList<>();
    private final ArrayList<TaskModel> completedTasks = new ArrayList<>();

    private TaskAdapter todayAdapter;
    private TaskAdapter upcomingAdapter;
    private TaskAdapter allAdapter;
    private TaskAdapter completedAdapter;

    private ValueEventListener tasksListener;
    private String currentTab = "Today";

    public IndexFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIndexBinding.inflate(inflater, container, false);
        taskInputField = binding.Task;
        taskCompletedInputField = binding.TaskCompleted;

        todayAdapter = new TaskAdapter(todayTasks, this::openEditTask);
        upcomingAdapter = new TaskAdapter(upcomingTasks, this::openEditTask);
        allAdapter = new TaskAdapter(allTasks, this::openEditTask);
        completedAdapter = new TaskAdapter(completedTasks, this::openEditTask);

        setupTabs();
        setupQuickActions();
        updateGreeting();
        loadSavedAvatar();

        updateTabUI(currentTab);
        loadTasksFromFirebase();
        return binding.getRoot();
    }

    private void setupTabs() {
        binding.tabToday.setOnClickListener(v -> updateTabUI("Today"));
        binding.tabAll.setOnClickListener(v -> updateTabUI("All"));
        binding.tabUpcoming.setOnClickListener(v -> updateTabUI("Upcoming"));
        binding.tabCompleted.setOnClickListener(v -> updateTabUI("Completed"));
    }

    private void setupQuickActions() {
        binding.btnQuickAddAction.setOnClickListener(v -> {
            String quickTitle = binding.edtQuickSearch.getText().toString().trim();
            Intent intent = new Intent(requireContext(), com.example.todolist.activity.CreateTaskActivity.class);
            if (!quickTitle.isEmpty()) {
                intent.putExtra("quick_title", quickTitle);
            }
            startActivity(intent);
        });

        binding.btnSearchTop.setOnClickListener(v -> binding.edtQuickSearch.requestFocus());

        binding.btnDeepFocusPlay.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Starting Deep Focus session...", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateGreeting() {
        if (binding == null || !isAdded()) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting;
        if (hour < 12) {
            timeGreeting = "Good morning";
        } else if (hour < 17) {
            timeGreeting = "Good afternoon";
        } else {
            timeGreeting = "Good evening";
        }

        String displayName = "Elena";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            displayName = user.getDisplayName();
        } else {
            String savedName = requireContext()
                    .getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE)
                    .getString("user_name", null);
            if (savedName != null && !savedName.trim().isEmpty()) {
                displayName = savedName;
            }
        }
        binding.txtGreeting.setText(timeGreeting + ", " + displayName);
    }

    private void updateTabUI(String tabName) {
        currentTab = tabName;
        if (binding == null || !isAdded()) return;

        int selectedBg = R.drawable.bg_tab_selected;
        int unselectedBg = R.drawable.bg_tab_unselected;
        int selectedBadgeBg = R.drawable.bg_badge_counter_purple;
        int unselectedBadgeBg = R.drawable.bg_badge_counter_dark;
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int muted = ContextCompat.getColor(requireContext(), R.color.text_muted);

        boolean isToday = "Today".equals(tabName);
        boolean isAll = "All".equals(tabName);
        boolean isUpcoming = "Upcoming".equals(tabName);
        boolean isCompleted = "Completed".equals(tabName);

        // Tab pill and badge styling
        binding.tabToday.setBackgroundResource(isToday ? selectedBg : unselectedBg);
        binding.txtTabToday.setTextColor(isToday ? white : muted);
        binding.badgeTabToday.setBackgroundResource(isToday ? selectedBadgeBg : unselectedBadgeBg);
        binding.badgeTabToday.setTextColor(isToday ? white : muted);

        binding.tabAll.setBackgroundResource(isAll ? selectedBg : unselectedBg);
        binding.txtTabAll.setTextColor(isAll ? white : muted);
        binding.badgeTabAll.setBackgroundResource(isAll ? selectedBadgeBg : unselectedBadgeBg);
        binding.badgeTabAll.setTextColor(isAll ? white : muted);

        binding.tabUpcoming.setBackgroundResource(isUpcoming ? selectedBg : unselectedBg);
        binding.txtTabUpcoming.setTextColor(isUpcoming ? white : muted);
        binding.badgeTabUpcoming.setBackgroundResource(isUpcoming ? selectedBadgeBg : unselectedBadgeBg);
        binding.badgeTabUpcoming.setTextColor(isUpcoming ? white : muted);

        binding.tabCompleted.setBackgroundResource(isCompleted ? selectedBg : unselectedBg);
        binding.txtTabCompleted.setTextColor(isCompleted ? white : muted);
        binding.badgeTabCompleted.setBackgroundResource(isCompleted ? selectedBadgeBg : unselectedBadgeBg);
        binding.badgeTabCompleted.setTextColor(isCompleted ? white : muted);

        // Content visibility according to selected tab
        if (isToday) {
            // Show only Today's tasks; remove/hide completed tasks
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("Today");
            taskInputField.setRecyclerViewAdapter(todayAdapter);

            binding.cardDeepFocus.setVisibility(View.VISIBLE);
            taskCompletedInputField.setVisibility(View.GONE);
        } else if (isAll) {
            // Show all active tasks and completed tasks
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("All Tasks");
            taskInputField.setRecyclerViewAdapter(allAdapter);

            binding.cardDeepFocus.setVisibility(View.GONE);
            taskCompletedInputField.setVisibility(View.VISIBLE);
            taskCompletedInputField.setLabel("Completed");
            taskCompletedInputField.setRecyclerViewAdapter(completedAdapter);
        } else if (isUpcoming) {
            // Show only upcoming tasks
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("Upcoming");
            taskInputField.setRecyclerViewAdapter(upcomingAdapter);

            binding.cardDeepFocus.setVisibility(View.GONE);
            taskCompletedInputField.setVisibility(View.GONE);
        } else if (isCompleted) {
            // Show only completed tasks
            taskInputField.setVisibility(View.GONE);
            binding.cardDeepFocus.setVisibility(View.GONE);

            taskCompletedInputField.setVisibility(View.VISIBLE);
            taskCompletedInputField.setLabel("Completed");
            taskCompletedInputField.setRecyclerViewAdapter(completedAdapter);
        }
    }

    private void loadSavedAvatar() {
        if (binding == null || !isAdded()) {
            return;
        }
        String savedAvatarUri = requireContext()
                .getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            try {
                java.io.File file = new java.io.File(savedAvatarUri);
                if (file.exists()) {
                    binding.ivIndexAvatar.setImageURI(null);
                    binding.ivIndexAvatar.setImageURI(android.net.Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
    }

    private void openEditTask(TaskModel taskModel) {
        Intent intent = new Intent(requireContext(), EditTaskActivity.class);
        intent.putExtra("taskId", taskModel.getTaskId());
        intent.putExtra("taskName", taskModel.getTaskName());
        intent.putExtra("description", taskModel.getDescription());
        intent.putExtra("date", taskModel.getDate());
        intent.putExtra("time", taskModel.getTime());
        intent.putExtra("category", taskModel.getCategory());
        intent.putExtra("priority", taskModel.getPriority());
        intent.putExtra("categoryColor", taskModel.getCategoryColor());
        intent.putExtra("categoryIcon", taskModel.getCategoryIcon());
        intent.putExtra("completed", taskModel.isCompleted());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasksFromFirebase();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private Calendar parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String[] formats = new String[]{"dd/MM/yyyy", "d/M/yyyy", "MMM dd, yyyy", "MMM d, yyyy", "yyyy-MM-dd"};
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(dateStr.trim());
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal;
                }
            } catch (Exception ignored) {}
        }
        return null;
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
                upcomingTasks.clear();
                allTasks.clear();
                completedTasks.clear();

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TaskModel taskModel = dataSnapshot.getValue(TaskModel.class);
                    if (taskModel != null) {
                        if (taskModel.isCompleted()) {
                            completedTasks.add(taskModel);
                        } else {
                            allTasks.add(taskModel);
                            Calendar taskCal = parseDate(taskModel.getDate());
                            if (taskCal != null) {
                                if (isSameDay(taskCal, today)) {
                                    todayTasks.add(taskModel);
                                } else if (taskCal.after(today)) {
                                    upcomingTasks.add(taskModel);
                                } else {
                                    // Overdue active tasks belong in Today's focus
                                    todayTasks.add(taskModel);
                                }
                            } else {
                                // Default undated tasks show up in today's active tasks
                                todayTasks.add(taskModel);
                            }
                        }
                    }
                }

                todayAdapter.notifyDataSetChanged();
                upcomingAdapter.notifyDataSetChanged();
                allAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();

                // Update tab badge counters
                binding.badgeTabToday.setText(String.valueOf(todayTasks.size()));
                binding.badgeTabAll.setText(String.valueOf(allTasks.size()));
                binding.badgeTabUpcoming.setText(String.valueOf(upcomingTasks.size()));
                binding.badgeTabCompleted.setText(String.valueOf(completedTasks.size()));

                // Update summary text
                binding.txtTaskSummary.setText(todayTasks.size() == 1
                        ? "You have 1 task scheduled for today"
                        : "You have " + todayTasks.size() + " tasks scheduled for today");

                // Update progress pill (done %)
                int totalCount = allTasks.size() + completedTasks.size();
                int percent = totalCount > 0 ? (completedTasks.size() * 100) / totalCount : 0;
                binding.txtProgressPercent.setText(percent + "%");
                binding.progressDone.setProgress(percent);

                // Refresh active tab views
                updateTabUI(currentTab);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (databaseReference != null && tasksListener != null) {
            databaseReference.removeEventListener(tasksListener);
        }

        binding = null;
    }
}
