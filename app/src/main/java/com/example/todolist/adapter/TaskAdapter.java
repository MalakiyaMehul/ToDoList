package com.example.todolist.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.databinding.TaskCardBinding;
import com.example.todolist.model.CategoryModel;
import com.example.todolist.model.TaskModel;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final ArrayList<TaskModel> taskModels;

    public TaskAdapter(ArrayList<TaskModel> taskModels) {
        this.taskModels = taskModels;
    }

    @NonNull
    @Override
    public TaskAdapter.TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TaskCardBinding binding = TaskCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel taskModel = taskModels.get(position);
        TaskCardBinding binding = holder.binding;
        binding.txtTitle.setText(taskModel.getTaskName());
        binding.txtTime.setText(taskModel.getTime());
        binding.categoryInclude.txtCategory.setText(taskModel.getCategory());
        binding.priorityInclude.priority.setText(taskModel.getPriority());
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
                holder.itemView.getLayoutParams();

        int spaceInDp = (int) (20 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
        params.bottomMargin = spaceInDp;
        holder.itemView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return taskModels.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TaskCardBinding binding;

        public TaskViewHolder(TaskCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
