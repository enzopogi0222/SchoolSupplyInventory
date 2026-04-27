package com.example.schoolsupplyinventory;

import java.util.UUID;

public class SupplyItem {

    private UUID mId;
    private String mName;
    private boolean mIsAvailable;

    public SupplyItem() {
        mId = UUID.randomUUID();
    }

    public SupplyItem(String name, boolean isAvailable) {
        this();
        mName = name;
        mIsAvailable = isAvailable;
    }

    public UUID getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isAvailable() {
        return mIsAvailable;
    }

    public void setAvailable(boolean available) {
        mIsAvailable = available;
    }
}
