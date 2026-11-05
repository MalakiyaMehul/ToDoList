package com.example.todolist.Fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.todolist.Dialog.ChangeUserNameDialog;
import com.example.todolist.Dialog.ChangeUserPassword;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseClient;
import com.example.todolist.activity.ChangeUserProfileImage;
import com.example.todolist.activity.LoginActivity;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.databinding.FragmentProfileBinding;
import com.example.todolist.model.TaskModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

        userId = SharedPrefManager.getInstance(requireContext()).getString("user_id");
        if (userId.isEmpty()) {
            userId = SupabaseAuth.getUserId(requireContext());
        }

        accessToken = SharedPrefManager.getInstance(requireContext()).getString("access_token");
        if (accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(requireContext());
        }

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

        getParentFragmentManager().setFragmentResultListener(
                ChangeUserProfileImage.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String uriString = result.getString(ChangeUserProfileImage.BUNDLE_IMAGE_URI);
                    if (uriString != null && binding != null) {
                        saveProfileImage(Uri.parse(uriString));
                    }
                });

        binding.ivAvatar.setOnClickListener(v -> openChangeImageBottomSheet());
        binding.btnChangeAvatar.setOnClickListener(v -> openChangeImageBottomSheet());

        binding.rowChangeName.setOnClickListener(v -> openChangeUsernameDialog());
        binding.btnManageSecurity.setOnClickListener(v -> openChangePasswordDialog());
        binding.rowChangePassword.setOnClickListener(v -> openChangePasswordDialog());

        binding.rowNotificationPreferences.setOnClickListener(v ->
                showMessage("Notification Preferences updated")
        );

        binding.rowConnectedCalendars.setOnClickListener(v ->
                showMessage("Connected Calendars: 2 synced")
        );

        binding.rowAppSettings.setOnClickListener(v -> showAppSettingsDialog());
        binding.btnCustomization.setOnClickListener(v -> showAppSettingsDialog());

        binding.rowSoundHaptics.setOnClickListener(v ->
                showMessage("Sound & Haptics active: Synthesized chime")
        );

        binding.rowLogOut.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            userId = SharedPrefManager.getInstance(requireContext()).getString("user_id");
            if (userId.isEmpty()) {
                userId = SupabaseAuth.getUserId(requireContext());
            }

            accessToken = SharedPrefManager.getInstance(requireContext()).getString("access_token");
            if (accessToken.isEmpty()) {
                accessToken = SupabaseAuth.getAccessToken(requireContext());
            }

            loadUserName();
            loadProfileImage();
            loadTaskStats();
        }
    }

    private void loadUserName() {
        if (userId == null || userId.isEmpty()) {
            if (binding != null) {
                binding.txtUserName.setText("User");
                binding.txtUserEmail.setText("");
            }
            return;
        }

        String authHeader = (accessToken != null && !accessToken.isEmpty())
                ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                : "Bearer " + SupabaseClient.SUPABASE_KEY;

        loadUserNameFromTable(authHeader, 0);
    }

    private void loadUserNameFromTable(String authHeader, int tableIdx) {
        String[] tables = new String[]{"profile", "profiles"};
        if (tableIdx >= tables.length) return;

        Request request = new Request.Builder()
                .url(SupabaseClient.SUPABASE_URL
                        + "/rest/v1/"
                        + tables[tableIdx]
                        + "?id=eq."
                        + userId
                        + "&select=username,email")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (tableIdx + 1 < tables.length) {
                    loadUserNameFromTable(authHeader, tableIdx + 1);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null
                        ? response.body().string()
                        : "[]";

                boolean success = response.isSuccessful();
                response.close();

                if (!success) {
                    if (tableIdx + 1 < tables.length) {
                        loadUserNameFromTable(authHeader, tableIdx + 1);
                    }
                    return;
                }

                try {
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
                } catch (Exception ignored) {}
            }
        });
    }

    private Date parseTaskDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        String[] formats = new String[]{"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "MMM dd, yyyy", "MMM d, yyyy"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                Date d = sdf.parse(dateStr.trim());
                if (d != null) return d;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private int calculateStreak(List<TaskModel> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0;
        Set<String> completedDateKeys = new HashSet<>();
        SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (TaskModel task : tasks) {
            if (task.isCompleted() && task.getDate() != null && !task.getDate().trim().isEmpty()) {
                Date d = parseTaskDate(task.getDate());
                if (d != null) {
                    completedDateKeys.add(dayKeyFormat.format(d));
                }
            }
        }
        if (completedDateKeys.isEmpty()) return 0;

        Calendar cal = Calendar.getInstance();
        String todayKey = dayKeyFormat.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayKey = dayKeyFormat.format(cal.getTime());

        int streak = 0;
        if (completedDateKeys.contains(todayKey)) {
            cal = Calendar.getInstance();
            while (completedDateKeys.contains(dayKeyFormat.format(cal.getTime()))) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else if (completedDateKeys.contains(yesterdayKey)) {
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            while (completedDateKeys.contains(dayKeyFormat.format(cal.getTime()))) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else {
            streak = completedDateKeys.size();
        }
        return streak;
    }

    private void loadTaskStats() {
        if (userId == null || userId.isEmpty()) {
            if (binding != null) {
                binding.tvCompletedStat.setText("0");
                binding.tvOnTimeStat.setText("0%");
                binding.tvStreakStat.setText("0d");
            }
            return;
        }

        new SupabaseRepository().getTasks(userId, accessToken, new SupabaseRepository.GetTasksCallback() {
            @Override
            public void onSuccess(List<TaskModel> tasks) {
                if (!isAdded() || binding == null || getActivity() == null) return;

                int total = tasks.size();
                int completed = 0;
                for (TaskModel task : tasks) {
                    if (task.isCompleted()) {
                        completed++;
                    }
                }

                int rate = total > 0 ? (completed * 100) / total : 0;
                int streak = calculateStreak(tasks);

                final int finalCompleted = completed;
                final int finalRate = rate;
                final int finalStreak = streak;

                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.tvCompletedStat.setText(String.valueOf(finalCompleted));
                    binding.tvOnTimeStat.setText(finalRate + "%");
                    binding.tvStreakStat.setText(finalStreak + "d");
                });
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void openChangeUsernameDialog() {
        if (!SupabaseAuth.isLoggedIn(requireContext())) {
            showMessage("Please log in first");
            return;
        }

        ChangeUserNameDialog dialog = new ChangeUserNameDialog();
        dialog.show(getParentFragmentManager(), "ChangeUserNameDialog");
    }

    private void openChangePasswordDialog() {
        if (!SupabaseAuth.isLoggedIn(requireContext())) {
            showMessage("Please log in first");
            return;
        }

        ChangeUserPassword dialog = new ChangeUserPassword();
        dialog.show(getParentFragmentManager(), "ChangeUserPassword");
    }

    private void openChangeImageBottomSheet() {
        if (!SupabaseAuth.isLoggedIn(requireContext())) {
            showMessage("Please log in first");
            return;
        }

        ChangeUserProfileImage bottomSheet = new ChangeUserProfileImage();
        bottomSheet.show(getParentFragmentManager(), "ChangeUserProfileImage");
    }

    private void saveProfileImage(Uri sourceUri) {
        try {
            String uid = userId != null ? userId : "user";
            File internalFile = new File(requireContext().getFilesDir(), "profile_" + uid + ".jpg");

            try (InputStream in = requireContext().getContentResolver().openInputStream(sourceUri);
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
            }

            requireContext()
                    .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    .edit()
                    .putString("avatar_uri", internalFile.getAbsolutePath())
                    .apply();

            showMessage("Profile image updated successfully");

        } catch (Exception e) {
            showMessage("Failed to save image: " + e.getMessage());
        }
    }

    private void loadProfileImage() {
        String uid = userId != null ? userId : "user";
        File internalFile = new File(requireContext().getFilesDir(), "profile_" + uid + ".jpg");

        if (internalFile.exists()) {
            Uri uri = Uri.fromFile(internalFile);
            binding.ivAvatar.setImageURI(uri);
            return;
        }

        String savedPath = requireContext()
                .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);

        if (savedPath != null) {
            File savedFile = new File(savedPath);
            if (savedFile.exists()) {
                Uri uri = Uri.fromFile(savedFile);
                binding.ivAvatar.setImageURI(uri);
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
                .setMessage("Are you sure you want to log out of your account?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Log Out", (dialog, which) -> {
                    SupabaseAuth.signOut(requireContext());
                    showMessage("Logged out successfully");
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
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
