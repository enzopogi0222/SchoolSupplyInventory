package com.example.schoolsupplyinventory;

public class SupplyItem {

    private String mName;
    private boolean mIsAvailable;

    public SupplyItem(String name, boolean isAvailable) {
        mName = name;
        mIsAvailable = isAvailable;
    }

    public String getName() {
        return mName;
    }

    public boolean isAvailable() {
        return mIsAvailable;
    }
}
