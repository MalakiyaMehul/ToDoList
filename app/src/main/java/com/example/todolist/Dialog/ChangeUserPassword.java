
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

import com.example.todolist.SupabaseAuth;
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

        binding.btnEditTask.setOnClickListener(v -> changePassword());
    }


    private void changePassword() {

        String currentPassword =
                binding.edtOldPassword.getText().toString().trim();

        String newPassword =
                binding.edtNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            binding.edtOldPassword.setError("Enter current password");
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            binding.edtNewPassword.setError("Enter new password");
            return;
        }

        if (newPassword.length() < 6) {
            binding.edtNewPassword.setError(
                    "Password must be at least 6 characters"
            );
            return;
        }

        String email = SupabaseAuth.getEmail(requireContext());

        String accessToken = SupabaseAuth.getAccessToken(requireContext());

        if (email == null || accessToken == null) {

            showMessage("Please login again");
            return;
        }

        binding.btnEditTask.setEnabled(false);

        SupabaseAuth.signIn(email, currentPassword,
                new SupabaseAuth.AuthCallback() {

                    @Override
                    public void onSuccess(com.google.gson.JsonObject response) {

                        String verifiedToken = response.has("access_token")
                                ? response.get("access_token").getAsString()
                                : accessToken;

                        SupabaseAuth.updatePassword(
                                verifiedToken,
                                newPassword,
                                new SupabaseAuth.AuthCallback() {

                                    @Override
                                    public void onSuccess(
                                            com.google.gson.JsonObject response
                                    ) {

                                        requireActivity().runOnUiThread(() -> {

                                            binding.btnEditTask.setEnabled(true);

                                            showMessage("Password updated successfully");

                                            dismiss();
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {

                                        requireActivity().runOnUiThread(() -> {

                                            binding.btnEditTask.setEnabled(true);

                                            showMessage(
                                                    "Password update failed: " + error
                                            );
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {

                        requireActivity().runOnUiThread(() -> {

                            binding.btnEditTask.setEnabled(true);

                            showMessage("Current password does not match");
                        });
                    }
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
