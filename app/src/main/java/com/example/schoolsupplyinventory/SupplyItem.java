package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class SupplyItem {

    private UUID mId;
    private String mName;
    private Date mDate;
    private boolean mBorrowed;
    private Category mCategory;
    private String mBrand;
    private String mBorrower;

    public SupplyItem() {
        this(UUID.randomUUID());
    }

    public SupplyItem(UUID id) {
        mId = id;
        mDate = new Date();
        mCategory = Category.OTHER;
    }

    public SupplyItem(String name, boolean isBorrowed) {
        this();
        mName = name;
        mBorrowed = isBorrowed;
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

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public boolean isBorrowed() {
        return mBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        mBorrowed = borrowed;
    }

    public Category getCategory() {
        return mCategory;
    }

    public void setCategory(Category category) {
        mCategory = category;
    }

    public String getBrand() {
        return mBrand;
    }

    public void setBrand(String brand) {
        mBrand = brand;
    }

    public String getBorrower() {
        return mBorrower;
    }

    public void setBorrower(String borrower) {
        mBorrower = borrower;
    }
}
