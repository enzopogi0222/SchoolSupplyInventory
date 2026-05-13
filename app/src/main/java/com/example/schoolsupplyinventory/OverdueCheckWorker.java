package com.example.schoolsupplyinventory;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.stream.Collectors;

public class OverdueCheckWorker extends Worker {
    private static final String TAG = "OverdueCheckWorker";
    private static final String CHANNEL_ID = "overdue_alerts";
    private static final int NOTIFICATION_ID = 101;

    public OverdueCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker started checking for overdue items");
        
        // We need to use a blocking call here as doWork runs on a background thread
        List<BorrowRecord> borrows = SupplyLab.get(getApplicationContext()).getActiveBorrowRecords();
        long currentTime = System.currentTimeMillis();
        
        List<BorrowRecord> overdueRecords = borrows.stream()
                .filter(r -> r.getExpectedReturnDate() != null && r.getExpectedReturnDate().getTime() < currentTime)
                .collect(Collectors.toList());

        if (!overdueRecords.isEmpty()) {
            sendOverdueNotification(overdueRecords.size());
        }

        return Result.success();
    }

    private void sendOverdueNotification(int count) {
        Context context = getApplicationContext();
        createNotificationChannel(context);

        Intent intent = new Intent(context, InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Overdue Items Alert")
                .setContentText("There are " + count + " items past their expected return date!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(Color.RED);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SupplyFlow Alerts";
            String description = "Notifications for items that have not been returned on time";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
