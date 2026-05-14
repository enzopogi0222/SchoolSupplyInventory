package com.example.schoolsupplyinventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SupplyItem {

    public static final String TYPE_CONSUMABLE = "Consumable";
    public static final String TYPE_BORROWABLE = "Borrowable";

    private UUID mId;
    private String mName;
    private String mCategory;
    private String mItemType; // Consumable or Borrowable
    private int mTotalQuantity;
    private int mAvailableQuantity;
    private int mBorrowedQuantity;
    private int mUsedQuantity;
    private String mUnit;
    private String mDescription;
    private String mCondition; // New, Good, Damaged, Old
    private String mStatus; // Available, Borrowed, Used, Out of Stock
    private Date mDateAdded;
    private String mUnitIdentifiers; // Comma-separated unique IDs for each piece
    
    // Additional fields from previous versions that might still be useful
    private String mBrand;
    private String mBarcode;
    private String mPropertyTag;
    private String mRoom;
    private String mLocation;
    private String mSupplier;
    private Date mExpirationDate;

    public SupplyItem() {
        this(UUID.randomUUID());
    }

    public SupplyItem(UUID id) {
        mId = id;
        mDateAdded = new Date();
        mItemType = TYPE_CONSUMABLE;
        mCategory = "OFFICE SUPPLIES";
        mTotalQuantity = 0;
        mAvailableQuantity = 0;
        mBorrowedQuantity = 0;
        mUsedQuantity = 0;
        mUnit = "Piece";
        mCondition = "New";
        mStatus = "Available";
        mDescription = "";
        mUnitIdentifiers = "";
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

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getItemType() {
        return mItemType;
    }

    public void setItemType(String itemType) {
        mItemType = itemType;
    }

    public int getTotalQuantity() {
        return mTotalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        mTotalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return mAvailableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        mAvailableQuantity = availableQuantity;
    }

    public int getBorrowedQuantity() {
        return mBorrowedQuantity;
    }

    public void setBorrowedQuantity(int borrowedQuantity) {
        mBorrowedQuantity = borrowedQuantity;
    }

    public int getUsedQuantity() {
        return mUsedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        mUsedQuantity = usedQuantity;
    }

    public String getUnit() {
        return mUnit;
    }

    public void setUnit(String unit) {
        mUnit = unit;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getCondition() {
        return mCondition;
    }

    public void setCondition(String condition) {
        mCondition = condition;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        mStatus = status;
    }

    public Date getDateAdded() {
        return mDateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        mDateAdded = dateAdded;
    }

    public String getUnitIdentifiers() {
        return mUnitIdentifiers;
    }

    public void setUnitIdentifiers(String unitIdentifiers) {
        mUnitIdentifiers = unitIdentifiers;
    }

    public List<String> getUnitIdentifiersList() {
        if (mUnitIdentifiers == null || mUnitIdentifiers.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(mUnitIdentifiers.split("\\s*,\\s*")));
    }

    // Legacy/Compatibility Getters and Setters
    public int getQuantity() {
        return mAvailableQuantity;
    }

    public String getBrand() { return mBrand; }
    public void setBrand(String brand) { mBrand = brand; }
    public String getBarcode() { return mBarcode; }
    public void setBarcode(String barcode) { mBarcode = barcode; }
    public String getPropertyTag() { return mPropertyTag; }
    public void setPropertyTag(String propertyTag) { mPropertyTag = propertyTag; }
    public String getRoom() { return mRoom; }
    public void setRoom(String room) { mRoom = room; }
    public String mLocation() { return mLocation; }
    public void setLocation(String location) { mLocation = location; }
    public String getSupplier() { return mSupplier; }
    public void setSupplier(String supplier) { mSupplier = supplier; }
    public Date getExpirationDate() { return mExpirationDate; }
    public void setExpirationDate(Date expirationDate) { mExpirationDate = expirationDate; }
    
    public String getPhotoFilename() {
        return "IMG_" + getId().toString() + ".jpg";
    }

    public boolean isBorrowable() {
        return TYPE_BORROWABLE.equals(mItemType);
    }
}
