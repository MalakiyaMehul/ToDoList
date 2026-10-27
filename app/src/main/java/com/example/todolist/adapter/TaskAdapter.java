        package com.example.todolist.adapter;

        import android.graphics.Color;
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
            public interface OnTaskClickListener {
                void onTaskClick(TaskModel taskModel);
            }

            private final OnTaskClickListener listener;

            public TaskAdapter(ArrayList<TaskModel> taskModels) {
                this(taskModels, null);
            }

            public TaskAdapter(ArrayList<TaskModel> taskModels, OnTaskClickListener listener) {
                this.taskModels = taskModels;
                this.listener = listener;
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
                String categoryColor = taskModel.getCategoryColor();

                if (categoryColor != null && !categoryColor.trim().isEmpty()) {
                    try {
                        int color = Color.parseColor(categoryColor);
                        binding.categoryInclude.bgcolor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                    } catch (IllegalArgumentException e) {
                        binding.categoryInclude.bgcolor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                    }
                }
                String categoryIcon = taskModel.getCategoryIcon();
                if (categoryIcon != null && !categoryIcon.trim().isEmpty()) {
                    try {
                        binding.categoryInclude.imgCategory.setImageResource(Integer.parseInt(categoryIcon));
                    } catch (NumberFormatException e) {
                        binding.categoryInclude.imgCategory.setImageResource(R.drawable.university);
                    }
                }
                binding.priorityInclude.priority.setText(taskModel.getPriority());

                ViewGroup.LayoutParams layoutParams =
                        holder.itemView.getLayoutParams();
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) layoutParams;
                    int spaceInDp = (int) (20 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
                    params.bottomMargin = spaceInDp;
                    holder.itemView.setLayoutParams(params);
                }
                binding.radioCompleted.setOnCheckedChangeListener(null);
                binding.radioCompleted.setChecked(taskModel.isCompleted());
                binding.radioCompleted.setOnClickListener(v -> {
                    boolean newStatus = !taskModel.isCompleted();
                    binding.radioCompleted.setChecked(newStatus);
                    taskModel.setCompleted(newStatus);
                    com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && taskModel.getTaskId() != null) {
                        com.google.firebase.database.DatabaseReference taskRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                                .getReference("Users")
                                .child(user.getUid())
                                .child("Tasks")
                                .child(taskModel.getTaskId());
                        taskRef.child("completed").setValue(newStatus);
                    }
                });
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTaskClick(taskModel);
                    }
                });
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
