package com.example.todolist.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;

import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivitySplashBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private final Handler navHandler = new Handler(Looper.getMainLooper());
    private Runnable autoNavRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Smooth neural index progress animation
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(binding.progressSyncing, "progress", 10, 100);
        progressAnimator.setDuration(1200);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();

        // If user already logged in with Firebase, automatically transition into Home
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            autoNavRunnable = () -> {
                if (!isFinishing()) {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                    finish();
                }
            };
            navHandler.postDelayed(autoNavRunnable, 1600);
        }

        // Primary Action: Get Started -> Register
        binding.btnGetStarted.setOnClickListener(v -> {
            cancelAutoNav();
            Intent intent = new Intent(SplashActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Secondary Action: Existing Account -> Login
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
