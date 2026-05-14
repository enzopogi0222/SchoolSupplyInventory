package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class BorrowRecord {
    private final UUID mId;
    private UUID mItemId;
    private String mBorrowerName;
    private int mQuantity;
    private int mInitialQuantity;
    private Date mDateBorrowed;
    private Date mExpectedReturnDate;
    private Date mActualReturnDate;
    private String mStatus;
    private String mUnitId; // Unique ID of the physical piece borrowed

    public BorrowRecord() {
        this(UUID.randomUUID());
    }

    public BorrowRecord(UUID id) {
        mId = id;
        mDateBorrowed = new Date();
        mStatus = "Borrowed";
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

    public String getBorrowerName() {
        return mBorrowerName;
    }

    public void setBorrowerName(String borrowerName) {
        mBorrowerName = borrowerName;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public void setQuantity(int quantity) {
        mQuantity = quantity;
    }

    public int getInitialQuantity() {
        return mInitialQuantity;
    }

    public void setInitialQuantity(int initialQuantity) {
        mInitialQuantity = initialQuantity;
    }

    public Date getDateBorrowed() {
        return mDateBorrowed;
    }

    public void setDateBorrowed(Date dateBorrowed) {
        mDateBorrowed = dateBorrowed;
    }

    public Date getExpectedReturnDate() {
        return mExpectedReturnDate;
    }

    public void setExpectedReturnDate(Date expectedReturnDate) {
        mExpectedReturnDate = expectedReturnDate;
    }

    public Date getActualReturnDate() {
        return mActualReturnDate;
    }

    public void setActualReturnDate(Date actualReturnDate) {
        mActualReturnDate = actualReturnDate;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public String getUnitId() {
        return mUnitId;
    }

    public void setUnitId(String unitId) {
        mUnitId = unitId;
    }
}
