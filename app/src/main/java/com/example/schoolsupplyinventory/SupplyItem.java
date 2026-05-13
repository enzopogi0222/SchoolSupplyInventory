package com.example.schoolsupplyinventory;

import java.util.Date;
import java.util.UUID;

public class SupplyItem {

    private UUID mId;
    private String mName;
    private Date mDate;
    private boolean mBorrowed;
    private String mCategory; // Changed to String for dynamic categories
    private String mBrand;
    private String mBorrower;
    private String mPhotoFilename;
    private Room mRoom;
    private int mQuantity;
    private String mLocation;

    public SupplyItem() {
        this(UUID.randomUUID());
    }

    public SupplyItem(UUID id) {
        mId = id;
        mDate = new Date();
        mCategory = "STATIONERY";
        mRoom = Room.ITE_OFFICE;
        mQuantity = 1;
        mLocation = "";
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

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
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

    public String getPhotoFilename() {
        return "IMG_" + getId().toString() + ".jpg";
    }

    public Room getRoom() {
        return mRoom;
    }

    public void setRoom(Room room) {
        mRoom = room;
    }

    public int getQuantity() {
        return mQuantity;
    }

    public void setQuantity(int quantity) {
        mQuantity = quantity;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
        mLocation = location;
    }
}
