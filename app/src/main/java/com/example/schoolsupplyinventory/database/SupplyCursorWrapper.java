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
        String brand = getString(getColumnIndexOrThrow(SupplyTable.Cols.BRAND));
        long date = getLong(getColumnIndexOrThrow(SupplyTable.Cols.DATE));
        long expirationDate = getLong(getColumnIndexOrThrow(SupplyTable.Cols.EXPIRATION_DATE));
        String categoryName = getString(getColumnIndexOrThrow(SupplyTable.Cols.CATEGORY));
        String supplier = getString(getColumnIndexOrThrow(SupplyTable.Cols.SUPPLIER));
        String borrower = getString(getColumnIndexOrThrow(SupplyTable.Cols.BORROWER));
        String roomName = getString(getColumnIndexOrThrow(SupplyTable.Cols.ROOM));
        int quantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.QUANTITY));
        String unit = getString(getColumnIndexOrThrow(SupplyTable.Cols.UNIT));
        String location = getString(getColumnIndexOrThrow(SupplyTable.Cols.LOCATION));
        String barcode = getString(getColumnIndexOrThrow(SupplyTable.Cols.BARCODE));
        String propertyTag = getString(getColumnIndexOrThrow(SupplyTable.Cols.PROPERTY_TAG));
        int isBorrowable = getInt(getColumnIndexOrThrow(SupplyTable.Cols.IS_BORROWABLE));
        
        String description = getString(getColumnIndexOrThrow(SupplyTable.Cols.DESCRIPTION));
        String condition = getString(getColumnIndexOrThrow(SupplyTable.Cols.CONDITION));
        String status = getString(getColumnIndexOrThrow(SupplyTable.Cols.STATUS));

        SupplyItem item = new SupplyItem(UUID.fromString(uuidString));
        item.setName(title);
        item.setBrand(brand);
        item.setDate(new Date(date));
        if (expirationDate != 0) {
            item.setExpirationDate(new Date(expirationDate));
        }
        item.setSupplier(supplier);
        item.setBorrower(borrower);
        item.setQuantity(quantity);
        item.setUnit(unit != null ? unit : "Piece");
        item.setLocation(location);
        item.setBarcode(barcode);
        item.setCategory(categoryName != null ? categoryName : "OFFICE SUPPLIES");
        item.setRoom(roomName != null ? roomName : "ITE OFFICE");
        item.setPropertyTag(propertyTag != null ? propertyTag : "");
        item.setBorrowable(isBorrowable != 0);
        
        item.setDescription(description != null ? description : "");
        item.setCondition(condition != null ? condition : "New");
        item.setStatus(status != null ? status : "Available");

        return item;
    }
}
