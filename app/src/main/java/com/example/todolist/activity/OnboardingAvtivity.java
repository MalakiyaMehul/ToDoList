package com.example.todolist.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.adapter.OnboardingAdapter;
import com.example.todolist.databinding.ActivityOnboardingBinding;
import com.example.todolist.model.OnboardingModel;

import java.util.ArrayList;

public class OnboardingAvtivity extends BaseActivity {

    private ActivityOnboardingBinding binding;
    private ArrayList<OnboardingModel> list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        boolean hasRegistered = preferences.getBoolean("hasRegistered", false);

        if (hasRegistered) {
            startActivity(new Intent(OnboardingAvtivity.this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        list = new ArrayList<>();
        list.add(new OnboardingModel(R.drawable.group_182_viewpager, "Manage your task", "You can easily manage all of your daily tasks in DoMe for free"));
        list.add(new OnboardingModel(R.drawable.frame_162_viewpager, "Create daily routine", "In Uptodo  you can create your personalized routine to stay productive"));
        list.add(new OnboardingModel(R.drawable.group_182_viewpager, "Orgonaize your tasks", "You can organize your daily tasks by adding your tasks into separate categories"));

        OnboardingAdapter adapter = new OnboardingAdapter(list);
        binding.viewPager.setAdapter(adapter);
        binding.btnNext.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < list.size() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1);
            } else {
                Intent intent = new Intent(OnboardingAvtivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
        });
        binding.txtBack.setOnClickListener(v -> {
            int currentPage = binding.viewPager.getCurrentItem();
            if (currentPage > 0) {
                binding.viewPager.setCurrentItem(currentPage - 1, true);
            }
        });
        binding.txtSkip.setOnClickListener(v -> {
            Intent intent = new Intent(OnboardingAvtivity.this, StartActivity.class);
            startActivity(intent);
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            public void onPageSelected(int position) {
                if (position == 0) {
                    binding.txtBack.setVisibility(View.VISIBLE);
                    binding.btnNext.setText("NEXT");
                } else if (position == 1) {
                    binding.txtBack.setVisibility(View.VISIBLE);
                    binding.btnNext.setText("NEXT");
                } else if (position == 2) {
                    binding.txtBack.setVisibility(View.VISIBLE);
                    binding.btnNext.setText("GET STARTED");
                }
            }
        });
    }
}