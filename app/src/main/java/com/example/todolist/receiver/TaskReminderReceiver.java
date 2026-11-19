package com.example.todolist.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.example.todolist.service.TaskReminderService;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static void createNotificationChannel(Context context) {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        Intent serviceIntent = new Intent(context, TaskReminderService.class);
        serviceIntent.setAction(TaskReminderService.ACTION_START_REMINDER);
        serviceIntent.putExtra("taskId", intent.getStringExtra("taskId"));
        serviceIntent.putExtra("taskName", intent.getStringExtra("taskName"));
        serviceIntent.putExtra("category", intent.getStringExtra("category"));
        serviceIntent.putExtra("offsetMinutes", intent.getIntExtra("offsetMinutes", 15));

        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (Exception e) {
            try {
                context.startService(serviceIntent);
            } catch (Exception ignored) {}
        }
    }
}
