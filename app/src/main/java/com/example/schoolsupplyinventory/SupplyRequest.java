package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class SupplyRequest {
    public static final String TYPE_BORROW = "BORROW";
    public static final String TYPE_CONSUME = "CONSUME";
    public static final String TYPE_RETURN = "RETURN";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private UUID mId;
    private UUID mItemId;
    private String mItemTitle;
    private String mRequesterName;
    private int mQuantity;
    private Date mDateRequested;
    private String mStatus;
    private String mRequestType;
    private String mPurpose;
    private Date mExpectedReturnDate;
    private String mUnitId;
    private UUID mBorrowRecordId; // ID of the specific borrow record being returned

    public SupplyRequest() {
        this(UUID.randomUUID());
    }

    public SupplyRequest(UUID id) {
        mId = id;
        mDateRequested = new Date();
        mStatus = STATUS_PENDING;
        mRequestType = TYPE_CONSUME;
    }

    public UUID getId() {
        return mId;
    }

    public UUID getItemId() {
        return mItemId;
    }

    public void setItemId(UUID itemId) {
        mItemId = itemId;
    }

    public String getItemTitle() {
        return mItemTitle;
    }

    public void setItemTitle(String itemTitle) {
        mItemTitle = itemTitle;
    }

    public String getRequesterName() {
        return mRequesterName;
    }

    public void setRequesterName(String requesterName) {
        mRequesterName = requesterName;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public void setQuantity(int quantity) {
        mQuantity = quantity;
    }

    public Date getDateRequested() {
        return mDateRequested;
    }

    public void setDateRequested(Date dateRequested) {
        mDateRequested = dateRequested;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public String getRequestType() {
        return mRequestType;
    }

    public void setRequestType(String requestType) {
        mRequestType = requestType;
    }

    public String getPurpose() {
        return mPurpose;
    }

    public void setPurpose(String purpose) {
        mPurpose = purpose;
    }

    public Date getExpectedReturnDate() {
        return mExpectedReturnDate;
    }

    public void setExpectedReturnDate(Date expectedReturnDate) {
        mExpectedReturnDate = expectedReturnDate;
    }

    public String getUnitId() {
        return mUnitId;
    }

    public void setUnitId(String unitId) {
        mUnitId = unitId;
    }

    public UUID getBorrowRecordId() {
        return mBorrowRecordId;
    }

    public void setBorrowRecordId(UUID borrowRecordId) {
        mBorrowRecordId = borrowRecordId;
    }
}
