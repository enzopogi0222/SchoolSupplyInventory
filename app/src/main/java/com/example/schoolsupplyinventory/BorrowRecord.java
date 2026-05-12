package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class BorrowRecord {
    private final UUID mId;
    private UUID mItemId;
    private String mBorrowerName;
    private int mQuantity;
    private Date mDateBorrowed;
    private Date mExpectedReturnDate;
    private Date mActualReturnDate;
    private String mStatus;

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
}
