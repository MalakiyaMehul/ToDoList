package com.example.todolist.Fragment;

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
import com.example.todolist.adapter.TaskAdapter;
import com.example.todolist.databinding.FragmentCalendarBinding;
import com.example.todolist.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
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
    private TaskAdapter adapter;
    private boolean showCompleted = false;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        adapter = new TaskAdapter(visibleTasks);
        binding.recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCalendarTasks.setAdapter(adapter);
        
        updateMonth();
        buildDays();
        loadTasksFromFirebase();

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
            showCompleted = false;
            binding.btnToday.setBackgroundColor(Color.rgb(133, 133, 232));
            binding.btnCompleted.setBackgroundResource(R.drawable.bg_completed_button);
            filterTasks();
        });

        binding.btnCompleted.setOnClickListener(v -> {
            showCompleted = true;
            binding.btnCompleted.setBackgroundColor(Color.rgb(133, 133, 232));
            binding.btnToday.setBackgroundColor(Color.rgb(69, 69, 69));
            filterTasks();
        });

        return binding.getRoot();
    }

    private void updateMonth() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        binding.txtMonth.setText(monthFormat.format(calendar.getTime()).toUpperCase(Locale.ENGLISH));
        binding.txtYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
    }

    private void buildDays() {
        binding.dayContainer.removeAllViews();

        Calendar firstDay = (Calendar) calendar.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            final int selected = day;

            Calendar dayCalendar = (Calendar) calendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, day);

            String weekday = new SimpleDateFormat("EEE", Locale.ENGLISH).format(dayCalendar.getTime()).toUpperCase(Locale.ENGLISH);

            LinearLayout dayItem = new LinearLayout(requireContext());
            dayItem.setOrientation(LinearLayout.VERTICAL);
            dayItem.setGravity(Gravity.CENTER);
            dayItem.setPadding(10, 8, 10, 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(52), dp(54));
            params.setMargins(dp(3), 0, dp(3), 0);
            dayItem.setLayoutParams(params);

            boolean isSelected = selectedCalender.get(Calendar.DAY_OF_MONTH) == day && selectedCalender.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && selectedCalender.get(Calendar.YEAR) == calendar.get(Calendar.YEAR);

            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(dp(5));
            background.setColor(isSelected ? Color.rgb(133, 133, 232) : Color.rgb(45, 45, 45));
            dayItem.setBackground(background);

            TextView dayName = new TextView(requireContext());
            dayName.setText(weekday);
            dayName.setTextSize(11);
            dayName.setTypeface(null, Typeface.BOLD);
            dayName.setGravity(Gravity.CENTER);

            boolean weekend = dayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

            dayName.setTextColor(weekend && !isSelected ? Color.RED : Color.WHITE);

            TextView dayNumber = new TextView(requireContext());
            dayNumber.setText(String.valueOf(day));
            dayNumber.setTextSize(13);
            dayNumber.setTextColor(Color.WHITE);
            dayNumber.setGravity(Gravity.CENTER);

            dayItem.addView(dayName);
            dayItem.addView(dayNumber);

            dayItem.setOnClickListener(v -> {
                selectedCalender.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), selected);
                buildDays();
                filterTasks();
            });
            binding.dayContainer.addView(dayItem);
        }
    }

    private void loadTasksFromFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Tasks");
        tasksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTasks.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    TaskModel task = child.getValue(TaskModel.class);
                    if (task != null) {
                        allTasks.add(task);
                    }
                }
                filterTasks();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void filterTasks() {
        visibleTasks.clear();
        String selectedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedCalender.getTime());

        for (TaskModel task : allTasks) {
            if (showCompleted) {
                if (task.isCompleted()) {
                    visibleTasks.add(task);
                }
            } else {
                if (!task.isCompleted() && selectedDate.equals(task.getDate())) {
                    visibleTasks.add(task);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}