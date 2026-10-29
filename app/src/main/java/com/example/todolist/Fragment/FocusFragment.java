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
import androidx.fragment.app.Fragment;

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
        displaySampleData();
        loadTasksAnalytics();
    }

    private void displaySampleData() {
        if (binding == null) return;

        binding.tvTotalTasks.setText("24");
        binding.tvPendingTasks.setText("8");

        binding.donutChart.loadDefaultSample();

        List<CategoryStat> sampleList = new ArrayList<>();
        sampleList.add(new CategoryStat("Work", Color.parseColor("#FF9680"), 7));
        sampleList.add(new CategoryStat("Grocery", Color.parseColor("#CCFF80"), 4));
        sampleList.add(new CategoryStat("Design", Color.parseColor("#80FFD9"), 5));
        sampleList.add(new CategoryStat("University", Color.parseColor("#809CFF"), 5));
        sampleList.add(new CategoryStat("Sport", Color.parseColor("#80FFFF"), 3));

        updateCategoryLegend(sampleList);

        binding.circularProgress.setProgress(67);
        binding.tvProgressFraction.setText("16 / 24");
        binding.progressBarHorizontal.setProgress(67);
        binding.tvProgressMessage.setText("Great work! Keep going!");

        binding.tvCompletedStat.setText("16");
        binding.tvPendingStat.setText("8");
        binding.tvTotalStat.setText("24");
        binding.tvFooterNote.setText("Illustrative UI preview · Sample data only");
    }

    private void updateCategoryLegend(List<CategoryStat> categories) {
        if (binding == null || !isAdded()) return;

        binding.layoutCategoryLegend.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (CategoryStat cat : categories) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (4 * density), 0, (int) (4 * density));

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
            tvName.setTextSize(13f);
            row.addView(tvName);

            // Task count
            TextView tvCount = new TextView(requireContext());
            tvCount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tvCount.setText(String.valueOf(cat.count));
            tvCount.setTextColor(Color.WHITE);
            tvCount.setTextSize(14f);
            tvCount.setTypeface(null, Typeface.BOLD);
            row.addView(tvCount);

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
                int progressPercent = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;

                // Update metric cards
                binding.tvTotalTasks.setText(String.valueOf(totalTasks));
                binding.tvPendingTasks.setText(String.valueOf(pendingTasks));

                // Update Donut Chart and legend with actual categories
                List<DonutChartView.Slice> slices = new ArrayList<>();
                List<CategoryStat> statList = new ArrayList<>(categoryMap.values());

                for (CategoryStat stat : statList) {
                    slices.add(new DonutChartView.Slice(stat.name, stat.color, stat.count));
                }

                binding.donutChart.setData(slices, totalTasks);
                updateCategoryLegend(statList);

                // Update Progress Ring & Bar
                binding.circularProgress.setProgress(progressPercent);
                binding.tvProgressFraction.setText(completedTasks + " / " + totalTasks);
                binding.progressBarHorizontal.setProgress(progressPercent);

                if (progressPercent == 100) {
                    binding.tvProgressMessage.setText("All tasks completed! Outstanding job!");
                } else if (progressPercent >= 50) {
                    binding.tvProgressMessage.setText("Great work! Keep going!");
                } else {
                    binding.tvProgressMessage.setText("Keep going! You can do it!");
                }

                // Update summary stats
                binding.tvCompletedStat.setText(String.valueOf(completedTasks));
                binding.tvPendingStat.setText(String.valueOf(pendingTasks));
                binding.tvTotalStat.setText(String.valueOf(totalTasks));
                binding.tvFooterNote.setText("Live productivity analytics from your tasks");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep current display
            }
        };

        tasksRef.addValueEventListener(tasksListener);
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
