package com.example.todolist.activity.Supabase;

import static com.example.todolist.activity.Supabase.SupabaseClient.SUPABASE_KEY;
import static com.example.todolist.activity.Supabase.SupabaseClient.SUPABASE_URL;
import static com.example.todolist.activity.Supabase.SupabaseClient.client;

import android.os.Handler;
import android.os.Looper;

import com.example.todolist.model.TaskModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseRepository {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface TaskCallback {
        void onSuccess(String taskId);
        void onError(String error);
    }

    public interface UserCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface GetTasksCallback {
        void onSuccess(List<TaskModel> tasks);
        void onError(String error);
    }

    private static final String[] PROFILE_TABLES = {"profile", "profiles"};
    private static final String[] PASSWORD_KEYS = {"demo_password", "demopassword", "password"};

    public void saveUser(String userId, String username, String email, String password, UserCallback callback) {
        saveUser(userId, username, email, password, null, callback);
    }

    public void saveUser(String userId, String username, String email, String password, String accessToken, UserCallback callback) {
        String authHeader = (accessToken != null && !accessToken.trim().isEmpty())
                ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                : "Bearer " + SUPABASE_KEY;

        attemptSaveUser(userId, username, email, password, authHeader, 0, 0, callback);
    }

    private void attemptSaveUser(
            String userId,
            String username,
            String email,
            String password,
            String authHeader,
            int tableIdx,
            int keyIdx,
            UserCallback callback
    ) {
        if (tableIdx >= PROFILE_TABLES.length) {
            new Handler(Looper.getMainLooper()).post(callback::onSuccess);
            return;
        }

        String table = PROFILE_TABLES[tableIdx];
        String url = SUPABASE_URL + "/rest/v1/" + table;

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", userId);
            jsonObject.put("username", username);
            jsonObject.put("email", email);
            if (keyIdx < PASSWORD_KEYS.length) {
                jsonObject.put(PASSWORD_KEYS[keyIdx], password);
            }

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (tableIdx + 1 < PROFILE_TABLES.length) {
                        attemptSaveUser(userId, username, email, password, authHeader, tableIdx + 1, 0, callback);
                    } else {
                        new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    boolean isSuccess = response.isSuccessful();
                    String responseBody = response.body() != null ? response.body().string() : "";
                    response.close();

                    if (isSuccess) {
                        new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                        return;
                    }

                    if (responseBody.contains("PGRST205") || response.code() == 404) {
                        if (tableIdx + 1 < PROFILE_TABLES.length) {
                            attemptSaveUser(userId, username, email, password, authHeader, tableIdx + 1, 0, callback);
                            return;
                        }
                    }

                    if ((responseBody.contains("PGRST204") || responseBody.contains("column") || responseBody.contains("schema cache"))
                            && keyIdx + 1 < PASSWORD_KEYS.length) {
                        attemptSaveUser(userId, username, email, password, authHeader, tableIdx, keyIdx + 1, callback);
                        return;
                    }

                    Request patchRequest = new Request.Builder()
                            .url(url + "?id=eq." + userId)
                            .addHeader("apikey", SUPABASE_KEY)
                            .addHeader("Authorization", authHeader)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=minimal")
                            .patch(RequestBody.create(jsonObject.toString(), JSON))
                            .build();

                    client.newCall(patchRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call1, IOException e1) {
                            new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                        }

                        @Override
                        public void onResponse(Call call1, Response response1) {
                            boolean patchSuccess = response1.isSuccessful();
                            response1.close();
                            if (patchSuccess) {
                                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                            } else if (tableIdx + 1 < PROFILE_TABLES.length) {
                                attemptSaveUser(userId, username, email, password, authHeader, tableIdx + 1, 0, callback);
                            } else {
                                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(callback::onSuccess);
        }
    }

    public void getTasks(String userId, String accessToken, GetTasksCallback callback) {

        if (userId == null || userId.trim().isEmpty()) {
            callback.onError("User ID is missing");
            return;
        }

        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Access token is missing. Please login again.");
            return;
        }

        String authHeader = accessToken.startsWith("Bearer ")
                ? accessToken
                : "Bearer " + accessToken;

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/tasks?user_id=eq." + userId + "&select=*")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(
                        () -> callback.onError(e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseData = response.body() != null
                        ? response.body().string()
                        : "";

                if (!response.isSuccessful()) {
                    String error = "Code: " + response.code() + " " + responseData;
                    response.close();

                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onError(error)
                    );
                    return;
                }

                response.close();

                try {
                    JSONArray jsonArray = new JSONArray(responseData);

                    List<TaskModel> taskList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {

                        JSONObject object = jsonArray.getJSONObject(i);

                        TaskModel model = new TaskModel();

                        model.setTaskId(object.optString("task_id", ""));
                        String uname = object.optString("username", "");
                        model.setUsername(uname);
                        
                        model.setTaskName(object.optString("task_name", ""));
                        model.setDescription(object.optString("description", ""));
                        model.setDate(object.optString("date", ""));
                        model.setTime(object.optString("time", ""));
                        model.setCategory(object.optString("category", ""));
                        model.setCategoryColor(object.optString("category_color", ""));
                        model.setCategoryIcon(object.optString("category_icon", ""));
                        model.setPriority(object.optString("priority", ""));
                        model.setCompleted(object.optBoolean("completed", false));

                        taskList.add(model);
                    }

                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onSuccess(taskList)
                    );

                } catch (Exception e) {

                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onError("Data parsing error: " + e.getMessage())
                    );
                }
            }
        });
    }

    public void saveTask(
            String userId,
            String username,
            String accessToken,
            String taskName,
            String description,
            String date, String time, String category, String categoryColor, String categoryIcon, String priority,
            boolean completed,
            TaskCallback callback) {

        try {
            String taskId = UUID.randomUUID().toString();

            JSONObject jsonObject = new JSONObject();

            jsonObject.put("task_id", taskId);
            jsonObject.put("user_id", userId);
            if (username != null && !username.trim().isEmpty()) {
                jsonObject.put("username", username.trim());
            }
            jsonObject.put("task_name", taskName);
            jsonObject.put("description", description);
            jsonObject.put("date", date);
            jsonObject.put("time", time);
            jsonObject.put("category", category);
            jsonObject.put("category_color", categoryColor);
            jsonObject.put("category_icon", categoryIcon);
            jsonObject.put("priority", priority);
            jsonObject.put("completed", completed);

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/tasks")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(body);

            String authHeader = (accessToken != null && !accessToken.isEmpty())
                    ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                    : "Bearer " + SUPABASE_KEY;
            requestBuilder.addHeader("Authorization", authHeader);

            client.newCall(requestBuilder.build()).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    new Handler(Looper.getMainLooper()).post(
                            () -> callback.onError(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {

                    String responseData = response.body() != null
                            ? response.body().string()
                            : "No response body";

                    if (response.isSuccessful()) {
                        new Handler(Looper.getMainLooper()).post(
                                () -> callback.onSuccess(taskId)
                        );
                    } else {
                        new Handler(Looper.getMainLooper()).post(
                                () -> callback.onError(
                                        "Code: " + response.code() + " " + responseData
                                )
                        );
                    }

                    response.close();
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public void updateTaskCompleted(String taskId, String accessToken, boolean completed, TaskCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("completed", completed);

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
            String authHeader = (accessToken != null && !accessToken.isEmpty())
                    ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                    : "Bearer " + SUPABASE_KEY;

            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/tasks?task_id=eq." + taskId)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(body);

            client.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";
                    boolean success = response.isSuccessful();
                    response.close();

                    if (success && !responseData.trim().equals("[]")) {
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                        }
                        return;
                    }

                    Request.Builder fallbackBuilder = new Request.Builder()
                            .url(SUPABASE_URL + "/rest/v1/tasks?id=eq." + taskId)
                            .addHeader("apikey", SUPABASE_KEY)
                            .addHeader("Authorization", authHeader)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=representation")
                            .patch(body);

                    client.newCall(fallbackBuilder.build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call1, IOException e1) {
                            if (callback != null) {
                                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e1.getMessage()));
                            }
                        }

                        @Override
                        public void onResponse(Call call1, Response response1) {
                            boolean ok = response1.isSuccessful();
                            response1.close();
                            if (callback != null) {
                                if (ok) {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                                } else {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Update failed"));
                                }
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    public void updateTask(
            String taskId,
            String accessToken,
            String taskName,
            String description,
            String date,
            String time,
            String category,
            String categoryColor,
            String categoryIcon,
            String priority,
            boolean completed,
            TaskCallback callback) {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("task_name", taskName);
            jsonObject.put("description", description);
            jsonObject.put("date", date);
            jsonObject.put("time", time);
            jsonObject.put("category", category);
            jsonObject.put("category_color", categoryColor);
            jsonObject.put("category_icon", categoryIcon);
            jsonObject.put("priority", priority);
            jsonObject.put("completed", completed);

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            String authHeader = (accessToken != null && !accessToken.isEmpty())
                    ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                    : "Bearer " + SUPABASE_KEY;

            Request.Builder requestBuilder = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/tasks?task_id=eq." + taskId)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(body);

            client.newCall(requestBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseData = response.body() != null ? response.body().string() : "";
                    boolean success = response.isSuccessful();
                    response.close();

                    if (success && !responseData.trim().equals("[]")) {
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                        }
                        return;
                    }

                    Request.Builder fallbackBuilder = new Request.Builder()
                            .url(SUPABASE_URL + "/rest/v1/tasks?id=eq." + taskId)
                            .addHeader("apikey", SUPABASE_KEY)
                            .addHeader("Authorization", authHeader)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "return=representation")
                            .patch(body);

                    client.newCall(fallbackBuilder.build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call1, IOException e1) {
                            if (callback != null) {
                                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e1.getMessage()));
                            }
                        }

                        @Override
                        public void onResponse(Call call1, Response response1) {
                            boolean ok = response1.isSuccessful();
                            response1.close();
                            if (callback != null) {
                                if (ok) {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                                } else {
                                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Update failed"));
                                }
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        }
    }

    public void deleteTask(String taskId, String accessToken, TaskCallback callback) {
        String authHeader = (accessToken != null && !accessToken.isEmpty())
                ? (accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken)
                : "Bearer " + SUPABASE_KEY;

        Request.Builder requestBuilder = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/tasks?task_id=eq." + taskId)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Prefer", "return=representation")
                .delete();

        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful();
                response.close();

                if (success && !responseData.trim().equals("[]")) {
                    if (callback != null) {
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                    }
                    return;
                }

                Request.Builder fallbackBuilder = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/tasks?id=eq." + taskId)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", authHeader)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .delete();

                client.newCall(fallbackBuilder.build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e1) {
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> callback.onError(e1.getMessage()));
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response1) {
                        boolean ok = response1.isSuccessful();
                        response1.close();
                        if (callback != null) {
                            if (ok) {
                                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(taskId));
                            } else {
                                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Delete failed"));
                            }
                        }
                    }
                });
            }
        });
    }
}
