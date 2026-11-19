package com.example.todolist.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.todolist.model.TaskModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskReminderScheduler {

    public static void scheduleTaskReminder(Context context, TaskModel taskModel, int offsetMinutes, boolean isEnabled) {
        if (context == null || taskModel == null || taskModel.getTaskId() == null || taskModel.getTaskId().isEmpty()) {
            return;
        }

        TaskReminderReceiver.createNotificationChannel(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int requestCode = taskModel.getTaskId().hashCode();

        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.putExtra("taskId", taskModel.getTaskId());
        intent.putExtra("taskName", taskModel.getTaskName());
        intent.putExtra("category", taskModel.getCategory());
        intent.putExtra("offsetMinutes", offsetMinutes);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!isEnabled) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        long triggerTimeMillis = calculateTriggerTime(taskModel.getDate(), taskModel.getTime(), offsetMinutes);
        if (triggerTimeMillis <= System.currentTimeMillis()) {
            // Schedule immediate alert in 3s so the notification is guaranteed to fire for the user
            triggerTimeMillis = System.currentTimeMillis() + 3000L;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            }
        } catch (Exception e) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            } catch (Exception ignored) {}
        }
    }

    public static void cancelTaskReminder(Context context, String taskId) {
        if (context == null || taskId == null || taskId.isEmpty()) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int requestCode = taskId.hashCode();
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    private static long calculateTriggerTime(String dateStr, String timeStr, int offsetMinutes) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return 0L;
        }

        Calendar calendar = Calendar.getInstance();

        Date parsedDate = null;
        String[] dateFormats = new String[]{"dd/MM/yyyy", "d/M/yyyy", "MMM dd, yyyy", "MMM d, yyyy", "yyyy-MM-dd"};
        for (String fmt : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                sdf.setLenient(false);
                parsedDate = sdf.parse(dateStr.trim());
                if (parsedDate != null) break;
            } catch (Exception ignored) {}
        }

        if (parsedDate == null) return 0L;

        Calendar dateCal = Calendar.getInstance();
        dateCal.setTime(parsedDate);

        calendar.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, dateCal.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH));

        if (timeStr != null && !timeStr.trim().isEmpty()) {
            String cleanTime = timeStr.trim();
            if (cleanTime.contains("•")) {
                cleanTime = cleanTime.substring(0, cleanTime.indexOf("•")).trim();
            }
            if (cleanTime.contains("(")) {
                cleanTime = cleanTime.substring(0, cleanTime.indexOf("(")).trim();
            }

            Date parsedTime = null;
            String[] timeFormats = new String[]{"hh:mm a", "h:mm a", "HH:mm", "H:m"};
            for (String fmt : timeFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                    sdf.setLenient(false);
                    parsedTime = sdf.parse(cleanTime);
                    if (parsedTime != null) break;
                } catch (Exception ignored) {}
            }

            if (parsedTime != null) {
                Calendar timeCal = Calendar.getInstance();
                timeCal.setTime(parsedTime);
                calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
        }

        long targetMillis = calendar.getTimeInMillis();
        return targetMillis - (offsetMinutes * 60 * 1000L);
    }
}
