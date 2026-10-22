package com.example.todolist.activity.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.R;
import com.example.todolist.databinding.DialogAddPriorityBinding;

public class DialogAddPriority extends DialogFragment {
    private DialogAddPriorityBinding binding;
    private int selectedPriority = 1;
    private View selectedView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddPriorityBinding.inflate(getLayoutInflater());

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(binding.getRoot());
        GridLayout priorityGrid = binding.PriorityGrid;

        priorityGrid.removeAllViews();
        for (int i = 1; i <= 10; i++) {
            final int priority = i;
            LinearLayout item = new LinearLayout(requireContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setBackgroundResource(R.drawable.bg_priority_item);

            ImageView flag = new ImageView(requireContext());
            flag.setImageResource(R.drawable.flag);
            flag.setColorFilter(android.graphics.Color.WHITE);

            LinearLayout.LayoutParams flagParams = new LinearLayout.LayoutParams(dp(24), dp(24));

            item.addView(flag, flagParams);
            TextView number = new TextView(requireContext());
            number.setText(String.valueOf(priority));
            number.setTextColor(android.graphics.Color.WHITE);
            number.setGravity(Gravity.CENTER);
            number.setTextSize(16);
            item.addView(number);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(90);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(8), dp(8), dp(8), dp(8));
            item.setLayoutParams(params);
            if (priority == selectedPriority) {
                selectedView = item;
                item.setSelected(true);
            }
            item.setOnClickListener(v -> {
                if (selectedView != null) {
                    selectedView.setSelected(false);
                }
                selectedPriority = priority;
                item.setSelected(true);
                selectedView = item;

                binding.btnSave.setOnClickListener(view -> {
                    if (listener != null) {
                        listener.onPrioritySelected(selectedPriority);
                    }
                    dismiss();
                });
                binding.btnCancel.setOnClickListener(view -> {
                    dismiss();
                });

            });
            priorityGrid.addView(item);
        }
        return dialog;
    }

    public interface OnPrioritySelectedListener {
        void onPrioritySelected(int priority);
    }

    private DialogAddPriority.OnPrioritySelectedListener listener;

    public void setOnPrioritySelectedListener(DialogAddPriority.OnPrioritySelectedListener listener) {
        this.listener = listener;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setDimAmount(0.65f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.95),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
