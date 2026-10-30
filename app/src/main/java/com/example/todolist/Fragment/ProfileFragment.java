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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private ValueEventListener profileListener;

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

        firebaseAuth = FirebaseAuth.getInstance();

        loadUserName();
        loadProfileImage();
        loadTaskStats();

        // Listen for Name change dialog result and sync to Firebase Realtime Database
        getParentFragmentManager().setFragmentResultListener(
                ChangeUserNameDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String newName = result.getString(ChangeUserNameDialog.BUNDLE_USER_NAME);
                    if (newName != null && binding != null) {
                        binding.txtUserName.setText(newName);

                        // Save locally for immediate display across IndexFragment & CalendarFragment
                        requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                                .edit()
                                .putString("user_name", newName)
                                .apply();

                        // Sync to Firebase Realtime Database
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("Users")
                                    .child(user.getUid());
                            ref.child("userName").setValue(newName);
                            ref.child("username").setValue(newName);
                            ref.child("name").setValue(newName);
                        }
                    }
                });

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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            binding.txtUserName.setText("Elena Rostova");
            binding.txtUserEmail.setText("elena.rostova@designcraft.io");
            return;
        }

        String email = currentUser.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            binding.txtUserEmail.setText(email);
        } else {
            binding.txtUserEmail.setText("elena.rostova@designcraft.io");
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;

                String username = snapshot.child("userName").getValue(String.class);
                if (username == null || username.trim().isEmpty()) {
                    username = snapshot.child("username").getValue(String.class);
                }
                if (username == null || username.trim().isEmpty()) {
                    username = snapshot.child("name").getValue(String.class);
                }
                if (username == null || username.trim().isEmpty()) {
                    username = currentUser.getDisplayName();
                }
                if (username == null || username.trim().isEmpty()) {
                    username = "Elena Rostova";
                }
                binding.txtUserName.setText(username);

                String userEmail = snapshot.child("email").getValue(String.class);
                if (userEmail != null && !userEmail.trim().isEmpty()) {
                    binding.txtUserEmail.setText(userEmail);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showMessage(error.getMessage());
            }
        };
        userRef.addValueEventListener(profileListener);
    }

    private void loadTaskStats() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            binding.tvCompletedStat.setText("248");
            binding.tvOnTimeStat.setText("94%");
            binding.tvStreakStat.setText("18d");
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUser.getUid())
                .child("Tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (binding == null) return;
                        int total = 0;
                        int completed = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            total++;
                            Boolean isCompleted = child.child("completed").getValue(Boolean.class);
                            if (Boolean.TRUE.equals(isCompleted)) {
                                completed++;
                            }
                        }

                        if (total > 0) {
                            binding.tvCompletedStat.setText(String.valueOf(completed));
                            int rate = (completed * 100) / total;
                            binding.tvOnTimeStat.setText(rate + "%");
                        } else {
                            binding.tvCompletedStat.setText("248");
                            binding.tvOnTimeStat.setText("94%");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void openChangeUsernameDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            showMessage("Please log in first");
            return;
        }
        ChangeUserNameDialog dialog = new ChangeUserNameDialog();
        dialog.show(getParentFragmentManager(), "ChangeUserNameDialog");
    }

    private void openChangePasswordDialog() {
        ChangeUserPassword dialog = new ChangeUserPassword();
        dialog.show(getParentFragmentManager(), "ChangeUserPassword");
    }

    private void openChangeImageBottomSheet() {
        if (firebaseAuth.getCurrentUser() == null) {
            showMessage("Please log in first");
            return;
        }
        ChangeUserProfileImage bottomSheet = new ChangeUserProfileImage();
        bottomSheet.show(getParentFragmentManager(), "ChangeUserProfileImage");
    }

    private void saveProfileImage(Uri sourceUri) {
        try {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            String uid = currentUser != null ? currentUser.getUid() : "user";
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
                binding.ivAvatar.setImageURI(null);
                binding.ivAvatar.setImageURI(Uri.fromFile(internalFile));
                binding.ivTopAvatar.setImageURI(null);
                binding.ivTopAvatar.setImageURI(Uri.fromFile(internalFile));
            }

            requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    .edit()
                    .putString("avatar_uri", internalFile.getAbsolutePath())
                    .apply();

            Bitmap bitmap = BitmapFactory.decodeFile(internalFile.getAbsolutePath());
            if (bitmap != null && currentUser != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 180, 180, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                // Update in Firebase Realtime Database
                FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(currentUser.getUid())
                        .child("profileImage")
                        .setValue(base64Image);
            }

            showMessage("Profile image updated successfully");
        } catch (Exception e) {
            showMessage("Failed to save image: " + e.getMessage());
        }
    }

    private void loadProfileImage() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String uid = currentUser != null ? currentUser.getUid() : "user";
        File internalFile = new File(requireContext().getFilesDir(), "profile_" + uid + ".jpg");

        if (internalFile.exists()) {
            Uri uri = Uri.fromFile(internalFile);
            binding.ivAvatar.setImageURI(uri);
            binding.ivTopAvatar.setImageURI(uri);
            return;
        }

        String savedPath = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);
        if (savedPath != null) {
            File savedFile = new File(savedPath);
            if (savedFile.exists()) {
                Uri uri = Uri.fromFile(savedFile);
                binding.ivAvatar.setImageURI(uri);
                binding.ivTopAvatar.setImageURI(uri);
                return;
            }
        }

        if (currentUser != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid())
                    .child("profileImage")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (binding == null) return;
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            try {
                                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                if (bitmap != null) {
                                    binding.ivAvatar.setImageBitmap(bitmap);
                                    binding.ivTopAvatar.setImageBitmap(bitmap);
                                    try (FileOutputStream fos = new FileOutputStream(internalFile)) {
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                                    } catch (Exception ignored) {}
                                }
                            } catch (Exception ignored) {}
                        }
                    });
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
                    firebaseAuth.signOut();
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
        if (userRef != null && profileListener != null) {
            userRef.removeEventListener(profileListener);
            profileListener = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
