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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.todolist.R;
import com.example.todolist.databinding.FragmentFocusBinding;
import com.example.todolist.model.TaskModel;
import com.example.todolist.view.DonutChartView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private DatabaseReference tasksRef;
    private ValueEventListener tasksListener;
    private String selectedTimeframe = "Week";

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
        displaySampleData();
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
    }

    private void setupActions() {
        binding.btnSearchTop.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Search analytics...", Toast.LENGTH_SHORT).show()
        );

        binding.btnHeaderTelemetry.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Telemetry stream live", Toast.LENGTH_SHORT).show()
        );

        binding.btnCategoryMenu.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Category options", Toast.LENGTH_SHORT).show()
        );

        binding.btnStreakDetails.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Consistency details: 5 day active streak!", Toast.LENGTH_SHORT).show()
        );
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
                    binding.ivFocusAvatar.setImageURI(null);
                    binding.ivFocusAvatar.setImageURI(android.net.Uri.fromFile(file));
                }
            } catch (Exception ignored) {}
        }
    }

    private void displaySampleData() {
        if (binding == null) return;

        binding.tvTotalTasks.setText("48");
        binding.tvPendingTasks.setText("7");
        binding.tvCompletionPercent.setText("85.4%");
        binding.progressBarCompletion.setProgress(85);
        binding.tvCompletionDelta.setText("+5.4%");
        binding.tvFocusHours.setText("14.2");
        binding.tvWeeklyCompletedCount.setText("41 completed tasks this week");

        binding.donutChart.loadDefaultSample();

        List<CategoryStat> sampleList = new ArrayList<>();
        sampleList.add(new CategoryStat("Work", Color.parseColor("#8B5CF6"), 22));
        sampleList.add(new CategoryStat("Design", Color.parseColor("#C084FC"), 12));
        sampleList.add(new CategoryStat("Personal", Color.parseColor("#06B6D4"), 9));
        sampleList.add(new CategoryStat("Health", Color.parseColor("#10B981"), 5));

        updateCategoryLegend(sampleList, 48);
    }

    private void updateCategoryLegend(List<CategoryStat> categories, int totalCount) {
        if (binding == null || !isAdded()) return;

        binding.layoutCategoryLegend.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (CategoryStat cat : categories) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (6 * density), 0, (int) (6 * density));

            // Category color dot
            View dot = new View(requireContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams((int) (8 * density), (int) (8 * density));
            dot.setLayoutParams(dotParams);
            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(cat.color);
            dot.setBackground(dotDrawable);
            row.addView(dot);

            // Category name
            TextView tvName = new TextView(requireContext());
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.setMarginStart((int) (10 * density));
            tvName.setLayoutParams(nameParams);
            tvName.setText(cat.name);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(14f);
            tvName.setTypeface(null, Typeface.BOLD);
            row.addView(tvName);

            // Task count (e.g. "22 tasks")
            TextView tvCount = new TextView(requireContext());
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvCount.setText(cat.count + (cat.count == 1 ? " task" : " tasks"));
            tvCount.setTextColor(Color.parseColor("#94A3B8"));
            tvCount.setTextSize(13f);
            tvCount.setPadding(0, 0, (int) (14 * density), 0);
            row.addView(tvCount);

            // Percentage (e.g. "45%")
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        tasksRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("Tasks");

        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null || !isAdded()) return;

                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    displaySampleData();
                    return;
                }

                int totalTasks = 0;
                int completedTasks = 0;

                // Category name -> CategoryStat
                Map<String, CategoryStat> categoryMap = new LinkedHashMap<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    TaskModel task = child.getValue(TaskModel.class);
                    if (task == null) continue;

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

                // Update metric cards
                binding.tvTotalTasks.setText(String.valueOf(totalTasks));
                binding.tvPendingTasks.setText(String.valueOf(pendingTasks));

                binding.tvCompletionPercent.setText(String.format(Locale.ENGLISH, "%.1f%%", completionPercent));
                binding.progressBarCompletion.setProgress((int) completionPercent);

                float delta = completionPercent - 80f;
                binding.tvCompletionDelta.setText((delta >= 0 ? "+" : "") + String.format(Locale.ENGLISH, "%.1f%%", delta));

                double focusHours = totalTasks * 0.75;
                binding.tvFocusHours.setText(String.format(Locale.ENGLISH, "%.1f", focusHours));

                binding.tvWeeklyCompletedCount.setText(completedTasks + " completed tasks this week");

                // Update Donut Chart and legend with actual categories
                List<DonutChartView.Slice> slices = new ArrayList<>();
                List<CategoryStat> statList = new ArrayList<>(categoryMap.values());

                for (CategoryStat stat : statList) {
                    slices.add(new DonutChartView.Slice(stat.name, stat.color, stat.count));
                }

                binding.donutChart.setData(slices, totalTasks);
                updateCategoryLegend(statList, totalTasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep current display
            }
        };

        tasksRef.addValueEventListener(tasksListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedAvatar();
    }

    @Override
    public void onDestroyView() {
        if (tasksRef != null && tasksListener != null) {
            tasksRef.removeEventListener(tasksListener);
            tasksListener = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
