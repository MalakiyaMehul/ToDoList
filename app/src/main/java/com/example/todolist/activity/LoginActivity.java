package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.todolist.InputField;
import com.example.todolist.R;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivityLoginBinding;
import com.google.gson.JsonObject;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InputField usernameField = binding.txtUserLogin;
        InputField passwordField = binding.edtPassword;

        usernameField.setLabel("Email or Username");
        usernameField.setHint("elena@designcraft.io");
        passwordField.setLabel("Password");
        passwordField.setHint("● ● ● ● ● ● ● ● ● ●");

        int passwordType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        passwordField.setInputType(passwordType);

        final boolean[] isRememberMeChecked = {true};
        binding.checkRememberMe.setOnClickListener(v -> {
            isRememberMeChecked[0] = !isRememberMeChecked[0];
            binding.checkRememberMe.setBackgroundResource(
                    isRememberMeChecked[0] ? R.drawable.bg_checkbox_checked : R.drawable.bg_card_input
            );
        });

        binding.txtForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Password recovery feature", Toast.LENGTH_SHORT).show();
        });

        binding.GoogleWithLogin.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In", Toast.LENGTH_SHORT).show();
        });
        binding.txtRegistration.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.txtUserLogin.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = username.contains("@")
                    ? username
                    : username.replaceAll("\\s+", "").toLowerCase() + "@todolist.com";

            binding.btnLogin.setEnabled(false);

            SupabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(JsonObject response) {
                    String savedUsername = username.contains("@")
                            ? username.substring(0, username.indexOf("@"))
                            : username;

                    SupabaseAuth.saveSession(
                            LoginActivity.this,
                            response,
                            savedUsername
                    );

                    runOnUiThread(() -> {
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
