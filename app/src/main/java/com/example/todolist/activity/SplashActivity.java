package com.example.todolist.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;

import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private final Handler navHandler = new Handler(Looper.getMainLooper());
    private Runnable autoNavRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(binding.progressSyncing, "progress", 10, 100);
        progressAnimator.setDuration(1200);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();

        if (SupabaseAuth.isLoggedIn(this)) {

            autoNavRunnable = () -> {

                if (!isFinishing()) {

                    Intent intent = new Intent(
                            SplashActivity.this,
                            HomeActivity.class
                    );

                    startActivity(intent);
                    finish();
                }
            };

            navHandler.postDelayed(autoNavRunnable, 1600);
        }

        binding.btnGetStarted.setOnClickListener(v -> {
            cancelAutoNav();
            Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        binding.btnSignIn.setOnClickListener(v -> {
            cancelAutoNav();
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void cancelAutoNav() {
        if (autoNavRunnable != null) {
            navHandler.removeCallbacks(autoNavRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        cancelAutoNav();
        super.onDestroy();
    }
}
