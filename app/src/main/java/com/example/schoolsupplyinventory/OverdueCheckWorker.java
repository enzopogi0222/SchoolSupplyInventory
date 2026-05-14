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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class OverdueCheckWorker extends Worker {
    private static final String TAG = "OverdueCheckWorker";
    private static final String CHANNEL_ID_ALERTS = "supply_alerts";
    private static final int NOTIFY_OVERDUE = 101;
    private static final int NOTIFY_LOW_STOCK = 102;
    private static final int NOTIFY_EXPIRING = 103;
    private static final int NOTIFY_PENDING_REQUESTS = 104;

    public OverdueCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Notification check worker started");
        Context context = getApplicationContext();
        SupplyLab lab = SupplyLab.get(context);
        createNotificationChannel(context);

        // 1. Check for Overdue Items (Borrow due reminders)
        List<BorrowRecord> borrows = lab.getActiveBorrowRecords();
        long currentTime = System.currentTimeMillis();
        List<BorrowRecord> overdueRecords = borrows.stream()
                .filter(r -> r.getExpectedReturnDate() != null && r.getExpectedReturnDate().getTime() < currentTime)
                .collect(Collectors.toList());

        if (!overdueRecords.isEmpty()) {
            sendNotification(NOTIFY_OVERDUE, "Overdue Items Alert", 
                "There are " + overdueRecords.size() + " items past their return date!");
        }

        // 2. Check for Low Stock - Restricted to Consumables only
        List<SupplyItem> items = lab.getItems();
        List<SupplyItem> lowStockItems = items.stream()
                .filter(item -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(item.getItemType()) 
                        && item.getAvailableQuantity() > 0 
                        && item.getAvailableQuantity() <= 5)
                .collect(Collectors.toList());
        
        if (!lowStockItems.isEmpty()) {
            sendNotification(NOTIFY_LOW_STOCK, "Low Stock Warning", 
                lowStockItems.size() + " consumable items are running low on stock.");
        }

        // 3. Check for Expiring Items (within next 30 days)
        long thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000;
        List<SupplyItem> expiringItems = items.stream()
                .filter(item -> item.getExpirationDate() != null && 
                                item.getExpirationDate().getTime() > currentTime &&
                                item.getExpirationDate().getTime() < (currentTime + thirtyDaysMillis))
                .collect(Collectors.toList());

        if (!expiringItems.isEmpty()) {
            sendNotification(NOTIFY_EXPIRING, "Expiring Supplies Alert", 
                expiringItems.size() + " items will expire within the next 30 days.");
        }

        // 4. Check for Pending Requests (Request approval notifications)
        List<SupplyRequest> pendingRequests = lab.getPendingRequests();
        if (!pendingRequests.isEmpty()) {
            sendNotification(NOTIFY_PENDING_REQUESTS, "Pending Requests", 
                "There are " + pendingRequests.size() + " supply requests awaiting approval.");
        }

        return Result.success();
    }

    private void sendNotification(int id, String title, String text) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#673AB7")); // primary_purple

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(id, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission not granted", e);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "InventoSchool Inventory Alerts";
            String description = "Alerts for low stock, expiring items, and overdue borrows";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALERTS, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
