package com.example.schoolsupplyinventory;

import android.app.Application;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class InventoryApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Schedule periodic inventory checks for notifications
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest inventoryCheckRequest =
                new PeriodicWorkRequest.Builder(OverdueCheckWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "InventoryHealthCheck",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                inventoryCheckRequest
        );
    }
}
