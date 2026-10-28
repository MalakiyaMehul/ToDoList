package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todolist.InputField;
import com.example.todolist.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
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

        InputField emailField = binding.username;
        InputField passwordField = binding.passwordField;
        InputField confirmPasswordField = binding.edtConfirmPassword;

        emailField.setLabel("Email");
        emailField.setHint("Enter your Email");

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
            String email = emailField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful() && auth.getCurrentUser() != null) {
                    FirebaseUser user = auth.getCurrentUser();
                    String uid = user.getUid();
                    
                    HashMap<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("email", email);
                    userData.put("password", password); // Storing password in DB (Note: generally not recommended for production)
                    
                    database.child(uid).setValue(userData).addOnSuccessListener(unused -> {
                        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("hasRegistered", true)
                                .apply();
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    Toast.makeText(this, "Registration Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown Error"), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
