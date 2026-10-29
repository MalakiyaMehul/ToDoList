
package com.example.todolist.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

        binding = FragmentProfileBinding.inflate(
                inflater, container, false);

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

        getParentFragmentManager().setFragmentResultListener(
                ChangeUserNameDialog.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String newName = result.getString(
                            ChangeUserNameDialog.BUNDLE_USER_NAME);

                    if (newName != null && binding != null) {
                        binding.txtUserName.setText(newName);
                    }
                });

        getParentFragmentManager().setFragmentResultListener(
                ChangeUserProfileImage.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String uriString = result.getString(ChangeUserProfileImage.BUNDLE_IMAGE_URI);
                    if (uriString != null && binding != null) {
                        saveProfileImage(Uri.parse(uriString));
                    }
                });

        binding.rowChangeName.setOnClickListener(v -> openChangeUsernameDialog());

        binding.ivAvatar.setOnClickListener(v -> openChangeImageBottomSheet());
        binding.rowChangeImage.setOnClickListener(v -> openChangeImageBottomSheet());

        binding.rowChangePassword.setOnClickListener(v -> {
            ChangeUserPassword dialog = new ChangeUserPassword();
            dialog.show(getParentFragmentManager(), "ChangeUserPassword");
        });

        binding.rowAppSettings.setOnClickListener(v -> showAppSettingsDialog());

        binding.rowAboutUs.setOnClickListener(v -> showAboutUsDialog());

        binding.rowFaq.setOnClickListener(v -> showFaqDialog());

        binding.rowHelpFeedback.setOnClickListener(v -> showHelpFeedbackDialog());

        binding.rowSupportUs.setOnClickListener(v -> showSupportUsDialog());

        binding.rowLogOut.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadUserName() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            binding.txtUserName.setText("User");
            return;
        }
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) {
                    return;
                }
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
                    username = "User";
                }
                binding.txtUserName.setText(username);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showMessage(error.getMessage());
            }
        };
        userRef.addValueEventListener(profileListener);
    }

    private void openChangeUsernameDialog() {
        if (firebaseAuth.getCurrentUser() == null) {
            showMessage("Please log in first");
            return;
        }
        ChangeUserNameDialog dialog = new ChangeUserNameDialog();
        dialog.show(getParentFragmentManager(), "ChangeUserNameDialog");
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
            binding.ivAvatar.setImageURI(Uri.fromFile(internalFile));
            return;
        }

        String savedPath = requireContext().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                .getString("avatar_uri", null);
        if (savedPath != null) {
            File savedFile = new File(savedPath);
            if (savedFile.exists()) {
                binding.ivAvatar.setImageURI(Uri.fromFile(savedFile));
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
                .setTitle("App Settings")
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
                .setTitle("About UpTodo")
                .setMessage("UpTodo v1.0.0\n\nUpTodo is your everyday task manager designed to help you organize daily routines, manage to-do items, track categories, and improve productivity.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showFaqDialog() {
        String faqText = "1. How to add a task?\n" +
                "Tap '+' at the bottom bar, enter title, description, category, and priority, then save.\n\n" +
                "2. How to complete or edit a task?\n" +
                "Tap any task in your list to edit details or mark it done.\n\n" +
                "3. How to create custom categories?\n" +
                "When choosing a category, tap '+ Create New' to create customized categories.\n\n" +
                "4. How to change username or profile picture?\n" +
                "Use 'Change account name' or tap on your avatar in this screen.";

        new AlertDialog.Builder(requireContext())
                .setTitle("FAQ")
                .setMessage(faqText)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showHelpFeedbackDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Write your feedback or issue here...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("Help & Feedback")
                .setView(input)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String feedback = input.getText().toString().trim();
                    if (!feedback.isEmpty()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        String uid = user != null ? user.getUid() : "anonymous";
                        DatabaseReference feedbackRef = FirebaseDatabase.getInstance()
                                .getReference("Feedback")
                                .push();
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("uid", uid);
                        map.put("feedback", feedback);
                        map.put("timestamp", System.currentTimeMillis());
                        feedbackRef.setValue(map);
                        showMessage("Thank you for your feedback!");
                    } else {
                        showMessage("Please enter feedback before submitting");
                    }
                })
                .setNeutralButton("Email Support", (dialog, which) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:support@uptodo.com"));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "UpTodo Feedback");
                    try {
                        startActivity(Intent.createChooser(emailIntent, "Send Email"));
                    } catch (Exception e) {
                        showMessage("No email app found");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSupportUsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Support Us")
                .setMessage("Love using UpTodo? Help us grow by sharing the app with your family and friends!")
                .setPositiveButton("Share App", (dialog, which) -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out UpTodo - Simple & fast task management!");
                    startActivity(Intent.createChooser(shareIntent, "Share UpTodo via"));
                })
                .setNeutralButton("Rate 5 Stars", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + requireContext().getPackageName())));
                    } catch (Exception e) {
                        showMessage("Thank you for your 5-star support!");
                    }
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Log out", (dialog, which) -> {
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
