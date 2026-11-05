package com.example.todolist.activity.Supabase;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuth {

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private static final String AUTH_URL =
            SupabaseClient.SUPABASE_URL + "/auth/v1";

    private static final String PREF_NAME = "SupabaseSession";

    public interface AuthCallback {
        void onSuccess(JsonObject response);
        void onError(String error);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public static String getAccessToken(Context context) {
        String token = SharedPrefManager.getInstance(context).getString("access_token");
        if (token == null || token.isEmpty()) {
            token = getPrefs(context).getString("access_token", "");
            if (token != null && !token.isEmpty()) {
                SharedPrefManager.getInstance(context).setString("access_token", token);
            }
        }
        return token;
    }

    public static String getRefreshToken(Context context) {
        String token = SharedPrefManager.getInstance(context).getString("refresh_token");
        if (token == null || token.isEmpty()) {
            token = getPrefs(context).getString("refresh_token", "");
            if (token != null && !token.isEmpty()) {
                SharedPrefManager.getInstance(context).setString("refresh_token", token);
            }
        }
        return token;
    }

    public static String getUserId(Context context) {
        String uid = SharedPrefManager.getInstance(context).getString("user_id");
        if (uid == null || uid.isEmpty()) {
            uid = getPrefs(context).getString("user_id", "");
            if (uid != null && !uid.isEmpty()) {
                SharedPrefManager.getInstance(context).setString("user_id", uid);
            }
        }
        return uid;
    }

    public static String getEmail(Context context) {
        String email = SharedPrefManager.getInstance(context).getString("email");
        if (email == null || email.isEmpty()) {
            email = getPrefs(context).getString("email", "");
            if (email != null && !email.isEmpty()) {
                SharedPrefManager.getInstance(context).setString("email", email);
            }
        }
        return email;
    }

    public static String getUsername(Context context) {
        String username = SharedPrefManager.getInstance(context).getString("username");
        if (username == null || username.isEmpty()) {
            username = getPrefs(context).getString("username", "");
            if (username != null && !username.isEmpty()) {
                SharedPrefManager.getInstance(context).setString("username", username);
            }
        }
        return username;
    }

    public static boolean isLoggedIn(Context context) {
        String token = getAccessToken(context);
        return token != null && !token.isEmpty();
    }

    public static void saveSession(
            Context context,
            JsonObject response,
            String username) {

        String accessToken = response.has("access_token")
                ? response.get("access_token").getAsString()
                : (response.has("session") && response.get("session").isJsonObject() && response.getAsJsonObject("session").has("access_token")
                        ? response.getAsJsonObject("session").get("access_token").getAsString()
                        : "");

        String refreshToken = response.has("refresh_token")
                ? response.get("refresh_token").getAsString()
                : (response.has("session") && response.get("session").isJsonObject() && response.getAsJsonObject("session").has("refresh_token")
                        ? response.getAsJsonObject("session").get("refresh_token").getAsString()
                        : "");

        JsonObject user = response.has("user")
                && response.get("user").isJsonObject()
                ? response.getAsJsonObject("user")
                : (response.has("session") && response.get("session").isJsonObject() && response.getAsJsonObject("session").has("user") && response.getAsJsonObject("session").get("user").isJsonObject()
                        ? response.getAsJsonObject("session").getAsJsonObject("user")
                        : null);

        String userId = user != null && user.has("id")
                ? user.get("id").getAsString()
                : "";

        String email = user != null && user.has("email")
                ? user.get("email").getAsString()
                : "";

        SharedPrefManager.getInstance(context).setString("access_token", accessToken);
        SharedPrefManager.getInstance(context).setString("refresh_token", refreshToken);
        SharedPrefManager.getInstance(context).setString("user_id", userId);
        SharedPrefManager.getInstance(context).setString("email", email);
        SharedPrefManager.getInstance(context).setString("username", username != null ? username : "");

        getPrefs(context).edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putString("user_id", userId)
                .putString("email", email)
                .putString("username", username)
                .apply();
    }

    public static void signUp(
            String email,
            String password,
            String username,
            AuthCallback callback
    ) {

        JsonObject body = new JsonObject();

        body.addProperty("email", email);
        body.addProperty("password", password);

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);
        data.addProperty("demo_password", password);
        data.addProperty("demopassword", password);

        body.add("data", data);

        Request request = new Request.Builder()
                .url(AUTH_URL + "/signup")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        execute(request, callback);
    }

    public static void savePasswordToProfile(
            String userId,
            String accessToken,
            String username,
            String email,
            String password,
            AuthCallback callback
    ) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) callback.onError("Empty user id");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("id", userId);
        if (username != null && !username.isEmpty()) {
            body.addProperty("username", username);
        }
        if (email != null && !email.isEmpty()) {
            body.addProperty("email", email);
        }
        body.addProperty("password", password);

        String authHeader = (accessToken != null && !accessToken.isEmpty())
                ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                : "Bearer " + SupabaseClient.SUPABASE_KEY;

        Request.Builder builder = new Request.Builder()
                .url(SupabaseClient.SUPABASE_URL + "/rest/v1/profiles")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(RequestBody.create(body.toString(), JSON));

        SupabaseClient.client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    if (callback != null) callback.onSuccess(new JsonObject());
                    response.close();
                    return;
                }
                response.close();

                Request.Builder patchBuilder = new Request.Builder()
                        .url(SupabaseClient.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId)
                        .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                        .addHeader("Authorization", authHeader)
                        .addHeader("Content-Type", "application/json")
                        .patch(RequestBody.create(body.toString(), JSON));

                SupabaseClient.client.newCall(patchBuilder.build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call1, IOException e1) {
                        if (callback != null) callback.onError(e1.getMessage());
                    }

                    @Override
                    public void onResponse(Call call1, Response response1) {
                        if (response1.isSuccessful()) {
                            if (callback != null) callback.onSuccess(new JsonObject());
                        } else {
                            if (callback != null) callback.onError("Code: " + response1.code());
                        }
                        response1.close();
                    }
                });
            }
        });
    }

    public static void signIn(
            String email,
            String password,
            AuthCallback callback
    ) {

        JsonObject body = new JsonObject();

        body.addProperty("email", email);
        body.addProperty("password", password);

        Request request = new Request.Builder()
                .url(AUTH_URL + "/token?grant_type=password")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        execute(request, callback);
    }

    public static void updatePassword(
            String accessToken,
            String newPassword,
            AuthCallback callback
    ) {

        JsonObject body = new JsonObject();
        body.addProperty("password", newPassword);

        Request request = new Request.Builder()
                .url(AUTH_URL + "/user")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(RequestBody.create(body.toString(), JSON))
                .build();

        execute(request, callback);
    }

    public static void signOut(Context context) {

        String accessToken = getAccessToken(context);

        if (accessToken != null && !accessToken.isEmpty()) {

            Request request = new Request.Builder()
                .url(AUTH_URL + "/logout")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(RequestBody.create("", JSON))
                .build();

            SupabaseClient.client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                }

                @Override
                public void onResponse(Call call, Response response) {
                    response.close();
                }
            });
        }

        SharedPrefManager.getInstance(context).clear();
        getPrefs(context).edit().clear().apply();
    }

    private static void execute(
            Request request,
            AuthCallback callback
    ) {

        SupabaseClient.client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.onError(e.getMessage() != null
                            ? e.getMessage()
                            : "Network error");
                }
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                String responseBody = response.body() != null
                        ? response.body().string()
                        : "{}";

                if (response.isSuccessful()) {

                    JsonObject json = JsonParser
                            .parseString(responseBody)
                            .getAsJsonObject();

                    if (callback != null) {
                        callback.onSuccess(json);
                    }

                } else {

                    String errorMessage = responseBody;

                    try {
                        JsonObject errorJson = JsonParser
                                .parseString(responseBody)
                                .getAsJsonObject();

                        if (errorJson.has("msg")) {
                            errorMessage =
                                    errorJson.get("msg").getAsString();
                        } else if (errorJson.has("message")) {
                            errorMessage =
                                    errorJson.get("message").getAsString();
                        }

                    } catch (Exception ignored) {
                    }

                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }

                response.close();
            }
        });
    }
}
