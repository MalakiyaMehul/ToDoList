package com.example.todolist.Fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.TaskInputField;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.activity.TaskDetail;
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.databinding.FragmentIndexBinding;
import com.example.todolist.model.TaskModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IndexFragment extends Fragment {

    private FragmentIndexBinding binding;
    private TaskInputField taskInputField;

    private final ArrayList<TaskModel> todayTasks = new ArrayList<>();
    private final ArrayList<TaskModel> upcomingTasks = new ArrayList<>();
    private final ArrayList<TaskModel> allTasks = new ArrayList<>();
    private final ArrayList<TaskModel> completedTasks = new ArrayList<>();

    private TaskAdapter todayAdapter;
    private TaskAdapter upcomingAdapter;
    private TaskAdapter allAdapter;
    private TaskAdapter completedAdapter;

    private String currentTab = "Today";
    private final List<TaskModel> masterTaskList = new ArrayList<>();
    private String currentSearchQuery = "";

    public IndexFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIndexBinding.inflate(inflater, container, false);
        taskInputField = binding.Task;

        todayAdapter = new TaskAdapter(todayTasks, this::openEditTask);
        upcomingAdapter = new TaskAdapter(upcomingTasks, this::openEditTask);
        allAdapter = new TaskAdapter(allTasks, this::openEditTask);
        completedAdapter = new TaskAdapter(completedTasks, this::openEditTask);

        setupTabs();
        setupQuickActions();
        updateGreeting();
        loadSavedAvatar();

        binding.swipeRefreshLayout.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadTasks);

        updateTabUI(currentTab);
        loadTasks();
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
            String query = binding.edtQuickSearch.getText().toString().trim();
            performTaskSearch(query);
        });

        binding.edtQuickSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP)) {
                String query = binding.edtQuickSearch.getText().toString().trim();
                performTaskSearch(query);
                return true;
            }
            return false;
        });

        binding.edtQuickSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s != null ? s.toString().trim() : "";
                performTaskSearch(query);
            }
        });

        binding.btnDeepFocusPlay.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Starting Deep Focus session...", Toast.LENGTH_SHORT).show()
        );
    }

    private void performTaskSearch(String query) {
        currentSearchQuery = query != null ? query.trim() : "";

        todayAdapter.setSearchQuery(currentSearchQuery);
        upcomingAdapter.setSearchQuery(currentSearchQuery);
        allAdapter.setSearchQuery(currentSearchQuery);
        completedAdapter.setSearchQuery(currentSearchQuery);

        if (masterTaskList == null || masterTaskList.isEmpty()) {
            populateTasks(new ArrayList<>());
            return;
        }

        if (currentSearchQuery.isEmpty()) {
            populateTasks(masterTaskList);
            return;
        }

        List<TaskModel> filteredList = new ArrayList<>();
        String q = currentSearchQuery.toLowerCase(Locale.getDefault());
        for (TaskModel task : masterTaskList) {
            if (task != null) {
                String name = task.getTaskName() != null ? task.getTaskName().toLowerCase(Locale.getDefault()) : "";
                String desc = task.getDescription() != null ? task.getDescription().toLowerCase(Locale.getDefault()) : "";
                String cat = task.getCategory() != null ? task.getCategory().toLowerCase(Locale.getDefault()) : "";
                if (name.contains(q) || desc.contains(q) || cat.contains(q)) {
                    filteredList.add(task);
                }
            }
        }

        populateTasks(filteredList);
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
        String savedName = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("user_name", null);
        if (savedName == null || savedName.trim().isEmpty()) {
            savedName = SharedPrefManager.getInstance(requireContext()).getString("username");
        }
        if (savedName != null && !savedName.trim().isEmpty()) {
            displayName = savedName;
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

        if (isToday) {
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("Today");
            taskInputField.setRecyclerViewAdapter(todayAdapter);
            binding.cardDeepFocus.setVisibility(View.VISIBLE);
        } else if (isAll) {
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("All Tasks");
            taskInputField.setRecyclerViewAdapter(allAdapter);
            binding.cardDeepFocus.setVisibility(View.GONE);
        } else if (isUpcoming) {
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("Upcoming");
            taskInputField.setRecyclerViewAdapter(upcomingAdapter);
            binding.cardDeepFocus.setVisibility(View.GONE);
        } else if (isCompleted) {
            taskInputField.setVisibility(View.VISIBLE);
            taskInputField.setLabel("Completed");
            taskInputField.setRecyclerViewAdapter(completedAdapter);
            binding.cardDeepFocus.setVisibility(View.GONE);
        }
    }

    private void loadSavedAvatar() {
        if (binding == null || !isAdded()) {
            return;
        }
        String savedAvatarUri = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            try {
                File file = new File(savedAvatarUri);
                if (file.exists()) {
                    binding.ivIndexAvatar.setImageURI(null);
                    binding.ivIndexAvatar.setImageURI(Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
    }

    private void openEditTask(TaskModel taskModel) {
        Intent intent = new Intent(requireContext(), TaskDetail.class);
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
        intent.putExtra("subTask", taskModel.getSubTask());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedAvatar();
        loadTasks();
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
                Date date = sdf.parse(dateStr.trim());
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void populateTasks(List<TaskModel> taskList) {
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

        for (TaskModel taskModel : taskList) {
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
                            todayTasks.add(taskModel);
                        }
                    } else {
                        todayTasks.add(taskModel);
                    }
                }
            }
        }

        todayAdapter.notifyDataSetChanged();
        upcomingAdapter.notifyDataSetChanged();
        allAdapter.notifyDataSetChanged();
        completedAdapter.notifyDataSetChanged();

        binding.badgeTabToday.setText(String.valueOf(todayTasks.size()));
        binding.badgeTabAll.setText(String.valueOf(allTasks.size()));
        binding.badgeTabUpcoming.setText(String.valueOf(upcomingTasks.size()));
        binding.badgeTabCompleted.setText(String.valueOf(completedTasks.size()));

        binding.txtTaskSummary.setText(todayTasks.size() == 1
                ? "You have 1 task scheduled for today"
                : "You have " + todayTasks.size() + " tasks scheduled for today");

        int totalCount = allTasks.size() + completedTasks.size();
        int percent = totalCount > 0 ? (completedTasks.size() * 100) / totalCount : 0;
        updateTabUI(currentTab);
    }

    private void loadTasks() {
        if (!isAdded()) return;

        SharedPrefManager prefManager = SharedPrefManager.getInstance(requireContext());
        String userId = prefManager.getString("user_id");
        if (userId.isEmpty()) {
            userId = SupabaseAuth.getUserId(requireContext());
        }
        String accessToken = prefManager.getString("access_token");
        if (accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(requireContext());
        }

        if (userId == null || userId.isEmpty()) {
            if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        SupabaseRepository repository = new SupabaseRepository();
        repository.getTasks(userId, accessToken, new SupabaseRepository.GetTasksCallback() {
            @Override
            public void onSuccess(List<TaskModel> tasks) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        }
                        if (tasks != null) {
                            masterTaskList.clear();
                            masterTaskList.addAll(tasks);
                        }
                        performTaskSearch(currentSearchQuery);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                            binding.swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
