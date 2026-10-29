
package com.example.todolist.Dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.databinding.ChangePasswordDialogBoxBinding;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangeUserPassword extends DialogFragment {

    private ChangePasswordDialogBoxBinding binding;

    public ChangeUserPassword() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = ChangePasswordDialogBoxBinding.inflate(
                inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnEditTask.setOnClickListener(v ->
                changePassword());
    }

    private void changePassword() {

        String currentPassword = binding.edtOldPassword.getText().toString().trim();

        String newPassword = binding.edtNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            binding.edtOldPassword.setError("Enter current password");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.edtNewPassword.setError("Enter new password");
            return;
        }

        if (newPassword.length() < 6) {
            binding.edtNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showMessage("Please log in again");
            return;
        }

        if (user.getEmail() == null) {
            showMessage("Email account not available");
            return;
        }
        binding.btnEditTask.setEnabled(false);
        com.google.firebase.auth.AuthCredential credential =
                EmailAuthProvider.getCredential(
                        user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(result -> {
                                if (!isAdded()) {
                                    return;
                                }
                                com.google.firebase.database.DatabaseReference userRef =
                                        com.google.firebase.database.FirebaseDatabase.getInstance()
                                                .getReference("Users")
                                                .child(user.getUid());

                                java.util.HashMap<String, Object> updates = new java.util.HashMap<>();
                                updates.put("password", newPassword);
                                updates.put("Password", newPassword);

                                userRef.updateChildren(updates).addOnCompleteListener(dbTask -> {
                                    if (!isAdded()) {
                                        return;
                                    }
                                    showMessage("Password updated successfully");
                                    dismiss();
                                });
                            })
                            .addOnFailureListener(error -> {
                                if (!isAdded() || binding == null) {
                                    return;
                                }
                                binding.btnEditTask.setEnabled(true);
                                showMessage("Failed to update password: " + error.getMessage());
                            });
                })
                .addOnFailureListener(error -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    binding.btnEditTask.setEnabled(true);
                    showMessage("Current password does not match");
                });
    }

    private void showMessage(String message) {

        if (isAdded()) {
            Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
