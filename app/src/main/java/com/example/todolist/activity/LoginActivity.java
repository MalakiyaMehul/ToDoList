package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
 import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.todolist.InputField;
import com.example.todolist.R;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;  
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private final SupabaseRepository repository = new SupabaseRepository();

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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.GoogleWithLogin.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
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

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                String displayName = account.getDisplayName();
                String googleId = account.getId();

                if (email == null || email.trim().isEmpty()) {
                    Toast.makeText(this, "Could not retrieve email from Google Account", Toast.LENGTH_SHORT).show();
                    return;
                }

                String username;
                if (displayName != null && !displayName.trim().isEmpty()) {
                    username = displayName.trim();
                } else if (email.contains("@")) {
                    username = email.substring(0, email.indexOf("@"));
                } else {
                    username = "user_" + (System.currentTimeMillis() % 10000);
                }

                String password = "GoogleAuth_" + (googleId != null ? googleId : "default_pass");

                binding.btnLogin.setEnabled(false);

                performGoogleSignUpOrLogin(username, email, password);
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performGoogleSignUpOrLogin(String username, String email, String password) {
        SupabaseAuth.signUp(email, password, username, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(JsonObject response) {
                runOnUiThread(() -> {
                    String userId = extractUserId(response);
                    String accessToken = extractAccessToken(response);

                    if (!userId.isEmpty()) {
                        repository.saveUser(userId, username, email, password, accessToken, new SupabaseRepository.UserCallback() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> navigateToHome(response, username));
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> navigateToHome(response, username));
                            }
                        });
                    } else {
                        navigateToHome(response, username);
                    }
                });
            }

            @Override
            public void onError(String error) {
                SupabaseAuth.signIn(email, password, new SupabaseAuth.AuthCallback() {
                    @Override
                    public void onSuccess(JsonObject signInResponse) {
                        runOnUiThread(() -> {
                            String userId = extractUserId(signInResponse);
                            String accessToken = extractAccessToken(signInResponse);

                            if (!userId.isEmpty()) {
                                repository.saveUser(userId, username, email, password, accessToken, new SupabaseRepository.UserCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(() -> navigateToHome(signInResponse, username));
                                    }

                                    @Override
                                    public void onError(String err) {
                                        runOnUiThread(() -> navigateToHome(signInResponse, username));
                                    }
                                });
                            } else {
                                navigateToHome(signInResponse, username);
                            }
                        });
                    }

                    @Override
                    public void onError(String signInError) {
                        runOnUiThread(() -> {
                            binding.btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Google Sign-In failed: " + signInError, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });
    }

    private String extractUserId(JsonObject response) {
        if (response == null) return "";
        if (response.has("user") && response.get("user").isJsonObject()) {
            JsonObject user = response.getAsJsonObject("user");
            if (user.has("id") && !user.get("id").isJsonNull()) {
                return user.get("id").getAsString();
            }
        }
        if (response.has("id") && !response.get("id").isJsonNull()) {
            return response.get("id").getAsString();
        }
        if (response.has("session") && response.get("session").isJsonObject()) {
            JsonObject session = response.getAsJsonObject("session");
            if (session.has("user") && session.get("user").isJsonObject()) {
                JsonObject user = session.getAsJsonObject("user");
                if (user.has("id") && !user.get("id").isJsonNull()) {
                    return user.get("id").getAsString();
                }
            }
        }
        return "";
    }

    private String extractAccessToken(JsonObject response) {
        if (response == null) return "";
        if (response.has("access_token") && !response.get("access_token").isJsonNull()) {
            return response.get("access_token").getAsString();
        }
        if (response.has("session") && response.get("session").isJsonObject()) {
            JsonObject session = response.getAsJsonObject("session");
            if (session.has("access_token") && !session.get("access_token").isJsonNull()) {
                return session.get("access_token").getAsString();
            }
        }
        return "";
    }

    private void navigateToHome(JsonObject response, String username) {
        if (isFinishing() || isDestroyed()) return;
        binding.btnLogin.setEnabled(true);
        if (response != null) {
            SupabaseAuth.saveSession(LoginActivity.this, response, username);
        }
        getSharedPreferences("UserProfile", MODE_PRIVATE)
                .edit()
                .putString("user_name", username)
                .apply();
        Toast.makeText(LoginActivity.this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
