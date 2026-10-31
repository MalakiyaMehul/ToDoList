package com.example.todolist.Fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.todolist.Dialog.ChangeUserNameDialog;
import com.example.todolist.Dialog.ChangeUserPassword;
import com.example.todolist.SupabaseAuth;
import com.example.todolist.SupabaseClient;
import com.example.todolist.activity.ChangeUserProfileImage;
import com.example.todolist.activity.LoginActivity;
import com.example.todolist.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private String userId;
    private String accessToken;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        userId = SupabaseAuth.getUserId(requireContext());

        accessToken = SupabaseAuth.getAccessToken(requireContext());

        loadUserName();
        loadProfileImage();
        loadTaskStats();

        getParentFragmentManager().setFragmentResultListener(
                ChangeUserNameDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {

                    String newName = result.getString(
                            ChangeUserNameDialog.BUNDLE_USER_NAME
                    );

                    if (newName != null && binding != null) {

                        binding.txtUserName.setText(newName);

                        requireContext()
                                .getSharedPreferences(
                                        "UserProfile",
                                        Context.MODE_PRIVATE
                                )
                                .edit()
                                .putString("user_name", newName)
                                .apply();
                    }
                }
        );

        // Listen for Image change bottom sheet result
        getParentFragmentManager().setFragmentResultListener(
                ChangeUserProfileImage.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String uriString = result.getString(ChangeUserProfileImage.BUNDLE_IMAGE_URI);
                    if (uriString != null && binding != null) {
                        saveProfileImage(Uri.parse(uriString));
                    }
                });

        // Top App Bar
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        binding.btnMoreOptions.setOnClickListener(v -> showAboutUsDialog());

        // Avatar click and Camera button click
        binding.ivAvatar.setOnClickListener(v -> openChangeImageBottomSheet());
        binding.btnChangeAvatar.setOnClickListener(v -> openChangeImageBottomSheet());
        binding.ivTopAvatar.setOnClickListener(v -> openChangeImageBottomSheet());

        // Account Settings Rows
        binding.rowChangeName.setOnClickListener(v -> openChangeUsernameDialog());
        binding.btnManageSecurity.setOnClickListener(v -> openChangePasswordDialog());

        binding.rowChangePassword.setOnClickListener(v -> openChangePasswordDialog());

        binding.rowNotificationPreferences.setOnClickListener(v ->
                showMessage("Notification Preferences updated")
        );

        binding.rowConnectedCalendars.setOnClickListener(v ->
                showMessage("Connected Calendars: 2 synced")
        );

        // App Preferences Rows
        binding.rowAppSettings.setOnClickListener(v -> showAppSettingsDialog());
        binding.btnCustomization.setOnClickListener(v -> showAppSettingsDialog());

        binding.rowSoundHaptics.setOnClickListener(v ->
                showMessage("Sound & Haptics active: Synthesized chime")
        );

        // Log out button
        binding.rowLogOut.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserName() {

        if (userId == null || accessToken == null) {

            binding.txtUserName.setText("User");
            binding.txtUserEmail.setText("");

            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/profiles?id=eq."
                        + userId
                        + "&select=username,email")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {

                    if (binding != null) {
                        showMessage("Profile load failed");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                String responseBody = response.body() != null
                        ? response.body().string()
                        : "[]";

                boolean success = response.isSuccessful();

                response.close();

                if (!success) return;

                JsonArray array = JsonParser
                        .parseString(responseBody)
                        .getAsJsonArray();

                if (array.size() == 0) return;

                JsonObject profile = array.get(0).getAsJsonObject();

                String username = profile.has("username")
                        && !profile.get("username").isJsonNull()
                        ? profile.get("username").getAsString()
                        : "User";

                String email = profile.has("email")
                        && !profile.get("email").isJsonNull()
                        ? profile.get("email").getAsString()
                        : "";

                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {

                    if (binding == null) return;

                    binding.txtUserName.setText(username);
                    binding.txtUserEmail.setText(email);

                    requireContext()
                            .getSharedPreferences(
                                    "UserProfile",
                                    Context.MODE_PRIVATE
                            )
                            .edit()
                            .putString("user_name", username)
                            .apply();
                });
            }
        });
    }



    private void loadTaskStats() {

        if (userId == null || accessToken == null) {

            binding.tvCompletedStat.setText("0");
            binding.tvOnTimeStat.setText("0%");
            binding.tvStreakStat.setText("0d");

            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/tasks?user_id=eq."
                        + userId
                        + "&select=completed,date")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {

                    if (binding != null) {
                        showMessage("Task statistics load failed");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                String responseBody = response.body() != null
                        ? response.body().string()
                        : "[]";

                boolean success = response.isSuccessful();

                response.close();

                if (!success) return;

                JsonArray tasks = JsonParser
                        .parseString(responseBody)
                        .getAsJsonArray();

                int total = tasks.size();
                int completed = 0;

                Set<String> completedDates = new HashSet<>();

                for (int i = 0; i < tasks.size(); i++) {

                    JsonObject task = tasks.get(i).getAsJsonObject();

                    boolean isCompleted = task.has("completed")
                            && !task.get("completed").isJsonNull()
                            && task.get("completed").getAsBoolean();

                    if (isCompleted) {

                        completed++;

                        if (task.has("date")
                                && !task.get("date").isJsonNull()) {

                            String date = task.get("date").getAsString();

                            if (!date.trim().isEmpty()) {
                                completedDates.add(date);
                            }
                        }
                    }
                }

                int rate = total > 0
                        ? (completed * 100) / total
                        : 0;

                int finalCompleted = completed;
                int finalRate = rate;
                int finalStreak = completedDates.size();

                if (getActivity() == null) return;

                requireActivity().runOnUiThread(() -> {

                    if (binding == null) return;

                    binding.tvCompletedStat.setText(
                            String.valueOf(finalCompleted)
                    );

                    binding.tvOnTimeStat.setText(
                            finalRate + "%"
                    );

                    binding.tvStreakStat.setText(
                            finalStreak + "d"
                    );
                });
            }
        });
    }


    private void openChangeUsernameDialog() {

        if (!SupabaseAuth.isLoggedIn(requireContext())) {

            showMessage("Please log in first");

            return;
        }

        ChangeUserNameDialog dialog =
                new ChangeUserNameDialog();

        dialog.show(
                getParentFragmentManager(),
                "ChangeUserNameDialog"
        );
    }

    private void openChangePasswordDialog() {

        if (!SupabaseAuth.isLoggedIn(requireContext())) {

            showMessage("Please log in first");

            return;
        }

        ChangeUserPassword dialog =
                new ChangeUserPassword();

        dialog.show(
                getParentFragmentManager(),
                "ChangeUserPassword"
        );
    }

    private void openChangeImageBottomSheet() {

        if (!SupabaseAuth.isLoggedIn(requireContext())) {

            showMessage("Please log in first");

            return;
        }

        ChangeUserProfileImage bottomSheet =
                new ChangeUserProfileImage();

        bottomSheet.show(
                getParentFragmentManager(),
                "ChangeUserProfileImage"
        );
    }


    private void saveProfileImage(Uri sourceUri) {

        try {

            String uid = userId != null
                    ? userId
                    : "user";

            File internalFile = new File(
                    requireContext().getFilesDir(),
                    "profile_" + uid + ".jpg"
            );

            try (InputStream in = requireContext()
                    .getContentResolver()
                    .openInputStream(sourceUri);

                 OutputStream out = new FileOutputStream(internalFile)) {

                byte[] buffer = new byte[4096];

                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            if (binding != null) {

                Uri savedUri = Uri.fromFile(internalFile);

                binding.ivAvatar.setImageURI(null);
                binding.ivAvatar.setImageURI(savedUri);

                binding.ivTopAvatar.setImageURI(null);
                binding.ivTopAvatar.setImageURI(savedUri);
            }

            requireContext()
                    .getSharedPreferences(
                            "UserProfile",
                            Context.MODE_PRIVATE
                    )
                    .edit()
                    .putString(
                            "avatar_uri",
                            internalFile.getAbsolutePath()
                    )
                    .apply();

            showMessage("Profile image updated successfully");

        } catch (Exception e) {

            showMessage(
                    "Failed to save image: " + e.getMessage()
            );
        }
    }


    private void loadProfileImage() {

        String uid = userId != null
                ? userId
                : "user";

        File internalFile = new File(
                requireContext().getFilesDir(),
                "profile_" + uid + ".jpg"
        );

        if (internalFile.exists()) {

            Uri uri = Uri.fromFile(internalFile);

            binding.ivAvatar.setImageURI(uri);
            binding.ivTopAvatar.setImageURI(uri);

            return;
        }

        String savedPath = requireContext()
                .getSharedPreferences(
                        "UserProfile",
                        Context.MODE_PRIVATE
                )
                .getString("avatar_uri", null);

        if (savedPath != null) {

            File savedFile = new File(savedPath);

            if (savedFile.exists()) {

                Uri uri = Uri.fromFile(savedFile);

                binding.ivAvatar.setImageURI(uri);
                binding.ivTopAvatar.setImageURI(uri);
            }
        }
    }


    private void showAppSettingsDialog() {
        String[] options = {"Dark Theme (Active)", "Notifications Enabled", "Clear Temporary Cache"};
        boolean[] checked = {true, true, false};

        new AlertDialog.Builder(requireContext())
                .setTitle("Appearance & Settings")
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    checked[which] = isChecked;
                    if (which == 2 && isChecked) {
                        try {
                            File cache = requireContext().getCacheDir();
                            File[] files = cache.listFiles();
                            if (files != null) {
                                for (File f : files) {
                                    f.delete();
                                }
                            }
                            showMessage("Cache cleared successfully");
                        } catch (Exception ignored) {}
                    }
                })
                .setPositiveButton("Done", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showAboutUsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("PulseTask")
                .setMessage("PulseTask v2.4.1 (Build 820)\nObsidian Engine\n\nElevate your daily productivity with hyper-focus telemetry and synchronized workflows.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void showLogoutDialog() {

        new AlertDialog.Builder(requireContext())

                .setTitle("Log Out")

                .setMessage(
                        "Are you sure you want to log out of your account?"
                )

                .setNegativeButton(
                        "Cancel",
                        (dialog, which) -> dialog.dismiss()
                )

                .setPositiveButton(
                        "Log Out",
                        (dialog, which) -> {

                            SupabaseAuth.signOut(requireContext());

                            showMessage("Logged out successfully");

                            Intent intent = new Intent(
                                    requireContext(),
                                    LoginActivity.class
                            );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);

                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        }
                )

                .show();
    }


    private void showMessage(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {

        binding = null;

        super.onDestroyView();
    }
}
