package com.example.todolist.Dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseClient;
import com.example.todolist.databinding.UserNameChangeDialogBoxBinding;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangeUserNameDialog extends DialogFragment {

    public static final String REQUEST_KEY =
            "change_username_request";

    public static final String BUNDLE_USER_NAME =
            "new_user_name";

    private UserNameChangeDialogBoxBinding binding;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        binding = UserNameChangeDialogBoxBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        String username = SupabaseAuth.getUsername(
                requireContext()
        );

        binding.edtUserName.setText(
                username == null ? "" : username
        );

        binding.btnCancel.setOnClickListener(v ->
                dismiss()
        );

        binding.btnEditTask.setOnClickListener(v -> {

            String newName = binding.edtUserName
                    .getText()
                    .toString()
                    .trim();

            if (TextUtils.isEmpty(newName)) {

                binding.edtUserName.setError(
                        "Name is required"
                );

                return;
            }

            if (newName.length() < 3) {

                binding.edtUserName.setError(
                        "Username must contain at least 3 characters"
                );

                return;
            }

            binding.btnEditTask.setEnabled(false);

            updateUsername(newName);
        });
    }

    private void updateUsername(String newName) {

        String userId = SupabaseAuth.getUserId(
                requireContext()
        );

        String accessToken = SupabaseAuth.getAccessToken(
                requireContext()
        );

        if (userId == null || accessToken == null) {

            binding.btnEditTask.setEnabled(true);

            Toast.makeText(
                    requireContext(),
                    "Please login again",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        JsonObject json = new JsonObject();

        json.addProperty(
                "username",
                newName
        );

        MediaType JSON = MediaType.parse(
                "application/json; charset=utf-8"
        );

        RequestBody body = RequestBody.create(
                json.toString(),
                JSON
        );

        attemptUpdateUsername(newName, userId, accessToken, body, 0);
    }

    private void attemptUpdateUsername(
            String newName,
            String userId,
            String accessToken,
            RequestBody body,
            int tableIdx
    ) {
        String[] tables = new String[]{"profile", "profiles"};
        if (tableIdx >= tables.length) {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (binding != null) binding.btnEditTask.setEnabled(true);
                Toast.makeText(requireContext(), "Database update failed", Toast.LENGTH_LONG).show();
            });
            return;
        }

        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + tables[tableIdx] + "?id=eq." + userId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (tableIdx + 1 < tables.length) {
                    attemptUpdateUsername(newName, userId, accessToken, body, tableIdx + 1);
                    return;
                }
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) binding.btnEditTask.setEnabled(true);
                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean success = response.isSuccessful();
                String responseBody = response.body() != null ? response.body().string() : "";
                response.close();

                if (!success && (responseBody.contains("PGRST205") || response.code() == 404)) {
                    if (tableIdx + 1 < tables.length) {
                        attemptUpdateUsername(newName, userId, accessToken, body, tableIdx + 1);
                        return;
                    }
                }

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    binding.btnEditTask.setEnabled(true);

                    if (success) {
                        requireContext()
                                .getSharedPreferences("SupabaseSession", Context.MODE_PRIVATE)
                                .edit()
                                .putString("username", newName)
                                .apply();

                        Bundle result = new Bundle();
                        result.putString(BUNDLE_USER_NAME, newName);
                        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);

                        Toast.makeText(requireContext(), "Username updated", Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Database update failed", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }
}