package com.harmonica.application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ModeReminderWorker extends Worker {

    public ModeReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String modeName = getInputData().getString("mode_name");
        if (modeName == null) modeName = "Support Mode";

        sendNotification(modeName);
        return Result.success();
    }

    private void sendNotification(String mode) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "harmonica_reminders";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Harmonica Care", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        String advice = getAdviceForMode(mode);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_heart)
                .setContentTitle("Harmonica: " + mode)
                .setContentText(advice)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(advice))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private String getAdviceForMode(String mode) {
        if (mode.contains("Panic")) return "Take a deep breath. Let's try a grounding exercise together in the Zen Space.";
        if (mode.contains("Depression")) return "I'm thinking of you. Even a small step like talking for 2 minutes can help.";
        if (mode.contains("Anxiety")) return "Your mind is racing. Let's try to find some calm in the app.";
        if (mode.contains("Stress")) return "High stress detected. It's time for a 4-second breathing break.";
        return "I'm here for you. Check in when you're ready.";
    }
}
