package com.example.todolist.SharedPrefManager;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static SharedPrefManager instance;
    private final SharedPreferences preferences;

    private SharedPrefManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("TaskManagerPrefs", Context.MODE_PRIVATE);
        SharedPreferences oldSession = context.getApplicationContext().getSharedPreferences("SupabaseSession", Context.MODE_PRIVATE);
        if (preferences.getString("user_id", "").isEmpty() && oldSession.contains("user_id")) {
            preferences.edit()
                    .putString("user_id", oldSession.getString("user_id", ""))
                    .putString("access_token", oldSession.getString("access_token", ""))
                    .putString("refresh_token", oldSession.getString("refresh_token", ""))
                    .putString("email", oldSession.getString("email", ""))
                    .putString("username", oldSession.getString("username", ""))
                    .apply();
        }
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public String getString(String key) {
        return preferences.getString(key, "");
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
