package com.example.todolist.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.todolist.R;
import com.example.todolist.activity.home.ItemAdd_Category;
import com.example.todolist.databinding.ActivityCreateCategoryBinding;
import com.example.todolist.model.CategoryModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class activity_create_category extends BaseActivity {
    private ActivityCreateCategoryBinding binding;
    private LinearLayout colorContainer;
    private String selectedIcon = "🏠";
    private String selectedColor = "#C4D82E";
    private View selectedColorView;
    private final String[] colors = {"#C4D82E", "#68C943", "#36C6A3", "#36A9E1", "#3486D8", "#D68B42", "#C43CB8", "#E84C8A"};

    private final String[] icons = {
            "R.drawable.university",
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreateCategoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.txtSelectedIcon.setVisibility(View.GONE);
        colorContainer = binding.colorContainer;
        addColorOptions();

        binding.btnChooseIcon.setOnClickListener(v -> showIconPicker());

        binding.btnCancel.setOnClickListener(v -> finish());

        binding.btnCreateCategory.setOnClickListener(v -> saveCategory());
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    private void addColorOptions() {
        for (String color : colors) {
            TextView circle = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(28), dp(28));
            params.setMargins(dp(4), dp(0), dp(4), dp(0));
            circle.setLayoutParams(params);
            circle.setGravity(Gravity.CENTER);

            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(android.graphics.Color.parseColor(color));
            circle.setBackground(background);

            circle.setText("");
            circle.setOnClickListener(v -> {
                selectedColor = color;
                if (selectedColorView != null) {
                    ((TextView) selectedColorView).setText("");
                }
                circle.setText("✓");
                circle.setTextColor(Color.WHITE);
                circle.setTextSize(14);
                selectedColorView = circle;
            });
            colorContainer.addView(circle);
            if (color.equals(selectedColor)) {
                circle.setText("✓");
                circle.setTextColor(Color.WHITE);
                selectedColorView = circle;
            }
        }
    }

    private void showIconPicker() {
        LinearLayout iconLayout = new LinearLayout(this);
        iconLayout.setOrientation(LinearLayout.VERTICAL);
        iconLayout.setPadding(dp(16), dp(8), dp(16), dp(8));
        binding.btnChooseIcon.setVisibility(View.GONE);
        binding.txtSelectedIcon.setVisibility(View.VISIBLE);

        final TextView[] selected = {null};
        for (int start = 0; start < icons.length; start += 4) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int i = start; i < Math.min(start + 4, icons.length); i++) {
                final String icon = icons[i];
                TextView iconView = new TextView(this);
                iconView.setText(icon);
                iconView.setTextSize(28);
                iconView.setTextColor(Color.WHITE);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), 1);

                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                iconView.setLayoutParams(params);
                iconView.setBackgroundColor(Color.rgb(45, 45, 45));

                iconView.setOnClickListener(v -> {
                    selectedIcon = icon;
                    binding.txtSelectedIcon.setText(icon);
                    if (selected[0] != null) {
                        selected[0].setBackgroundColor(Color.rgb(45, 45, 45));
                    }
                    iconView.setBackgroundColor(Color.rgb(100, 90, 200));
                    selected[0] = iconView;
                });
                row.addView(iconView);
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose Category Icon")
                .setView(iconLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Select", (dialog, which) -> {
                    int iconResId = getResources().getIdentifier(
                            selectedIcon, "drawable", getPackageName());

                    if (iconResId != 0) {
                        binding.txtSelectedIcon.setText("");
                        binding.txtSelectedIcon.setCompoundDrawablesWithIntrinsicBounds(
                                null, null, null, null);
                        binding.txtSelectedIcon.setBackgroundResource(iconResId);
                        binding.txtSelectedIcon.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }
    private void saveCategory() {

        String name = binding.edtCategoryName.getText().toString().trim();
        CategoryModel category = new CategoryModel(name, selectedIcon, selectedColor);
        ItemAdd_Category.categoryList.add(category);
        saveCategoryToFirebase(name, selectedIcon, selectedColor);

        android.widget.Toast.makeText(this, "Category added successfully", android.widget.Toast.LENGTH_SHORT).show();
        finish();
    }
    private void saveCategoryToFirebase(String name, String icon, String color) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User is not logged in with Firebase Authentication", Toast.LENGTH_LONG).show();
            return;
        }
        DatabaseReference categoryRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("Categories");
        String categoryId = categoryRef.push().getKey();
        if (categoryId == null) {
            Toast.makeText(this, "Unable to create category ID", Toast.LENGTH_SHORT).show();
            return;
        }
        CategoryModel category = new CategoryModel(name, icon, color);

        categoryRef.child(categoryId).setValue(category).addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Category saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(error ->
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }
}