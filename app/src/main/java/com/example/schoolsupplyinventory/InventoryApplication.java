package com.example.schoolsupplyinventory;

import android.app.Application;

public class InventoryApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Background scheduling removed - notifications will only occur on app open
    }
}
