package com.example.schoolsupplyinventory.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.example.schoolsupplyinventory.Category;
import com.example.schoolsupplyinventory.SupplyItem;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;

import java.util.Date;
import java.util.UUID;

public class SupplyCursorWrapper extends CursorWrapper {
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

        SupplyItem item = new SupplyItem(UUID.fromString(uuidString));
        item.setName(title);
        item.setDate(new Date(date));
        item.setBorrowed(isBorrowed != 0);
        item.setBrand(brand);
        
        if (categoryName != null) {
            item.setCategory(Category.valueOf(categoryName));
        }

        return item;
    }
}
