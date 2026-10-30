package com.example.todolist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todolist.InputField;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.app_name))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

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



        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null) {
                        try {
                            GoogleSignInAccount account =
                                    GoogleSignIn.getSignedInAccountFromIntent(
                                                    result.getData())
                                            .getResult(ApiException.class);

                            if (account != null && account.getIdToken() != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }

                        } catch (ApiException e) {
                            Toast.makeText(this,
                                    "Google Login failed: " + e.getStatusCode(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        binding.txtRegistration.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.txtUserLogin.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();
            if(username.isEmpty() || password.isEmpty())
            {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            String email = username.contains("@") ? username : (username.replaceAll("\\s+", "").toLowerCase() + "@todolist.com");
            binding.btnLogin.setEnabled(false);
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                binding.btnLogin.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getMessage() : "Invalid email or password";
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        });
        binding.GoogleWithLogin.setOnClickListener(v -> {
            googleSignInLauncher.launch(
                    googleSignInClient.getSignInIntent());
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Google Login successful",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(
                                LoginActivity.this, HomeActivity.class);

                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this,
                                "Google Login failed",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}