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
        String itemType = getString(getColumnIndexOrThrow(SupplyTable.Cols.TYPE));
        String supplier = getString(getColumnIndexOrThrow(SupplyTable.Cols.SUPPLIER));
        String roomName = getString(getColumnIndexOrThrow(SupplyTable.Cols.ROOM));
        
        int totalQuantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.TOTAL_QUANTITY));
        int availableQuantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.AVAILABLE_QUANTITY));
        int borrowedQuantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.BORROWED_QUANTITY));
        int usedQuantity = getInt(getColumnIndexOrThrow(SupplyTable.Cols.USED_QUANTITY));
        
        String unit = getString(getColumnIndexOrThrow(SupplyTable.Cols.UNIT));
        String location = getString(getColumnIndexOrThrow(SupplyTable.Cols.LOCATION));
        String barcode = getString(getColumnIndexOrThrow(SupplyTable.Cols.BARCODE));
        String propertyTag = getString(getColumnIndexOrThrow(SupplyTable.Cols.PROPERTY_TAG));
        
        String description = getString(getColumnIndexOrThrow(SupplyTable.Cols.DESCRIPTION));
        String condition = getString(getColumnIndexOrThrow(SupplyTable.Cols.CONDITION));
        String status = getString(getColumnIndexOrThrow(SupplyTable.Cols.STATUS));
        String unitIdentifiers = getString(getColumnIndexOrThrow(SupplyTable.Cols.UNIT_IDENTIFIERS));

        double unitPrice = getDouble(getColumnIndexOrThrow(SupplyTable.Cols.UNIT_PRICE));
        int reorderLevel = getInt(getColumnIndexOrThrow(SupplyTable.Cols.REORDER_LEVEL));
        String remarks = getString(getColumnIndexOrThrow(SupplyTable.Cols.REMARKS));

        SupplyItem item = new SupplyItem(UUID.fromString(uuidString));
        item.setName(title);
        item.setBrand(brand);
        item.setDateAdded(new Date(date));
        if (expirationDate != 0) {
            item.setExpirationDate(new Date(expirationDate));
        }
        item.setCategory(categoryName != null ? categoryName : "OFFICE SUPPLIES");
        item.setItemType(itemType != null ? itemType : SupplyItem.TYPE_CONSUMABLE);
        item.setSupplier(supplier);
        item.setRoom(roomName != null ? roomName : "ITE OFFICE");
        
        item.setTotalQuantity(totalQuantity);
        item.setAvailableQuantity(availableQuantity);
        item.setBorrowedQuantity(borrowedQuantity);
        item.setUsedQuantity(usedQuantity);
        
        item.setUnit(unit != null ? unit : "Piece");
        item.setLocation(location);
        item.setBarcode(barcode);
        item.setPropertyTag(propertyTag != null ? propertyTag : "");
        
        item.setDescription(description != null ? description : "");
        item.setCondition(condition != null ? condition : "New");
        item.setStatus(status != null ? status : "Available");
        item.setUnitIdentifiers(unitIdentifiers != null ? unitIdentifiers : "");

        item.setUnitPrice(unitPrice);
        item.setReorderLevel(reorderLevel);
        item.setRemarks(remarks != null ? remarks : "");

        return item;
    }
}
