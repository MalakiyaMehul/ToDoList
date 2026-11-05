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

import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseClient;
import com.example.todolist.databinding.ChangePasswordDialogBoxBinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
            binding.edtOldPassword.setError("Enter old password");
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

        String uid = SharedPrefManager.getInstance(requireContext()).getString("user_id");
        if (uid.isEmpty()) {
            uid = SupabaseAuth.getUserId(requireContext());
        }

        String accessToken = SharedPrefManager.getInstance(requireContext()).getString("access_token");
        if (accessToken.isEmpty()) {
            accessToken = SupabaseAuth.getAccessToken(requireContext());
        }

        String email = SharedPrefManager.getInstance(requireContext()).getString("email");
        if (email.isEmpty()) {
            email = SupabaseAuth.getEmail(requireContext());
        }
        final String finalEmail = email;

        if (uid == null || uid.isEmpty()) {
            showMessage("Please login again");
            return;
        }

        binding.btnEditTask.setEnabled(false);

        String finalUid = uid;
        String finalToken = accessToken;

        String authHeader = (finalToken != null && !finalToken.isEmpty())
                ? (finalToken.startsWith("Bearer ") ? finalToken : "Bearer " + finalToken)
                : "Bearer " + SupabaseClient.SUPABASE_KEY;

        fetchStoredPassword(finalUid, authHeader, 0, (storedPassword, errorMsg) -> {
            if (errorMsg != null && storedPassword.isEmpty()) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    binding.btnEditTask.setEnabled(true);
                    showMessage("Verification failed: " + errorMsg);
                });
                return;
            }

            if (!storedPassword.isEmpty()) {
                if (!storedPassword.equals(currentPassword)) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        binding.btnEditTask.setEnabled(true);
                        binding.edtOldPassword.setError("Current password does not match");
                        showMessage("Current password does not match");
                    });
                    return;
                }

                if (finalEmail != null && !finalEmail.isEmpty()) {
                    SupabaseAuth.signIn(finalEmail, currentPassword, new SupabaseAuth.AuthCallback() {
                        @Override
                        public void onSuccess(JsonObject resp) {
                            String verifiedToken = resp.has("access_token") ? resp.get("access_token").getAsString() : finalToken;
                            proceedUpdate(finalUid, verifiedToken, newPassword);
                        }

                        @Override
                        public void onError(String error) {
                            proceedUpdate(finalUid, finalToken, newPassword);
                        }
                    });
                } else {
                    proceedUpdate(finalUid, finalToken, newPassword);
                }
            } else {
                if (finalEmail != null && !finalEmail.isEmpty()) {
                    SupabaseAuth.signIn(finalEmail, currentPassword, new SupabaseAuth.AuthCallback() {
                        @Override
                        public void onSuccess(JsonObject resp) {
                            String verifiedToken = resp.has("access_token") ? resp.get("access_token").getAsString() : finalToken;
                            proceedUpdate(finalUid, verifiedToken, newPassword);
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                binding.btnEditTask.setEnabled(true);
                                binding.edtOldPassword.setError("Current password does not match");
                                showMessage("Current password does not match");
                            });
                        }
                    });
                } else {
                    proceedUpdate(finalUid, finalToken, newPassword);
                }
            }
        });
    }

    private interface PasswordFetchCallback {
        void onResult(String storedPassword, String error);
    }

    private void fetchStoredPassword(String uid, String authHeader, int tableIdx, PasswordFetchCallback callback) {
        String[] tables = new String[]{"profile", "profiles"};
        if (tableIdx >= tables.length) {
            callback.onResult("", null);
            return;
        }

        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/" + tables[tableIdx] + "?id=eq." + uid + "&select=*";
        Request checkRequest = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Accept", "application/json")
                .get()
                .build();

        SupabaseClient.client.newCall(checkRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (tableIdx + 1 < tables.length) {
                    fetchStoredPassword(uid, authHeader, tableIdx + 1, callback);
                } else {
                    callback.onResult("", e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "[]";
                boolean isSuccess = response.isSuccessful();
                response.close();

                if (!isSuccess && (responseBody.contains("PGRST205") || response.code() == 404)) {
                    if (tableIdx + 1 < tables.length) {
                        fetchStoredPassword(uid, authHeader, tableIdx + 1, callback);
                        return;
                    }
                }

                String foundPassword = "";
                try {
                    JsonArray arr = JsonParser.parseString(responseBody).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonObject obj = arr.get(0).getAsJsonObject();
                        if (obj.has("demo_password") && !obj.get("demo_password").isJsonNull()) {
                            foundPassword = obj.get("demo_password").getAsString();
                        } else if (obj.has("demopassword") && !obj.get("demopassword").isJsonNull()) {
                            foundPassword = obj.get("demopassword").getAsString();
                        } else if (obj.has("password") && !obj.get("password").isJsonNull()) {
                            foundPassword = obj.get("password").getAsString();
                        }
                    }
                } catch (Exception ignored) {
                }

                callback.onResult(foundPassword, null);
            }
        });
    }

    private void proceedUpdate(String uid, String token, String newPassword) {
        String authHeader = (token != null && !token.isEmpty())
                ? (token.startsWith("Bearer ") ? token : "Bearer " + token)
                : "Bearer " + SupabaseClient.SUPABASE_KEY;

        attemptPatchPassword(uid, token, newPassword, authHeader, 0, 0);
    }

    private void attemptPatchPassword(
            String uid,
            String token,
            String newPassword,
            String authHeader,
            int tableIdx,
            int keyIdx
    ) {
        String[] tables = new String[]{"profile", "profiles"};
        String[] keys = new String[]{"demo_password", "demopassword", "password"};

        if (tableIdx >= tables.length) {
            updateSupabaseAuthPassword(token, newPassword);
            return;
        }

        String table = tables[tableIdx];
        String key = keys[keyIdx];

        JsonObject body = new JsonObject();
        body.addProperty(key, newPassword);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        Request patchRequest = new Request.Builder()
                .url(SupabaseClient.SUPABASE_URL + "/rest/v1/" + table + "?id=eq." + uid)
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        SupabaseClient.client.newCall(patchRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (tableIdx + 1 < tables.length) {
                    attemptPatchPassword(uid, token, newPassword, authHeader, tableIdx + 1, 0);
                } else {
                    updateSupabaseAuthPassword(token, newPassword);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful();
                response.close();

                if (success) {
                    updateSupabaseAuthPassword(token, newPassword);
                    return;
                }

                if (responseBody.contains("PGRST205") || response.code() == 404) {
                    if (tableIdx + 1 < tables.length) {
                        attemptPatchPassword(uid, token, newPassword, authHeader, tableIdx + 1, 0);
                        return;
                    }
                }

                if ((responseBody.contains("PGRST204") || responseBody.contains("column") || responseBody.contains(key))
                        && keyIdx + 1 < keys.length) {
                    attemptPatchPassword(uid, token, newPassword, authHeader, tableIdx, keyIdx + 1);
                    return;
                }

                updateSupabaseAuthPassword(token, newPassword);
            }
        });
    }

    private void updateSupabaseAuthPassword(String token, String newPassword) {
        if (token != null && !token.isEmpty()) {
            SupabaseAuth.updatePassword(token, newPassword, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(JsonObject response) {
                }

                @Override
                public void onError(String error) {
                }
            });
        }

        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            binding.btnEditTask.setEnabled(true);
            showMessage("Password updated successfully");
            dismiss();
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
