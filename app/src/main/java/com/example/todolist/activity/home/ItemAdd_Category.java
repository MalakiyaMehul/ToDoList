package com.example.todolist.activity.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.todolist.R;
import com.example.todolist.adapter.CategoryAdapter;
import com.example.todolist.databinding.DialogAddCategoryBinding;
import com.example.todolist.model.CategoryModel;

import java.util.ArrayList;

public class ItemAdd_Category extends DialogFragment {
    private DialogAddCategoryBinding binding;
    private CategoryAdapter adapter;
    private String selectedCategory = "";
    private CategoryModel selectedCategoryModel;
    public static final ArrayList<CategoryModel> categoryList = new ArrayList<>();

    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
    {
        Dialog dialog = new Dialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(true);
        binding = DialogAddCategoryBinding.bind(view);

        binding.recyclerCategories.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        categoryList.add(new CategoryModel("Grocery",R.drawable.bread,"#CCFF80"));
        categoryList.add(new CategoryModel("Work",R.drawable.briefcase,"#FF9680"));
        categoryList.add(new CategoryModel("Sport",R.drawable.sport,"#80FFFF"));
        categoryList.add(new CategoryModel("Design",R.drawable.design,"#80FFD9"));
        categoryList.add(new CategoryModel("University",R.drawable.university,"#809CFF"));
        categoryList.add(new CategoryModel("Social",R.drawable.megaphone,"#FF80EB"));
        categoryList.add(new CategoryModel("Music",R.drawable.music,"#FC80FF"));
        categoryList.add(new CategoryModel("Heart",R.drawable.heart,"#80FFA3"));
        categoryList.add(new CategoryModel("Movie",R.drawable.camera,"#80D1FF"));
        categoryList.add(new CategoryModel("Home",R.drawable.home,"#FFCC80"));

        adapter = new CategoryAdapter(categoryList, category -> {
            selectedCategoryModel = category;
            selectedCategory = category.getCategoryName();
        });
        binding.recyclerCategories.setAdapter(adapter);

        binding.AddCategory.setOnClickListener(v -> {
            if (selectedCategoryModel == null) {
                Toast.makeText(requireContext(),
                        "Please select a category",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle result = new Bundle();

            result.putString("selectedCategory",
                    selectedCategoryModel.getCategoryName());

            result.putString("selectedCategoryColor",
                    selectedCategoryModel.getColor());

            result.putInt("selectedCategoryIcon",
                    selectedCategoryModel.getIcon());

            getParentFragmentManager().setFragmentResult(
                    "category_selected", result
            );

            dismiss();
        });
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setDimAmount(0.65f);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setGravity(Gravity.CENTER);
        return dialog;
    }
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();

        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.90), WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.CENTER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
