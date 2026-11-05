package com.example.todolist.Fragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.FragmentFocusBinding;
import com.example.todolist.model.TaskModel;
import com.example.todolist.view.DonutChartView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FocusFragment extends Fragment {

    public static class CategoryStat {
        public String name;
        public int color;
        public int count;

        public CategoryStat(String name, int color, int count) {
            this.name = name;
            this.color = color;
            this.count = count;
        }
    }

    private FragmentFocusBinding binding;
    private String selectedTimeframe = "Week";
    private final List<TaskModel> allTasksList = new ArrayList<>();

    public FocusFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentFocusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs();
        setupActions();
        loadSavedAvatar();
        processTasksAnalytics();
        loadTasksAnalytics();
    }

    private void setupTabs() {
        binding.tabThisWeek.setOnClickListener(v -> updateTimeframeTab("Week"));
        binding.tabThisMonth.setOnClickListener(v -> updateTimeframeTab("Month"));
        binding.tabAllTime.setOnClickListener(v -> updateTimeframeTab("All"));
    }

    private void updateTimeframeTab(String timeframe) {
        selectedTimeframe = timeframe;
        if (binding == null || !isAdded()) return;

        int selectedBg = R.drawable.bg_tab_selected;
        int unselectedBg = R.drawable.bg_tab_unselected;
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        int muted = ContextCompat.getColor(requireContext(), R.color.text_muted);

        boolean isWeek = "Week".equals(timeframe);
        boolean isMonth = "Month".equals(timeframe);
        boolean isAll = "All".equals(timeframe);

        binding.tabThisWeek.setBackgroundResource(isWeek ? selectedBg : unselectedBg);
        binding.txtTabThisWeek.setTextColor(isWeek ? white : muted);

        binding.tabThisMonth.setBackgroundResource(isMonth ? selectedBg : unselectedBg);
        binding.tabThisMonth.setTextColor(isMonth ? white : muted);

        binding.tabAllTime.setBackgroundResource(isAll ? selectedBg : unselectedBg);
        binding.tabAllTime.setTextColor(isAll ? white : muted);

        processTasksAnalytics();
    }

    private void setupActions() {

        binding.btnHeaderTelemetry.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Telemetry stream live", Toast.LENGTH_SHORT).show()
        );

        binding.btnCategoryMenu.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Category options", Toast.LENGTH_SHORT).show()
        );

        binding.btnStreakDetails.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Consistency details", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadSavedAvatar() {
        if (binding == null || !isAdded()) return;
        String savedAvatarUri = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            try {
                File file = new File(savedAvatarUri);
                if (file.exists()) {
                    binding.ivFocusAvatar.setImageURI(null);
                    binding.ivFocusAvatar.setImageURI(Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateCategoryLegend(List<CategoryStat> categories, int totalCount) {
        if (binding == null || !isAdded()) return;

        binding.layoutCategoryLegend.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        if (categories == null || categories.isEmpty() || totalCount == 0) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("No tasks created yet");
            emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            emptyText.setTextSize(13f);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, (int) (12 * density), 0, (int) (6 * density));
            binding.layoutCategoryLegend.addView(emptyText);
            return;
        }

        for (CategoryStat cat : categories) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));

            View dot = new View(requireContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams((int) (8 * density), (int) (8 * density));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(cat.color);
            dot.setBackground(dotDrawable);
            row.addView(dot);

            TextView tvName = new TextView(requireContext());
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.setMarginStart((int) (10 * density));
            tvName.setLayoutParams(nameParams);
            tvName.setText(cat.name);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(14f);
            tvName.setTypeface(null, Typeface.BOLD);
            row.addView(tvName);

            TextView tvCount = new TextView(requireContext());
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvCount.setText(cat.count + (cat.count == 1 ? " task" : " tasks"));
            tvCount.setTextColor(Color.parseColor("#94A3B8"));
            tvCount.setTextSize(13f);
            tvCount.setPadding(0, 0, (int) (14 * density), 0);
            row.addView(tvCount);

            int percent = totalCount > 0 ? Math.round((cat.count * 100f) / totalCount) : 0;
            TextView tvPercent = new TextView(requireContext());
            tvPercent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvPercent.setText(percent + "%");
            tvPercent.setTextColor(cat.color);
            tvPercent.setTextSize(13f);
            tvPercent.setTypeface(null, Typeface.BOLD);
            row.addView(tvPercent);

            binding.layoutCategoryLegend.addView(row);
        }
    }

    private void loadTasksAnalytics() {
        if (!isAdded()) return;
        String uid = SharedPrefManager.getInstance(requireContext()).getString("user_id");
        if (uid.isEmpty()) {
            uid = SupabaseAuth.getUserId(requireContext());
        }
        if (uid == null || uid.isEmpty()) {
            return;
        }
        String token = SharedPrefManager.getInstance(requireContext()).getString("access_token");
        if (token.isEmpty()) {
            token = SupabaseAuth.getAccessToken(requireContext());
        }

        new SupabaseRepository().getTasks(uid, token, new SupabaseRepository.GetTasksCallback() {
            @Override
            public void onSuccess(List<TaskModel> tasks) {
                if (!isAdded() || binding == null) return;
                allTasksList.clear();
                if (tasks != null) {
                    allTasksList.addAll(tasks);
                }
                processTasksAnalytics();
            }

            @Override
            public void onError(String error) {
            }
        });
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

    private boolean isDateInTimeframe(String dateStr, String timeframe) {
        if ("All".equals(timeframe)) return true;
        if (dateStr == null || dateStr.trim().isEmpty()) return false;
        
        try {
            Calendar taskCal = parseDate(dateStr);
            if (taskCal == null) return false;
            
            Calendar now = Calendar.getInstance();
            
            if ("Month".equals(timeframe)) {
                return taskCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                       taskCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);
            } else if ("Week".equals(timeframe)) {
                return taskCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                       taskCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);
            }
        } catch (Exception e) {
        }
        return false; 
    }

    private void processTasksAnalytics() {
        if (binding == null || !isAdded()) return;

        int totalTasks = 0;
        int completedTasks = 0;
        int dueTodayCount = 0;

        Calendar todayCal = Calendar.getInstance();
        Map<String, CategoryStat> categoryMap = new LinkedHashMap<>();

        for (TaskModel task : allTasksList) {
            if (task == null) continue;

            if (!task.isCompleted()) {
                Calendar taskCal = parseDate(task.getDate());
                if (taskCal != null &&
                        taskCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    dueTodayCount++;
                }
            }

            if (!isDateInTimeframe(task.getDate(), selectedTimeframe)) {
                continue;
            }

            totalTasks++;
            if (task.isCompleted()) {
                completedTasks++;
            }

            String category = task.getCategory();
            if (category == null || category.trim().isEmpty()) {
                category = "Other";
            } else {
                category = category.trim();
            }

            int color;
            String colorHex = task.getCategoryColor();
            if (colorHex != null && !colorHex.trim().isEmpty()) {
                try {
                    color = Color.parseColor(colorHex.trim());
                } catch (Exception e) {
                    color = DonutChartView.getCategoryColor(category);
                }
            } else {
                color = DonutChartView.getCategoryColor(category);
            }

            if (categoryMap.containsKey(category)) {
                categoryMap.get(category).count++;
            } else {
                categoryMap.put(category, new CategoryStat(category, color, 1));
            }
        }

        int pendingTasks = totalTasks - completedTasks;
        float completionPercent = totalTasks > 0 ? (completedTasks * 100f) / totalTasks : 0f;

        binding.tvTotalTasks.setText(String.valueOf(totalTasks));
        binding.tvPendingTasks.setText(String.valueOf(pendingTasks));
        binding.tvPendingDue.setText(dueTodayCount + " due today");
        binding.tvPendingStatusPill.setText(pendingTasks == 0 ? "● Caught up" : "● On track");

        binding.tvCompletionPercent.setText(String.format(Locale.ENGLISH, "%.1f%%", completionPercent));
        binding.progressBarCompletion.setProgress((int) completionPercent);

        float delta = totalTasks > 0 ? (completionPercent - 80f) : 0f;
        binding.tvCompletionDelta.setText((delta >= 0 ? "+" : "") + String.format(Locale.ENGLISH, "%.1f%%", delta));

        double focusHours = totalTasks > 0 ? totalTasks * 0.75 : 0.0;
        binding.tvFocusHours.setText(String.format(Locale.ENGLISH, "%.1f", focusHours));

        double avgPerDay = totalTasks > 0 ? focusHours / 7.0 : 0.0;
        binding.tvFocusAvgPill.setText(String.format(Locale.ENGLISH, "⏱ %.1fh / day", avgPerDay));

        String timeframeLabel = "Week".equals(selectedTimeframe) ? "this week" : ("Month".equals(selectedTimeframe) ? "this month" : "all time");
        binding.tvWeeklyCompletedCount.setText(completedTasks + " completed tasks " + timeframeLabel);

        List<DonutChartView.Slice> slices = new ArrayList<>();
        List<CategoryStat> statList = new ArrayList<>(categoryMap.values());

        for (CategoryStat stat : statList) {
            slices.add(new DonutChartView.Slice(stat.name, stat.color, stat.count));
        }

        binding.donutChart.setData(slices, totalTasks);
        updateCategoryLegend(statList, totalTasks);
        updateWeeklyOutputTrend();

        Set<String> completedDateKeys = new HashSet<>();
        SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (TaskModel task : allTasksList) {
            if (task != null && task.isCompleted() && task.getDate() != null && !task.getDate().trim().isEmpty()) {
                Calendar c = parseDate(task.getDate());
                if (c != null) {
                    completedDateKeys.add(dayKeyFormat.format(c.getTime()));
                }
            }
        }

        int streak = 0;
        if (!completedDateKeys.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            String todayKey = dayKeyFormat.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterdayKey = dayKeyFormat.format(cal.getTime());

            if (completedDateKeys.contains(todayKey)) {
                cal = Calendar.getInstance();
                while (completedDateKeys.contains(dayKeyFormat.format(cal.getTime()))) {
                    streak++;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
            } else if (completedDateKeys.contains(yesterdayKey)) {
                cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -1);
                while (completedDateKeys.contains(dayKeyFormat.format(cal.getTime()))) {
                    streak++;
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                }
            } else {
                streak = completedDateKeys.size();
            }
        }

        binding.tvStreakDaysBadge.setText(streak + "\nDays");
        binding.tvStreakSubtitle.setText(streak > 0 ? "You're on a " + streak + "-day streak! Keep going!" : "Complete a task today to start your streak!");
        final int finalStreak = streak;
        binding.btnStreakDetails.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Consistency details: " + finalStreak + " day active streak!", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateWeeklyOutputTrend() {
        if (binding == null || !isAdded()) return;

        int[] dayCounts = new int[7];
        Calendar now = Calendar.getInstance();
        int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
        int currentYear = now.get(Calendar.YEAR);

        for (TaskModel task : allTasksList) {
            if (task == null || !task.isCompleted()) continue;
            Calendar cal = parseDate(task.getDate());
            if (cal == null) continue;

            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek) {
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                int dayIdx;
                switch (dayOfWeek) {
                    case Calendar.MONDAY: dayIdx = 0; break;
                    case Calendar.TUESDAY: dayIdx = 1; break;
                    case Calendar.WEDNESDAY: dayIdx = 2; break;
                    case Calendar.THURSDAY: dayIdx = 3; break;
                    case Calendar.FRIDAY: dayIdx = 4; break;
                    case Calendar.SATURDAY: dayIdx = 5; break;
                    case Calendar.SUNDAY: dayIdx = 6; break;
                    default: dayIdx = -1; break;
                }
                if (dayIdx >= 0) {
                    dayCounts[dayIdx]++;
                }
            }
        }

        int max = 0;
        int peakIdx = -1;
        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            if (dayCounts[i] > max) {
                max = dayCounts[i];
                peakIdx = i;
            }
        }

        if (max > 0 && peakIdx >= 0) {
            binding.tvPeakDayBadge.setText("⚡ Peak: " + dayNames[peakIdx]);
        } else {
            binding.tvPeakDayBadge.setText("No activity yet");
        }

        View[] bars = {binding.barMon, binding.barTue, binding.barWed, binding.barThu, binding.barFri, binding.barSat, binding.barSun};
        TextView[] labels = {binding.lblMon, binding.lblTue, binding.lblWed, binding.lblThu, binding.lblFri, binding.lblSat, binding.lblSun};

        float density = getResources().getDisplayMetrics().density;
        float minHeightDp = 12f;
        float maxHeightDp = 90f;

        for (int i = 0; i < 7; i++) {
            float heightDp = max > 0 ? minHeightDp + ((float) dayCounts[i] / max) * (maxHeightDp - minHeightDp) : minHeightDp;
            ViewGroup.LayoutParams lp = bars[i].getLayoutParams();
            if (lp != null) {
                lp.height = (int) (heightDp * density);
                bars[i].setLayoutParams(lp);
            }

            if (max > 0 && i == peakIdx) {
                bars[i].setBackgroundResource(R.drawable.bg_bar_peak);
                labels[i].setTextColor(Color.parseColor("#C084FC"));
            } else {
                bars[i].setBackgroundResource(R.drawable.bg_bar_inactive);
                labels[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            }
        }

        if (max > 0 && peakIdx == 3) {
            binding.tvPeakBarNumber.setVisibility(View.VISIBLE);
            binding.tvPeakBarNumber.setText(String.valueOf(dayCounts[3]));
        } else {
            binding.tvPeakBarNumber.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedAvatar();
        loadTasksAnalytics();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
