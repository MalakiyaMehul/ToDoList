
package com.example.todolist;

import android.content.Context;
import android.content.SharedPreferences;

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
        return getPrefs(context).getString("access_token", null);
    }

    public static String getRefreshToken(Context context) {
        return getPrefs(context).getString("refresh_token", null);
    }

    public static String getUserId(Context context) {
        return getPrefs(context).getString("user_id", null);
    }

    public static String getEmail(Context context) {
        return getPrefs(context).getString("email", null);
    }

    public static String getUsername(Context context) {
        return getPrefs(context).getString("username", null);
    }

    public static boolean isLoggedIn(Context context) {
        return getAccessToken(context) != null;
    }

    public static void saveSession(
            Context context,
            JsonObject response,
            String username
    ) {

        String accessToken = response.has("access_token")
                ? response.get("access_token").getAsString()
                : null;

        String refreshToken = response.has("refresh_token")
                ? response.get("refresh_token").getAsString()
                : null;

        JsonObject user = response.has("user")
                && response.get("user").isJsonObject()
                ? response.getAsJsonObject("user")
                : null;

        String userId = user != null && user.has("id")
                ? user.get("id").getAsString()
                : null;

        String email = user != null && user.has("email")
                ? user.get("email").getAsString()
                : null;

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

        body.add("data", data);

        Request request = new Request.Builder()
                .url(AUTH_URL + "/signup")
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        execute(request, callback);
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

        if (accessToken != null) {

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

        getPrefs(context).edit().clear().apply();
    }

    private static void execute(
            Request request,
            AuthCallback callback
    ) {

        SupabaseClient.client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                callback.onError(e.getMessage() != null
                        ? e.getMessage()
                        : "Network error");
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

                    callback.onSuccess(json);

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

                    callback.onError(errorMessage);
                }

                response.close();
            }
        });
    }
}
