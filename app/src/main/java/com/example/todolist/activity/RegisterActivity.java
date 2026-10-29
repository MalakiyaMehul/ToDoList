package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todolist.InputField;
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
        InputField passwordField = binding.passwordField;
        InputField confirmPasswordField = binding.edtConfirmPassword;

        usernameField.setLabel("Username");
        usernameField.setHint("Enter your Username or Email");

        passwordField.setLabel("Password");
        passwordField.setHint("● ● ● ● ● ● ● ");

        confirmPasswordField.setLabel("Confirm Password");
        confirmPasswordField.setHint("● ● ● ● ● ● ●");

        int passwordType = android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
        passwordField.setInputType(passwordType);
        confirmPasswordField.setInputType(passwordType);

        binding.btnImg.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, StartActivity.class);
            startActivity(intent);
            finish();
        });

        binding.txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.username.getText().toString().trim();
            String password = binding.passwordField.getText().toString().trim();
            String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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

            String email = username.contains("@") ? username : (username.replaceAll("\\s+", "").toLowerCase() + "@todolist.com");

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
