package com.example.todolist.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.todolist.InputField;
import com.example.todolist.R;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.activity.home.HomeActivity;
import com.example.todolist.databinding.ActivityRegisterBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;

public class RegisterActivity extends BaseActivity {

    private static final long CHECK_DEBOUNCE_MS = 150L;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private enum AvailabilityState {
        NONE,
        INVALID,
        CHECKING,
        AVAILABLE,
        TAKEN,
        ERROR
    }

    private ActivityRegisterBinding binding;
    private boolean isTermsAgreed = true;
    private final Handler validationHandler = new Handler(Looper.getMainLooper());
    private final SupabaseRepository repository = new SupabaseRepository();
    private Runnable usernameCheckRunnable;
    private Runnable emailCheckRunnable;
    private int usernameCheckToken = 0;
    private int emailCheckToken = 0;
    private AvailabilityState usernameState = AvailabilityState.NONE;
    private AvailabilityState emailState = AvailabilityState.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.scrollView.setVerticalScrollBarEnabled(false);
        setupKeyboardInsets();

        InputField usernameField = binding.username;
        InputField emailField = binding.edtEmailAddress;
        InputField passwordField = binding.passwordField;
        InputField confirmPasswordField = binding.edtConfirmPassword;

        usernameField.setLabel("Username");
        usernameField.setHint("e.g. elena_rostova");
        usernameField.setStartIcon(R.drawable.ic_mention);
        usernameField.setEndIcon(R.drawable.ic_check_purple);
        usernameField.setInputType(InputType.TYPE_CLASS_TEXT);

        emailField.setLabel("Email Address");
        emailField.setHint("name@company.com");
        emailField.setStartIcon(R.drawable.ic_mail);
        emailField.setEndIcon(R.drawable.ic_shield);
        emailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        passwordField.setLabel("Master Key");
        passwordField.setRightLabel("Minimum 8 chars");
        passwordField.setHint("● ● ● ● ● ● ● ● ● ● ●");
        passwordField.setStartIcon(R.drawable.ic_lock);
        passwordField.showStrengthBar(true);

        confirmPasswordField.setLabel("Confirm Password");
        confirmPasswordField.setRightLabel("Enter match");
        confirmPasswordField.setHint("● ● ● ● ● ● ● ● ● ● ●");
        confirmPasswordField.setStartIcon(R.drawable.ic_shield_check);

        int passwordType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        passwordField.setInputType(passwordType);
        confirmPasswordField.setInputType(passwordType);

        setupLiveValidation(usernameField, emailField);

        usernameField.setOnEnterActionListener(EditorInfo.IME_ACTION_NEXT, () -> moveToField(emailField));
        emailField.setOnEnterActionListener(EditorInfo.IME_ACTION_NEXT, () -> moveToField(passwordField));
        passwordField.setOnEnterActionListener(EditorInfo.IME_ACTION_NEXT, () -> moveToField(confirmPasswordField));
        confirmPasswordField.setOnEnterActionListener(EditorInfo.IME_ACTION_DONE, () -> {
            hideKeyboard();
            scrollLastFieldAndRegisterButtonIntoView();
            attemptRegistration();
        });

        attachScrollOnFocus(usernameField, false);
        attachScrollOnFocus(emailField, false);
        attachScrollOnFocus(passwordField, false);
        attachScrollOnFocus(confirmPasswordField, true);

        binding.checkTerms.setOnClickListener(v -> {
            isTermsAgreed = !isTermsAgreed;
            binding.checkTerms.setBackgroundResource(
                    isTermsAgreed ? R.drawable.bg_checkbox_checked : R.drawable.bg_card_input
            );
        });

        binding.txtTermsLink.setOnClickListener(v ->
                Toast.makeText(this, "Terms of Service & Privacy Policy", Toast.LENGTH_SHORT).show()
        );

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

