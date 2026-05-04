package com.example.schoolsupplyinventory.database;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.Log;

import com.example.schoolsupplyinventory.Category;
import com.example.schoolsupplyinventory.Room;
import com.example.schoolsupplyinventory.SupplyItem;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;

import java.util.Date;
import java.util.UUID;

public class SupplyCursorWrapper extends CursorWrapper {
    private static final String TAG = "SupplyCursorWrapper";

    public SupplyCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public SupplyItem getSupply() {
        String uuidString = getString(getColumnIndex(SupplyTable.Cols.UUID));
        String title = getString(getColumnIndex(SupplyTable.Cols.TITLE));
        long date = getLong(getColumnIndex(SupplyTable.Cols.DATE));
        int isBorrowed = getInt(getColumnIndex(SupplyTable.Cols.BORROWED));
        String categoryName = getString(getColumnIndex(SupplyTable.Cols.CATEGORY));
        String brand = getString(getColumnIndex(SupplyTable.Cols.BRAND));
        String borrower = getString(getColumnIndex(SupplyTable.Cols.BORROWER));
        String roomName = getString(getColumnIndex(SupplyTable.Cols.ROOM));

        SupplyItem item = new SupplyItem(UUID.fromString(uuidString));
        item.setName(title);
        item.setDate(new Date(date));
        item.setBorrowed(isBorrowed != 0);
        item.setBrand(brand);
        item.setBorrower(borrower);
        
        if (categoryName != null) {
            try {
                item.setCategory(Category.valueOf(categoryName));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unknown category: " + categoryName + ", defaulting to STATIONERY");
                item.setCategory(Category.STATIONERY);
            }
        }

        if (roomName != null) {
            try {
                item.setRoom(Room.valueOf(roomName));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Unknown room: " + roomName + ", defaulting to ITE_OFFICE");
                item.setRoom(Room.ITE_OFFICE);
            }
        }

        return item;
    }
}
