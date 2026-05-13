package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class SupplyItem {

    private UUID mId;
    private String mName;
    private String mBrand;
    private Date mDate; // Date Added
    private Date mExpirationDate;
    private boolean mBorrowed;
    private String mCategory;
    private String mSupplier;
    private String mBorrower;
    private String mPhotoFilename;
    private String mRoom; // Storage location (Building/Room)
    private int mQuantity;
    private String mUnit;
    private String mLocation; // Specific storage location (Shelf/Cabinet)
    private String mBarcode;
    private String mPropertyTag;
    private boolean mIsBorrowable;

    public SupplyItem() {
        this(UUID.randomUUID());
    }

    public SupplyItem(UUID id) {
        mId = id;
        mDate = new Date();
        mCategory = "STATIONERY";
        mRoom = "ITE OFFICE";
        mQuantity = 1;
        mUnit = "pcs";
        mLocation = "";
        mPropertyTag = "";
        mBarcode = "";
        mIsBorrowable = true;
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

    public String getBrand() {
        return mBrand;
    }

    public void setBrand(String brand) {
        mBrand = brand;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public Date getExpirationDate() {
        return mExpirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        mExpirationDate = expirationDate;
    }

    public boolean isBorrowed() {
        return mBorrowed;
    }

    public void setBorrowed(boolean borrowed) {
        mBorrowed = borrowed;
    }

    public boolean isBorrowable() {
        return mIsBorrowable;
    }

    public void setBorrowable(boolean borrowable) {
        mIsBorrowable = borrowable;
    }

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getSupplier() {
        return mSupplier;
    }

    public void setSupplier(String supplier) {
        mSupplier = supplier;
    }

    public String getBorrower() {
        return mBorrower;
    }

    public void setBorrower(String borrower) {
        mBorrower = borrower;
    }

    public String getPhotoFilename() {
        return "IMG_" + getId().toString() + ".jpg";
    }

    public String getRoom() {
        return mRoom;
    }

    public void setRoom(String room) {
        mRoom = room;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public void setQuantity(int quantity) {
        mQuantity = quantity;
    }

    public String getUnit() {
        return mUnit;
    }

    public void setUnit(String unit) {
        mUnit = unit;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }

    public String getBarcode() {
        return mBarcode;
    }

    public void setBarcode(String barcode) {
        mBarcode = barcode;
    }

    public String getPropertyTag() {
        return mPropertyTag;
    }

    public void setPropertyTag(String propertyTag) {
        mPropertyTag = propertyTag;
    }
}
