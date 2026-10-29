package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todolist.InputField;
import com.example.todolist.R;
import com.example.todolist.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends BaseActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");
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
            String emailInput = binding.edtEmailAddress.getText().toString().trim();
            String password = binding.passwordField.getText().toString().trim();
            String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isTermsAgreed[0]) {
                Toast.makeText(this, "Please agree to the Terms of Service & Privacy Policy", Toast.LENGTH_SHORT).show();
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

            binding.btnRegister.setEnabled(false);

            String email;
            if (!emailInput.isEmpty() && emailInput.contains("@")) {
                email = emailInput;
            } else if (username.contains("@")) {
                email = username;
            } else {
                email = username.replaceAll("\\s+", "").toLowerCase() + "@todolist.com";
            }

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        saveUserDataAndNavigate(user.getUid(), username, email, password);
                    } else {
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(this, "Registration failed: User is null", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        // User already exists in Auth, sign in to save data and continue
                        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(loginTask -> {
                            if (loginTask.isSuccessful() && auth.getCurrentUser() != null) {
                                saveUserDataAndNavigate(auth.getCurrentUser().getUid(), username, email, password);
                            } else {
                                binding.btnRegister.setEnabled(true);
                                Toast.makeText(this, "Account already exists with this email/username. Please log in.", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        binding.btnRegister.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Registration Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void saveUserDataAndNavigate(String uid, String username, String email, String password) {
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("username", username);
        userData.put("userName", username);
        userData.put("email", email);
        userData.put("password", password);

        database.child(uid).setValue(userData).addOnSuccessListener(unused -> {
            getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("hasRegistered", true)
                    .apply();
            Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            binding.btnRegister.setEnabled(true);
            Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