        binding.txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnRegister.setOnClickListener(v -> attemptRegistration());
        binding.btnRegister.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    attemptRegistration();
                }
                return true;
            }
            return false;
        });
    }

    private void setupKeyboardInsets() {
        View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(contentView, (view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                );
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                if (windowInsets.isVisible(WindowInsetsCompat.Type.ime())
                        && binding != null
                        && binding.edtConfirmPassword.getEditText() != null
                        && binding.edtConfirmPassword.getEditText().hasFocus()) {
                    scrollLastFieldAndRegisterButtonIntoView();
                }
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(contentView);
        }
    }

    private void attachScrollOnFocus(InputField field, boolean includeRegisterButton) {
        if (field.getEditText() == null) {
            return;
        }
        field.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (includeRegisterButton) {
                    scrollLastFieldAndRegisterButtonIntoView();
                } else {
                    scrollViewIntoView(field);
                }
            }
        });
    }

    private void moveToField(InputField targetField) {
        targetField.focusInput();
        if (targetField == binding.edtConfirmPassword) {
            scrollLastFieldAndRegisterButtonIntoView();
        } else {
            scrollViewIntoView(targetField);
        }
    }

    private void scrollViewIntoView(View targetView) {
        if (binding == null) {
            return;
        }
        binding.scrollView.postDelayed(() -> {
            if (binding == null) {
                return;
            }
            Rect rect = new Rect();
            targetView.getDrawingRect(rect);
            binding.scrollView.offsetDescendantRectToMyCoords(targetView, rect);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            int scrollViewHeight = binding.scrollView.getHeight();
            int currentScrollY = binding.scrollView.getScrollY();
            if (rect.bottom + padding > currentScrollY + scrollViewHeight) {
                int targetY = Math.max(0, (rect.bottom + padding) - scrollViewHeight);
                binding.scrollView.smoothScrollTo(0, targetY);
            } else if (rect.top - padding < currentScrollY) {
                binding.scrollView.smoothScrollTo(0, Math.max(0, rect.top - padding));
            }
        }, 100);
    }

    private void scrollLastFieldAndRegisterButtonIntoView() {
        if (binding == null) {
            return;
        }
        binding.scrollView.postDelayed(() -> {
            if (binding == null) {
                return;
            }
            Rect fieldRect = new Rect();
            binding.edtConfirmPassword.getDrawingRect(fieldRect);
            binding.scrollView.offsetDescendantRectToMyCoords(binding.edtConfirmPassword, fieldRect);

            Rect buttonRect = new Rect();
            binding.btnRegister.getDrawingRect(buttonRect);
            binding.scrollView.offsetDescendantRectToMyCoords(binding.btnRegister, buttonRect);

            int scrollViewHeight = binding.scrollView.getHeight();
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            int desiredScrollY = (buttonRect.bottom + padding) - scrollViewHeight;
            int maxScrollToKeepFieldVisible = Math.max(0, fieldRect.top - padding);
            int targetY = Math.max(0, Math.min(desiredScrollY, maxScrollToKeepFieldVisible));
            binding.scrollView.smoothScrollTo(0, targetY);
        }, 120);
    }

    private void setupLiveValidation(InputField usernameField, InputField emailField) {
        usernameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (usernameCheckRunnable != null) {
                    validationHandler.removeCallbacks(usernameCheckRunnable);
                }
                int token = ++usernameCheckToken;
                String value = s != null ? s.toString().trim() : "";

                if (value.isEmpty()) {
                    usernameState = AvailabilityState.NONE;
                    usernameField.clearStatusMessage();
                    usernameField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));
                    return;
                }

                if (!value.matches("^[a-zA-Z0-9_]{3,20}$")) {
                    usernameState = AvailabilityState.INVALID;
                    usernameField.setStatusMessage(
                            "Username must be 3-20 chars (letters, numbers, _)",
                            ContextCompat.getColor(RegisterActivity.this, R.color.danger_red)
                    );
                    usernameField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.danger_red));
                    return;
                }

                usernameState = AvailabilityState.CHECKING;
                usernameField.setStatusMessage(
                        "Checking username...",
                        ContextCompat.getColor(RegisterActivity.this, R.color.text_muted)
                );
                usernameField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));

                usernameCheckRunnable = () -> repository.checkUsernameExists(value, new SupabaseRepository.AvailabilityCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        if (isFinishing() || isDestroyed() || binding == null
                                || token != usernameCheckToken
                                || !value.equals(binding.username.getText())) {
                            return;
                        }
                        if (exists) {
                            showUsernameTaken();
                        } else {
                            showUsernameAvailable();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isFinishing() || isDestroyed() || binding == null
                                || token != usernameCheckToken
                                || !value.equals(binding.username.getText())) {
                            return;
                        }
                        usernameState = AvailabilityState.ERROR;
                        binding.username.setStatusMessage(
                                "Unable to verify username",
                                ContextCompat.getColor(RegisterActivity.this, R.color.text_muted)
                        );
                        binding.username.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));
                    }
                });
                validationHandler.postDelayed(usernameCheckRunnable, CHECK_DEBOUNCE_MS);
            }
        });

        emailField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (emailCheckRunnable != null) {
                    validationHandler.removeCallbacks(emailCheckRunnable);
                }
                int token = ++emailCheckToken;
                String value = s != null ? s.toString().trim() : "";

                if (value.isEmpty()) {
                    emailState = AvailabilityState.NONE;
                    emailField.clearStatusMessage();
                    emailField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                    emailState = AvailabilityState.INVALID;
                    emailField.setStatusMessage(
                            "Enter a valid email address",
                            ContextCompat.getColor(RegisterActivity.this, R.color.danger_red)
                    );
                    emailField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.danger_red));
                    return;
                }

                emailState = AvailabilityState.CHECKING;
                emailField.setStatusMessage(
                        "Checking email ID...",
                        ContextCompat.getColor(RegisterActivity.this, R.color.text_muted)
                );
                emailField.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));

                emailCheckRunnable = () -> repository.checkEmailExists(value, new SupabaseRepository.AvailabilityCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        if (isFinishing() || isDestroyed() || binding == null
                                || token != emailCheckToken
                                || !value.equals(binding.edtEmailAddress.getText())) {
                            return;
                        }
                        if (exists) {
                            showEmailTaken();
                        } else {
                            showEmailAvailable();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isFinishing() || isDestroyed() || binding == null
                                || token != emailCheckToken
                                || !value.equals(binding.edtEmailAddress.getText())) {
                            return;
                        }
                        emailState = AvailabilityState.ERROR;
                        binding.edtEmailAddress.setStatusMessage(
                                "Unable to verify email ID",
                                ContextCompat.getColor(RegisterActivity.this, R.color.text_muted)
                        );
                        binding.edtEmailAddress.setEndIconTint(ContextCompat.getColor(RegisterActivity.this, R.color.text_muted));
                    }
                });
                validationHandler.postDelayed(emailCheckRunnable, CHECK_DEBOUNCE_MS);
            }
        });
    }

    private void showUsernameTaken() {
        usernameState = AvailabilityState.TAKEN;
        int errorColor = ContextCompat.getColor(this, R.color.danger_red);
        binding.username.setStatusMessage("This usernme alredy used", errorColor);
        binding.username.setEndIconTint(errorColor);
    }

    private void showUsernameAvailable() {
        usernameState = AvailabilityState.AVAILABLE;
        int successColor = ContextCompat.getColor(this, R.color.status_success);
        binding.username.setStatusMessage("SuccessFull", successColor);
        binding.username.setEndIconTint(successColor);
    }

    private void showEmailTaken() {
        emailState = AvailabilityState.TAKEN;
        int errorColor = ContextCompat.getColor(this, R.color.danger_red);
        binding.edtEmailAddress.setStatusMessage("This email is already used", errorColor);
        binding.edtEmailAddress.setEndIconTint(errorColor);
    }

    private void showEmailAvailable() {
        emailState = AvailabilityState.AVAILABLE;
        int successColor = ContextCompat.getColor(this, R.color.status_success);
        binding.edtEmailAddress.setStatusMessage("SuccessFull", successColor);
        binding.edtEmailAddress.setEndIconTint(successColor);
    }

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    private void attemptRegistration() {
        if (!binding.btnRegister.isEnabled()) {
            return;
        }

        String username = binding.username.getText().trim();
        String emailInput = binding.edtEmailAddress.getText().trim();
        String password = binding.passwordField.getText().trim();
        String confirmPassword = binding.edtConfirmPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            if (username.isEmpty()) {
                moveToField(binding.username);
            } else if (password.isEmpty()) {
                moveToField(binding.passwordField);
            } else {
                moveToField(binding.edtConfirmPassword);
            }
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
            moveToField(binding.username);
            return;
        }

        if (!emailInput.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            int errorColor = ContextCompat.getColor(this, R.color.danger_red);
            binding.edtEmailAddress.setStatusMessage("Enter a valid email address", errorColor);
            binding.edtEmailAddress.setEndIconTint(errorColor);
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            moveToField(binding.edtEmailAddress);
            return;
        }

        if (!isTermsAgreed) {
            Toast.makeText(this, "Please agree to Terms", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            moveToField(binding.passwordField);
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Password does not match", Toast.LENGTH_SHORT).show();
            moveToField(binding.edtConfirmPassword);
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
        binding.btnRegister.setAlpha(0.7f);

        checkBothAvailabilityAndSignUp(username, emailInput, email, password);
    }

    private void checkBothAvailabilityAndSignUp(String username, String emailInput, String resolvedEmail, String password) {
        final boolean[] usernameDone = {false};
        final boolean[] emailDone = {emailInput.isEmpty()};
        final boolean[] usernameTaken = {usernameState == AvailabilityState.TAKEN};
        final boolean[] emailTaken = {!emailInput.isEmpty() && emailState == AvailabilityState.TAKEN};

        Runnable checkAndProceed = () -> {
            if (!usernameDone[0] || !emailDone[0]) {
                return;
            }

            if (usernameTaken[0] || emailTaken[0]) {
                binding.btnRegister.setEnabled(true);
                binding.btnRegister.setAlpha(1.0f);

                if (usernameTaken[0]) {
                    showUsernameTaken();
                }
                if (emailTaken[0]) {
                    showEmailTaken();
                }

                if (usernameTaken[0] && emailTaken[0]) {
                    Toast.makeText(RegisterActivity.this, "Username and Email are already in use", Toast.LENGTH_SHORT).show();
                    moveToField(binding.username);
                } else if (usernameTaken[0]) {
                    Toast.makeText(RegisterActivity.this, "This username is already taken", Toast.LENGTH_SHORT).show();
                    moveToField(binding.username);
                } else {
                    Toast.makeText(RegisterActivity.this, "This email is already registered", Toast.LENGTH_SHORT).show();
                    moveToField(binding.edtEmailAddress);
                }
                return;
            }

            performSignUp(username, resolvedEmail, password);
        };

        if (usernameState == AvailabilityState.AVAILABLE) {
            usernameDone[0] = true;
        } else {
            repository.checkUsernameExists(username, new SupabaseRepository.AvailabilityCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    usernameDone[0] = true;
                    if (exists) {
                        usernameTaken[0] = true;
                    } else {
                        showUsernameAvailable();
                    }
                    checkAndProceed.run();
                }

                @Override
                public void onError(String error) {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    usernameDone[0] = true;
                    checkAndProceed.run();
                }
            });
        }

        if (emailInput.isEmpty() || emailState == AvailabilityState.AVAILABLE) {
            emailDone[0] = true;
            checkAndProceed.run();
        } else {
            repository.checkEmailExists(emailInput, new SupabaseRepository.AvailabilityCallback() {
                @Override
                public void onResult(boolean exists) {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    emailDone[0] = true;
                    if (exists) {
                        emailTaken[0] = true;
                    } else {
                        showEmailAvailable();
                    }
                    checkAndProceed.run();
                }

                @Override
                public void onError(String error) {
                    if (isFinishing() || isDestroyed() || binding == null) return;
                    emailDone[0] = true;
                    checkAndProceed.run();
                }
            });
        }
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

                binding.btnRegister.setEnabled(false);
                binding.btnRegister.setAlpha(0.6f);

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
                            binding.btnRegister.setEnabled(true);
                            binding.btnRegister.setAlpha(1.0f);
                            Toast.makeText(RegisterActivity.this, "Google Registration failed: " + signInError, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });
    }

    private void performSignUp(String username, String email, String password) {
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
                runOnUiThread(() -> {
                    binding.btnRegister.setEnabled(true);
                    binding.btnRegister.setAlpha(1.0f);
                    String displayError = error;
                    if (error != null && (error.toLowerCase().contains("duplicate")
                            || error.toLowerCase().contains("already registered")
                            || error.toLowerCase().contains("already in use")
                            || error.toLowerCase().contains("user_already_exists"))) {
                        showUsernameTaken();
                        showEmailTaken();
                        displayError = "Username and Email are already in use";
                        moveToField(binding.username);
                    }
                    Toast.makeText(RegisterActivity.this, displayError, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String extractUserId(JsonObject response) {
        if (response == null) {
            return "";
        }
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
        if (response == null) {
            return "";
        }
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
        if (isFinishing() || isDestroyed()) {
            return;
        }
        binding.btnRegister.setEnabled(true);
        binding.btnRegister.setAlpha(1.0f);
        if (response != null) {
            SupabaseAuth.saveSession(RegisterActivity.this, response, username);
        }
        getSharedPreferences("UserProfile", MODE_PRIVATE)
                .edit()
                .putString("user_name", username)
                .apply();
        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        validationHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
