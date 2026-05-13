package com.example.schoolsupplyinventory.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.example.schoolsupplyinventory.SupplyItem;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;

import java.util.Date;
import java.util.UUID;

public class SupplyCursorWrapper extends CursorWrapper {

    public SupplyCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public SupplyItem getSupply() {
        String uuidString = getString(getColumnIndexOrThrow(SupplyTable.Cols.UUID));
        String title = getString(getColumnIndexOrThrow(SupplyTable.Cols.TITLE));
        long date = getLong(getColumnIndexOrThrow(SupplyTable.Cols.DATE));
        int isBorrowed = getInt(getColumnIndexOrThrow(SupplyTable.Cols.BORROWED));
        String categoryName = getString(getColumnIndexOrThrow(SupplyTable.Cols.CATEGORY));
        String brand = getString(getColumnIndexOrThrow(SupplyTable.Cols.BRAND));
        String borrower = getString(getColumnIndexOrThrow(SupplyTable.Cols.BORROWER));
        String roomName = getString(getColumnIndexOrThrow(SupplyTable.Cols.ROOM));
        int quantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.QUANTITY));
        String location = getString(getColumnIndexOrThrow(SupplyTable.Cols.LOCATION));
        String propertyTag = getString(getColumnIndexOrThrow(SupplyTable.Cols.PROPERTY_TAG));
        int isBorrowable = getInt(getColumnIndexOrThrow(SupplyTable.Cols.IS_BORROWABLE));

        SupplyItem item = new SupplyItem(UUID.fromString(uuidString));
        item.setName(title);
        item.setDate(new Date(date));
        item.setBorrowed(isBorrowed != 0);
        item.setBrand(brand);
        item.setBorrower(borrower);
        item.setQuantity(quantity);
        item.setLocation(location);
        item.setCategory(categoryName != null ? categoryName : "OTHER");
        item.setRoom(roomName != null ? roomName : "ITE OFFICE");
        item.setPropertyTag(propertyTag != null ? propertyTag : "");
        item.setBorrowable(isBorrowable != 0);

        return item;
    }
}
