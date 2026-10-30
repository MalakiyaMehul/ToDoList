package com.example.todolist.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.databinding.ItemCalendarTimelineTaskBinding;
import com.example.todolist.model.TaskModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.CalendarViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel taskModel);
    }

    public interface OnTaskStatusChangeListener {
        void onStatusChange();
    }

    private final ArrayList<TaskModel> taskList;
    private final OnTaskClickListener clickListener;
    private final OnTaskStatusChangeListener statusChangeListener;

    public CalendarTaskAdapter(ArrayList<TaskModel> taskList, OnTaskClickListener clickListener) {
        this(taskList, clickListener, null);
    }

    public CalendarTaskAdapter(ArrayList<TaskModel> taskList, OnTaskClickListener clickListener, OnTaskStatusChangeListener statusChangeListener) {
        this.taskList = taskList;
        this.clickListener = clickListener;
        this.statusChangeListener = statusChangeListener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarTimelineTaskBinding binding = ItemCalendarTimelineTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CalendarViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        TaskModel task = taskList.get(position);
        ItemCalendarTimelineTaskBinding binding = holder.binding;

        // Title
        binding.txtTitle.setText(task.getTaskName() != null ? task.getTaskName() : "Untitled Task");

        // Time with formatted duration
        String timeStr = task.getTime();
        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = "Scheduled";
        } else if (!timeStr.contains("•") && !timeStr.contains("⏱")) {
            timeStr = timeStr + " • ⏱ 45m";
        }
        binding.txtTime.setText(timeStr);

        // Priority styling
        String priority = task.getPriority() != null ? task.getPriority().trim() : "Medium";
        if (priority.equalsIgnoreCase("High") || priority.equals("1")) {
            binding.txtPriority.setText("High Priority");
            binding.txtPriority.setTextColor(Color.parseColor("#F43F5E"));
            binding.txtPriority.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#381A24")));
        } else if (priority.equalsIgnoreCase("Low") || priority.equals("3")) {
            binding.txtPriority.setText("Low Priority");
            binding.txtPriority.setTextColor(Color.parseColor("#94A3B8"));
            binding.txtPriority.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F2430")));
        } else {
            binding.txtPriority.setText("Medium Priority");
            binding.txtPriority.setTextColor(Color.parseColor("#C084FC"));
            binding.txtPriority.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#291C3D")));
        }

        // Category Chip
        String category = task.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = "General";
        }
        binding.txtCategory.setText(category);

        String catColor = task.getCategoryColor();
        if (catColor != null && !catColor.trim().isEmpty()) {
            try {
                int c = Color.parseColor(catColor.trim());
                binding.dotCategory.setTextColor(c);
                binding.txtCategory.setTextColor(c);
            } catch (Exception ignored) {
                binding.dotCategory.setTextColor(Color.parseColor("#C084FC"));
                binding.txtCategory.setTextColor(Color.parseColor("#C084FC"));
            }
        } else {
            binding.dotCategory.setTextColor(Color.parseColor("#C084FC"));
            binding.txtCategory.setTextColor(Color.parseColor("#C084FC"));
        }

        // Subtag Chip (Description or location/members context)
        String desc = task.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            binding.layoutSubTag.setVisibility(View.VISIBLE);
            String displayDesc = desc.trim();
            if (displayDesc.length() > 20) {
                displayDesc = displayDesc.substring(0, 18) + "...";
            }
            binding.txtSubTag.setText(displayDesc);

            // Icon heuristic
            if (displayDesc.toLowerCase().contains("room") || displayDesc.toLowerCase().contains("office") || displayDesc.toLowerCase().contains("gym")) {
                binding.imgSubTag.setImageResource(R.drawable.ic_pin_location);
            } else {
                binding.imgSubTag.setImageResource(R.drawable.ic_group_users);
            }
        } else {
            // Default context if category is Work or Fitness
            if (category.equalsIgnoreCase("Work")) {
                binding.layoutSubTag.setVisibility(View.VISIBLE);
                binding.txtSubTag.setText("4 devs");
                binding.imgSubTag.setImageResource(R.drawable.ic_group_users);
            } else if (category.equalsIgnoreCase("Fitness") || category.equalsIgnoreCase("Health")) {
                binding.layoutSubTag.setVisibility(View.VISIBLE);
                binding.txtSubTag.setText("Workout");
                binding.imgSubTag.setImageResource(R.drawable.ic_pin_location);
            } else {
                binding.layoutSubTag.setVisibility(View.GONE);
            }
        }

        // Timeline Node & Connecting Line
        boolean isLast = (position == getItemCount() - 1);
        binding.viewTimelineLine.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);

        // Completion status UI
        boolean completed = task.isCompleted();
        if (completed) {
            binding.btnCheckTask.setBackgroundResource(R.drawable.bg_timeline_checkbox_checked);
            binding.imgCheckMark.setColorFilter(Color.WHITE);
            binding.txtTitle.setPaintFlags(binding.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.txtTitle.setTextColor(Color.parseColor("#71717A"));
            binding.viewTimelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_inactive);
        } else {
            binding.btnCheckTask.setBackgroundResource(R.drawable.bg_timeline_checkbox);
            binding.imgCheckMark.setColorFilter(Color.parseColor("#4B4D5E"));
            binding.txtTitle.setPaintFlags(binding.txtTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            binding.txtTitle.setTextColor(Color.WHITE);
            binding.viewTimelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_active);
        }

        // Checkbox click listener to toggle Firebase completed state
        binding.btnCheckTask.setOnClickListener(v -> {
            boolean newStatus = !task.isCompleted();
            task.setCompleted(newStatus);
            notifyItemChanged(holder.getAdapterPosition());

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && task.getTaskId() != null) {
                DatabaseReference taskRef = FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(user.getUid())
                        .child("Tasks")
                        .child(task.getTaskId());
                taskRef.child("completed").setValue(newStatus);
            }

            if (statusChangeListener != null) {
                statusChangeListener.onStatusChange();
            }
        });

        // Click on task card to edit
        binding.layoutCard.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        final ItemCalendarTimelineTaskBinding binding;

        public CalendarViewHolder(ItemCalendarTimelineTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

