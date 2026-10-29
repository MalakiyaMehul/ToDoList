package com.example.todolist.activity.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.todolist.R;
import com.example.todolist.ViewPagerAdapter;
import com.example.todolist.activity.BaseActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Intent;
import com.example.todolist.activity.CreateTaskActivity;
import com.example.todolist.databinding.ActivityHomeBinding;
import com.google.android.material.tabs.TabLayout;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.BottomAppBar, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        binding.bottomNavigationView.setItemActiveIndicatorEnabled(false);
        binding.fabAdd.setOnClickListener(v -> {
            try {
                startActivity(new Intent(HomeActivity.this, CreateTaskActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        binding.fabAdd.setOnLongClickListener(v -> {
            AddTaskBottomSheet sheet = new AddTaskBottomSheet();
            sheet.show(getSupportFragmentManager(), "AddTaskBottomSheet");
            return true;
        });
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_index) {
                binding.viewPager.setCurrentItem(0);
            } else if (id == R.id.nav_calendar) {
                binding.viewPager.setCurrentItem(1);
            } else if (id == R.id.nav_focus) {
                binding.viewPager.setCurrentItem(2);
            } else if (id == R.id.nav_profile) {
                binding.viewPager.setCurrentItem(3);
            }
            return true;
        });
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        binding.bottomNavigationView.setSelectedItemId(R.id.nav_index);
                        break;
                    case 1:
                        binding.bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
                        break;
                    case 2:
                        binding.bottomNavigationView.setSelectedItemId(R.id.nav_focus);
                        break;
                    case 3:
                        binding.bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });

    }
}