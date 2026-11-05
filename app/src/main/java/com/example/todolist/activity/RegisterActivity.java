package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todolist.InputField;
import com.example.todolist.R;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.ActivityRegisterBinding;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InputField usernameField = binding.username;
        InputField emailField = binding.edtEmailAddress;
        InputField passwordField = binding.passwordField;
        InputField confirmPasswordField = binding.edtConfirmPassword;

        usernameField.setLabel("Username");
        usernameField.setHint("e.g. elena_rostova");
        usernameField.setStartIcon(R.drawable.ic_mention);
        usernameField.setEndIcon(R.drawable.ic_check_purple);

        emailField.setLabel("Email Address");
        emailField.setHint("name@company.com");
        emailField.setStartIcon(R.drawable.ic_mail);
        emailField.setEndIcon(R.drawable.ic_shield);

        passwordField.setLabel("Master Key");
        passwordField.setRightLabel("Minimum 8 chars");
        passwordField.setHint("● ● ● ● ● ● ● ● ● ● ●");
        passwordField.setStartIcon(R.drawable.ic_lock);
        passwordField.showStrengthBar(true);

        confirmPasswordField.setLabel("Confirm Password");
        confirmPasswordField.setRightLabel("Enter match");
        confirmPasswordField.setHint("● ● ● ● ● ● ● ● ● ● ●");
        confirmPasswordField.setStartIcon(R.drawable.ic_shield_check);

        int passwordType = android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        passwordField.setInputType(passwordType);
        confirmPasswordField.setInputType(passwordType);

        final boolean[] isTermsAgreed = {true};
        binding.checkTerms.setOnClickListener(v -> {
            isTermsAgreed[0] = !isTermsAgreed[0];
            binding.checkTerms.setBackgroundResource(
                    isTermsAgreed[0] ? R.drawable.bg_checkbox_checked : R.drawable.bg_card_input
            );
        });

        binding.txtTermsLink.setOnClickListener(v -> {
            Toast.makeText(this, "Terms of Service & Privacy Policy", Toast.LENGTH_SHORT).show();
        });

        binding.GoogleWithLogin.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In", Toast.LENGTH_SHORT).show();
        });

        binding.txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.username.getText().toString().trim();
            if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
               Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
                return;
            }
            String emailInput = binding.edtEmailAddress.getText().toString().trim();
            String password = binding.passwordField.getText().toString().trim();
            String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isTermsAgreed[0]) {
                Toast.makeText(this, "Please agree to Terms", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show();
                return;
            }

            String email;
            if (!emailInput.isEmpty() && emailInput.contains("@")) {
                email = emailInput;
            } else if (username.contains("@")) {
                email = username;
            } else {
                email = username.replaceAll("\\s+", "").toLowerCase() + "@todolist.com";
            }

            binding.btnRegister.setEnabled(false);

            SupabaseAuth.signUp(email, password, username, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(com.google.gson.JsonObject response) {
                    runOnUiThread(() -> {
                        String userId = "";
                        String accessToken = "";
                        if (response.has("user") && response.get("user").isJsonObject()) {
                            userId = response.getAsJsonObject("user").get("id").getAsString();
                        } else if (response.has("id")) {
                            userId = response.get("id").getAsString();
                        } else if (response.has("session") && response.get("session").isJsonObject()) {
                            if (response.getAsJsonObject("session").has("user") && response.getAsJsonObject("session").get("user").isJsonObject()) {
                                userId = response.getAsJsonObject("session").getAsJsonObject("user").get("id").getAsString();
                            }
                        }

                        if (response.has("access_token")) {
                            accessToken = response.get("access_token").getAsString();
                        } else if (response.has("session") && response.get("session").isJsonObject() && response.getAsJsonObject("session").has("access_token")) {
                            accessToken = response.getAsJsonObject("session").get("access_token").getAsString();
                        }

                        if (!userId.isEmpty()) {
                            new SupabaseRepository().saveUser(userId, username, email, password, accessToken, new SupabaseRepository.UserCallback() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(() -> {
                                        binding.btnRegister.setEnabled(true);
                                        SupabaseAuth.saveSession(RegisterActivity.this, response, username);
                                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        binding.btnRegister.setEnabled(true);
                                        SupabaseAuth.saveSession(RegisterActivity.this, response, username);
                                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    });
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                binding.btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            });
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        binding.btnRegister.setEnabled(true);
                        String displayError = error;
                        if (error != null && (error.toLowerCase().contains("duplicate") || error.toLowerCase().contains("already registered") || error.toLowerCase().contains("already in use"))) {
                            displayError = "Username or Email is already in use";
                        }
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + displayError, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
