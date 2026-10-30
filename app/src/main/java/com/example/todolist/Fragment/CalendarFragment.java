package com.example.todolist.Fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.todolist.R;
import com.example.todolist.activity.EditTaskActivity;
import com.example.todolist.adapter.CalendarTaskAdapter;
import com.example.todolist.databinding.FragmentCalendarBinding;
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

public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private final Calendar calendar = Calendar.getInstance();
    private final Calendar selectedCalender = Calendar.getInstance();
    private final ArrayList<TaskModel> allTasks = new ArrayList<>();
    private final ArrayList<TaskModel> visibleTasks = new ArrayList<>();
    private CalendarTaskAdapter adapter;
    private DatabaseReference databaseReference;
    private ValueEventListener tasksListener;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);

        adapter = new CalendarTaskAdapter(visibleTasks, this::openEditTask, this::updateMomentumCard);
        binding.recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCalendarTasks.setAdapter(adapter);

        updateMonth();
        buildDays();
        loadSavedAvatar();
        setupListeners();
        loadTasksFromFirebase();

        return binding.getRoot();
    }

    private void setupListeners() {
        binding.btnPreviousMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonth();
            buildDays();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonth();
            buildDays();
        });

        binding.btnToday.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            calendar.setTime(now.getTime());
            selectedCalender.setTime(now.getTime());
            updateMonth();
            buildDays();
            filterTasks();
        });

        binding.btnTimelineView.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Timeline view active", Toast.LENGTH_SHORT).show()
        );

        binding.btnSearchTop.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Search tasks...", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateMonth() {
        if (binding == null) return;
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        binding.txtMonthYear.setText(monthYearFormat.format(calendar.getTime()));
    }

    private void buildDays() {
        if (binding == null || !isAdded()) return;
        binding.dayContainer.removeAllViews();

        Calendar firstDay = (Calendar) calendar.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        View selectedView = null;

        for (int day = 1; day <= daysInMonth; day++) {
            final int selectedDayNumber = day;

            Calendar dayCalendar = (Calendar) calendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, day);

            // Container column
            LinearLayout dayColumn = new LinearLayout(requireContext());
            dayColumn.setOrientation(LinearLayout.VERTICAL);
            dayColumn.setGravity(Gravity.CENTER_HORIZONTAL);
            dayColumn.setPadding(dp(2), 0, dp(2), 0);

            // Weekday letter initial (M, T, W, T, F, S, S)
            String weekday = new SimpleDateFormat("EEE", Locale.ENGLISH).format(dayCalendar.getTime());
            String initial = weekday.substring(0, 1).toUpperCase(Locale.ENGLISH);

            TextView dayLetter = new TextView(requireContext());
            dayLetter.setText(initial);
            dayLetter.setTextSize(11);
            dayLetter.setTypeface(Typeface.DEFAULT_BOLD);
            dayLetter.setTextColor(Color.parseColor("#71717A"));
            dayLetter.setGravity(Gravity.CENTER);

            // Day Pill
            LinearLayout pill = new LinearLayout(requireContext());
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(dp(44), dp(58));
            pillParams.topMargin = dp(4);
            pill.setLayoutParams(pillParams);

            boolean isSelected = isSameDay(selectedCalender, dayCalendar);
            if (isSelected) {
                pill.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                selectedView = dayColumn;
            } else {
                pill.setBackgroundResource(R.drawable.bg_calendar_day_unselected);
            }

            // Day Number
            TextView dayNumber = new TextView(requireContext());
            dayNumber.setText(String.valueOf(day));
            dayNumber.setTextSize(15);
            dayNumber.setTypeface(Typeface.DEFAULT_BOLD);
            dayNumber.setGravity(Gravity.CENTER);
            dayNumber.setTextColor(isSelected ? Color.parseColor("#200A4A") : Color.WHITE);
            pill.addView(dayNumber);

            // Indicator Dots Row for tasks on that day
            LinearLayout dotsRow = new LinearLayout(requireContext());
            dotsRow.setOrientation(LinearLayout.HORIZONTAL);
            dotsRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dotsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(6)
            );
            dotsParams.topMargin = dp(2);
            dotsRow.setLayoutParams(dotsParams);

            int countForDay = countTasksForDay(dayCalendar);
            if (countForDay > 0) {
                int maxDots = Math.min(countForDay, 3);
                for (int d = 0; d < maxDots; d++) {
                    int dotColor;
                    if (isSelected) {
                        dotColor = Color.parseColor("#200A4A");
                    } else {
                        dotColor = (d % 2 == 0) ? Color.parseColor("#C084FC") : Color.parseColor("#F43F5E");
                    }
                    dotsRow.addView(createDot(dotColor));
                }
            }
            pill.addView(dotsRow);

            dayColumn.addView(dayLetter);
            dayColumn.addView(pill);

            dayColumn.setOnClickListener(v -> {
                selectedCalender.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selectedDayNumber);
                buildDays();
                filterTasks();
            });

            binding.dayContainer.addView(dayColumn);
        }

        // Center scroll on selected day
        if (selectedView != null) {
            final View targetView = selectedView;
            binding.dayScroll.post(() -> {
                if (binding != null && targetView.getParent() != null) {
                    int scrollX = targetView.getLeft() - (binding.dayScroll.getWidth() / 2) + (targetView.getWidth() / 2);
                    binding.dayScroll.smoothScrollTo(Math.max(0, scrollX), 0);
                }
            });
        }
    }

    private View createDot(int color) {
        View dot = new View(requireContext());
        int size = dp(4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(1), 0, dp(1), 0);
        dot.setLayoutParams(lp);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        dot.setBackground(shape);
        return dot;
    }

    private int countTasksForDay(Calendar dayCalendar) {
        int count = 0;
        for (TaskModel task : allTasks) {
            Calendar taskCal = parseDate(task.getDate());
            if (taskCal != null && isSameDay(taskCal, dayCalendar)) {
                count++;
            }
        }
        return count;
    }

    private void loadTasksFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String uid = user.getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Tasks");

        if (tasksListener != null) {
            databaseReference.removeEventListener(tasksListener);
        }

        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;

                allTasks.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TaskModel task = child.getValue(TaskModel.class);
                    if (task != null) {
                        allTasks.add(task);
                    }
                }
                buildDays();
                filterTasks();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        };
        databaseReference.addValueEventListener(tasksListener);
    }

    private void filterTasks() {
        if (binding == null || !isAdded()) return;

        visibleTasks.clear();
        for (TaskModel task : allTasks) {
            Calendar taskCal = parseDate(task.getDate());
            if (taskCal != null && isSameDay(taskCal, selectedCalender)) {
                visibleTasks.add(task);
            }
        }
        adapter.notifyDataSetChanged();

        // Update Section Header
        SimpleDateFormat headerDateFormat = new SimpleDateFormat("MMMM d", Locale.ENGLISH);
        String formattedDate = headerDateFormat.format(selectedCalender.getTime());
        binding.txtTasksForDate.setText("Tasks for " + formattedDate);

        int count = visibleTasks.size();
        binding.txtTasksCountBadge.setText(count + (count == 1 ? " task" : " tasks"));

        // Empty state visibility
        binding.txtEmptyCalendarTasks.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        binding.recyclerCalendarTasks.setVisibility(count == 0 ? View.GONE : View.VISIBLE);

        // Update Daily Momentum Highlight Card
        updateMomentumCard();
    }

    private void updateMomentumCard() {
        if (binding == null || !isAdded()) return;

        int total = visibleTasks.size();
        if (total == 0) {
            binding.txtMomentumDesc.setText("0 hrs scheduled • No focus blocks");
            binding.progressMomentum.setProgress(0);
            binding.txtMomentumPercent.setText("0%");
            return;
        }

        int completedCount = 0;
        for (TaskModel t : visibleTasks) {
            if (t.isCompleted()) {
                completedCount++;
            }
        }

        double hours = total * 0.75;
        binding.txtMomentumDesc.setText(String.format(
                Locale.ENGLISH,
                "%.1f hrs scheduled across %d focus block%s",
                hours,
                total,
                total == 1 ? "" : "s"
        ));

        int percent = (completedCount * 100) / total;
        binding.progressMomentum.setProgress(percent);
        binding.txtMomentumPercent.setText(percent + "%");
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

    private void loadSavedAvatar() {
        if (binding == null || !isAdded()) return;
        String savedAvatarUri = requireContext()
                .getSharedPreferences("UserProfile", android.content.Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            try {
                java.io.File file = new java.io.File(savedAvatarUri);
                if (file.exists()) {
                    binding.ivCalendarAvatar.setImageURI(null);
                    binding.ivCalendarAvatar.setImageURI(android.net.Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedAvatar();
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
