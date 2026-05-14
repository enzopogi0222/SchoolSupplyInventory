package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class HistoryRecord {
    private UUID mId;
    private UUID mItemId;
    private String mItemName;
    private String mAction;
    private String mUser;
    private Date mTimestamp;
    private String mDetails;

    public HistoryRecord() {
        this(UUID.randomUUID());
    }

    public HistoryRecord(UUID id) {
        mId = id;
        mTimestamp = new Date();
    }

    public UUID getId() { return mId; }
    public UUID getItemId() { return mItemId; }
    public void setItemId(UUID itemId) { mItemId = itemId; }
    public String getItemName() { return mItemName; }
    public void setItemName(String itemName) { mItemName = itemName; }
    public String getAction() { return mAction; }
    public void setAction(String action) { mAction = action; }
    public String getUser() { return mUser; }
    public void setUser(String user) { mUser = user; }
    public Date getTimestamp() { return mTimestamp; }
    public void setTimestamp(Date timestamp) { mTimestamp = timestamp; }
    public String getDetails() { return mDetails; }
    public void setDetails(String details) { mDetails = details; }
}
