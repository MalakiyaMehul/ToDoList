package com.example.todolist.Dialog;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.SubTaskBottomSheetBinding;
import com.example.todolist.model.TaskModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SubTaskBottomSheet {

    public interface OnSubTaskUpdatedListener {
        void onSubTaskUpdated(String updatedSubTaskJson);
    }

    public static class SubTaskItem {
        private String title;
        private boolean done;

        public SubTaskItem(String title, boolean done) {
            this.title = title;
            this.done = done;
        }

        public String getTitle() {
            return title;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    public static void show(Context context, TaskModel taskModel, OnSubTaskUpdatedListener listener) {
        if (context == null || taskModel == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        SubTaskBottomSheetBinding binding = SubTaskBottomSheetBinding.inflate(LayoutInflater.from(context));
        bottomSheetDialog.setContentView(binding.getRoot());

        if (bottomSheetDialog.getWindow() != null) {
            View bottomSheetInternal = bottomSheetDialog.getWindow().findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        }

        String taskTitle = taskModel.getTaskName();
        binding.txtSubTaskTitle.setText(taskTitle != null && !taskTitle.isEmpty() ? taskTitle : "Subtasks");

        List<SubTaskItem> subTaskList = parseSubTasks(taskModel.getSubTask());

        binding.recyclerSubTasks.setLayoutManager(new LinearLayoutManager(context));
        SubTaskAdapter adapter = new SubTaskAdapter(subTaskList, new SubTaskAdapter.OnSubTaskActionListener() {
            @Override
            public void onToggle(int position) {
                SubTaskItem item = subTaskList.get(position);
                item.setDone(!item.isDone());
                saveSubTasksToDatabase(context, taskModel, subTaskList, listener);
            }

            @Override
            public void onDelete(int position) {
                subTaskList.remove(position);
                saveSubTasksToDatabase(context, taskModel, subTaskList, listener);
            }
        });
        binding.recyclerSubTasks.setAdapter(adapter);

        updateEmptyState(binding, subTaskList);

        Runnable addSubTaskAction = () -> {
            String input = binding.edtSubTaskInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(context, "Please enter subtask title", Toast.LENGTH_SHORT).show();
                return;
            }
            subTaskList.add(new SubTaskItem(input, false));
            binding.edtSubTaskInput.setText("");
            adapter.notifyDataSetChanged();
            updateEmptyState(binding, subTaskList);
            saveSubTasksToDatabase(context, taskModel, subTaskList, listener);
        };

        binding.btnAddSubTask.setOnClickListener(v -> addSubTaskAction.run());

        binding.edtSubTaskInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSubTaskAction.run();
                return true;
            }
            return false;
        });

        bottomSheetDialog.show();
    }

    private static void updateEmptyState(SubTaskBottomSheetBinding binding, List<SubTaskItem> subTaskList) {
        if (subTaskList == null || subTaskList.isEmpty()) {
            binding.txtEmptySubTasks.setVisibility(View.VISIBLE);
            binding.recyclerSubTasks.setVisibility(View.GONE);
        } else {
            binding.txtEmptySubTasks.setVisibility(View.GONE);
            binding.recyclerSubTasks.setVisibility(View.VISIBLE);
        }
    }

    private static List<SubTaskItem> parseSubTasks(String rawSubTask) {
        List<SubTaskItem> list = new ArrayList<>();
        if (rawSubTask == null || rawSubTask.trim().isEmpty()) {
            return list;
        }

        try {
            JSONArray array = new JSONArray(rawSubTask.trim());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.optString("title", "");
                boolean done = obj.optBoolean("done", false);
                if (!title.isEmpty()) {
                    list.add(new SubTaskItem(title, done));
                }
            }
        } catch (Exception e) {
            // Fallback for line-separated or plain text
            String[] lines = rawSubTask.split("\n");
            for (String line : lines) {
                String clean = line.trim();
                if (!clean.isEmpty()) {
                    boolean done = clean.startsWith("[x]") || clean.startsWith("✔");
                    clean = clean.replace("[x]", "").replace("[ ]", "").replace("✔", "").trim();
                    if (!clean.isEmpty()) {
                        list.add(new SubTaskItem(clean, done));
                    }
                }
            }
        }
        return list;
    }

    private static void saveSubTasksToDatabase(Context context, TaskModel taskModel, List<SubTaskItem> subTaskList, OnSubTaskUpdatedListener listener) {
        JSONArray array = new JSONArray();
        for (SubTaskItem item : subTaskList) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("title", item.getTitle());
                obj.put("done", item.isDone());
                array.put(obj);
            } catch (Exception ignored) {}
        }

        String jsonString = array.toString();
        taskModel.setSubTask(jsonString);

        if (listener != null) {
            listener.onSubTaskUpdated(jsonString);
        }

        if (taskModel.getTaskId() != null && !taskModel.getTaskId().isEmpty()) {
            String token = SharedPrefManager.getInstance(context).getString("access_token");
            if (token.isEmpty()) {
                token = SupabaseAuth.getAccessToken(context);
            }
            new SupabaseRepository().updateTaskSubTask(taskModel.getTaskId(), token, jsonString, null);
        }
    }

    private static class SubTaskAdapter extends RecyclerView.Adapter<SubTaskAdapter.ViewHolder> {

        interface OnSubTaskActionListener {
            void onToggle(int position);
            void onDelete(int position);
        }

        private final List<SubTaskItem> items;
        private final OnSubTaskActionListener listener;

        SubTaskAdapter(List<SubTaskItem> items, OnSubTaskActionListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_task, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SubTaskItem item = items.get(position);
            holder.txtTitle.setText(item.getTitle());

            holder.radioCompleted.setOnCheckedChangeListener(null);
            holder.radioCompleted.setChecked(item.isDone());

            if (item.isDone()) {
                holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.txtTitle.setAlpha(0.6f);
            } else {
                holder.txtTitle.setPaintFlags(holder.txtTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.txtTitle.setAlpha(1.0f);
            }

            holder.radioCompleted.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    listener.onToggle(adapterPos);
                    notifyItemChanged(adapterPos);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    listener.onDelete(adapterPos);
                    notifyItemRemoved(adapterPos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton radioCompleted;
            TextView txtTitle;
            ImageView btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                radioCompleted = itemView.findViewById(R.id.radioSubTaskCompleted);
                txtTitle = itemView.findViewById(R.id.txtSubTaskTitle);
                btnDelete = itemView.findViewById(R.id.btnDeleteSubTask);
            }
        }
    }
}
