package com.example.todolist.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
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
    private int selectedIcon;
    private String selectedColor = "#C4D82E";
    private View selectedColorView;
    private final String[] colors = {"#C4D82E", "#68C943", "#36C6A3", "#36A9E1", "#3486D8", "#D68B42", "#C43CB8", "#E84C8A"};

    private final int[] icons = {R.drawable.bread, R.drawable.briefcase, R.drawable.design,R.drawable.university,R.drawable.megaphone,R.drawable.music,R.drawable.heart,R.drawable.camera,R.drawable.home};

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

        final ImageView[] selectedView = {null};
        final int[] chosenIcon = {selectedIcon};

        for (int start = 0; start < icons.length; start += 4) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);

            for (int i = start;
                 i < Math.min(start + 4, icons.length); i++) {
                final int icon = icons[i];
                ImageView iconView = new ImageView(this);
                iconView.setImageResource(icon);
                iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                iconView.setPadding(dp(8), dp(8), dp(8), dp(8));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), 1);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                iconView.setLayoutParams(params);
                iconView.setBackgroundColor(Color.rgb(45, 45, 45));
                if (icon == selectedIcon) {
                    iconView.setBackgroundColor(Color.rgb(100, 90, 200));
                    selectedView[0] = iconView;
                }
                iconView.setOnClickListener(v -> {
                    chosenIcon[0] = icon;
                    if (selectedView[0] != null) {
                        selectedView[0].setBackgroundColor(
                                Color.rgb(45, 45, 45));
                    }
                    iconView.setBackgroundColor(
                            Color.rgb(100, 90, 200));
                    selectedView[0] = iconView;
                });
                row.addView(iconView);
            }
            iconLayout.addView(row);
        }
        new AlertDialog.Builder(this)
                .setTitle("Choose Category Icon")
                .setView(iconLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Select", (dialog, which) -> {
                    selectedIcon = chosenIcon[0];
                    binding.txtSelectedIcon.setText("");
                    binding.txtSelectedIcon.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(selectedIcon), null, null);
                    binding.btnChooseIcon.setVisibility(View.VISIBLE);
                    binding.txtSelectedIcon.setVisibility(View.VISIBLE);
                })
                .show();
    }
    private void saveCategory() {
        String name = binding.edtCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.edtCategoryName.setError("Enter category name");
            return;
        }
        saveCategoryToFirebase(name, selectedIcon, selectedColor);
    }
    private void saveCategoryToFirebase(String name, int icon, String color) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .child("Categories");

        String categoryId = categoryRef.push().getKey();
        CategoryModel category = new CategoryModel(name, icon, color);
        categoryRef.child(categoryId).setValue(category).addOnSuccessListener(unused -> {
            ItemAdd_Category.categoryList.add(category);
            Toast.makeText(this, "Category saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(error ->
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }
}
