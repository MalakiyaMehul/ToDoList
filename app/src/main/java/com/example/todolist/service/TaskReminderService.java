package com.example.todolist.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.todolist.R;
import com.example.todolist.SharedPrefManager.SharedPrefManager;
import com.example.todolist.activity.Supabase.SupabaseAuth;
import com.example.todolist.activity.Supabase.SupabaseRepository;
import com.example.todolist.activity.TaskDetail;
import com.example.todolist.activity.home.HomeActivity;

public class TaskReminderService extends Service {

    public static final String ACTION_START_REMINDER = "com.example.todolist.ACTION_START_REMINDER";
    public static final String ACTION_DISMISS_REMINDER = "com.example.todolist.ACTION_DISMISS_REMINDER";
    public static final String ACTION_COMPLETE_TASK = "com.example.todolist.ACTION_COMPLETE_TASK";

    public static final String CHANNEL_ID = "task_reminder_fg_v2";
    public static final String CHANNEL_NAME = "Persistent Task Reminders";
    public static final int NOTIFICATION_ID = 8801;

    private static String activeTaskId = "";

    public static String getActiveTaskId() {
        return activeTaskId;
    }

    public static void stopIfMatchingTask(Context context, String taskId) {
        if (context != null && taskId != null && taskId.equals(activeTaskId)) {
            Intent intent = new Intent(context, TaskReminderService.class);
            intent.setAction(ACTION_DISMISS_REMINDER);
            try {
                context.startService(intent);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISMISS_REMINDER.equals(intent.getAction())) {
            activeTaskId = "";
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_COMPLETE_TASK.equals(intent.getAction())) {
            String taskId = intent.getStringExtra("taskId");
            if (taskId != null && !taskId.isEmpty()) {
                String token = SharedPrefManager.getInstance(this).getString("access_token");
                if (token.isEmpty()) {
                    token = SupabaseAuth.getAccessToken(this);
                }
                new SupabaseRepository().updateTaskCompleted(taskId, token, true, null);
            }
            activeTaskId = "";
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            String taskId = intent.getStringExtra("taskId");
            String taskName = intent.getStringExtra("taskName");
            String category = intent.getStringExtra("category");
            int offsetMinutes = intent.getIntExtra("offsetMinutes", 15);

            activeTaskId = taskId != null ? taskId : "";

            if (taskName == null || taskName.isEmpty()) {
                taskName = "Task Reminder";
            }

            createNotificationChannel();

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            Intent viewIntent;
            if (taskId != null && !taskId.isEmpty()) {
                viewIntent = new Intent(this, TaskDetail.class);
                viewIntent.putExtra("taskId", taskId);
                viewIntent.putExtra("taskName", taskName);
                viewIntent.putExtra("category", category);
            } else {
                viewIntent = new Intent(this, HomeActivity.class);
            }
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int viewRequestCode = taskId != null ? taskId.hashCode() : 8802;
            PendingIntent viewPendingIntent = PendingIntent.getActivity(
                    this,
                    viewRequestCode,
                    viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Intent completeIntent = new Intent(this, TaskReminderService.class);
            completeIntent.setAction(ACTION_COMPLETE_TASK);
            completeIntent.putExtra("taskId", taskId);
            PendingIntent completePendingIntent = PendingIntent.getService(
                    this,
                    8804,
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String categoryLabel = (category != null && !category.isEmpty()) ? category : "Task";
            String contentText = categoryLabel + " • Starting in " + offsetMinutes + " mins!";
            String bigText = contentText + "\nNotification will remain active until task is marked as completed.";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_bell_pulse)
                    .setContentTitle("⏰ Pending Task: " + taskName)
                    .setContentText(contentText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 250, 500})
                    .setContentIntent(viewPendingIntent)
                    .addAction(R.drawable.ic_check_simple, "Mark Completed ✓", completePendingIntent)
                    .addAction(R.drawable.ic_eye, "View Task", viewPendingIntent);

            startForeground(NOTIFICATION_ID, builder.build());
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Persistent task reminder notification until task completion");
                channel.enableVibration(true);
                channel.setShowBadge(true);

                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();

                channel.setSound(soundUri, audioAttributes);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
