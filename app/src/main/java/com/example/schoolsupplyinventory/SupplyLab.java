package com.example.schoolsupplyinventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.schoolsupplyinventory.database.SupplyBaseHelper;
import com.example.schoolsupplyinventory.database.SupplyCursorWrapper;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.BorrowTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SupplyLab {
    private static SupplyLab sSupplyLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;

    public static SupplyLab get(Context context) {
        if (sSupplyLab == null) {
            sSupplyLab = new SupplyLab(context);
        }
        return sSupplyLab;
    }

    private SupplyLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new SupplyBaseHelper(mContext).getWritableDatabase();
    }

    public void addSupply(SupplyItem s) {
        ContentValues values = getContentValues(s);
        mDatabase.insert(SupplyTable.NAME, null, values);
    }

    public void deleteSupply(SupplyItem s) {
        String uuidString = s.getId().toString();
        mDatabase.delete(SupplyTable.NAME, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
        
        // Also delete the photo if it exists
        File photoFile = getPhotoFile(s);
        if (photoFile.exists()) {
            photoFile.delete();
        }
    }

    public List<SupplyItem> getItems() {
        List<SupplyItem> items = new ArrayList<>();
        SupplyCursorWrapper cursor = querySupplies(null, null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                items.add(cursor.getSupply());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        return items;
    }

    public SupplyItem getItem(UUID id) {
        SupplyCursorWrapper cursor = querySupplies(
                SupplyTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getSupply();
        } finally {
            cursor.close();
        }
    }

    public List<SupplyItem> getSuppliesInRoom(Room room) {
        List<SupplyItem> items = new ArrayList<>();
        SupplyCursorWrapper cursor = querySupplies(
                SupplyTable.Cols.ROOM + " = ?",
                new String[]{room.name()}
        );

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                items.add(cursor.getSupply());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        return items;
    }

    public void updateSupply(SupplyItem item) {
        String uuidString = item.getId().toString();
        ContentValues values = getContentValues(item);

        mDatabase.update(SupplyTable.NAME, values,
                SupplyTable.Cols.UUID + " = ?",
                new String[]{uuidString});
    }

    public boolean borrowItem(UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate) {
        SupplyItem item = getItem(itemId);
        if (item == null || item.getQuantity() < quantity) {
            return false;
        }

        // Reduce quantity
        item.setQuantity(item.getQuantity() - quantity);
        // If it's the last one or we want to mark it as borrowed
        item.setBorrowed(true);
        item.setBorrower(borrowerName);
        updateSupply(item);

        // Save record in Borrow Table
        ContentValues values = new ContentValues();
        values.put(BorrowTable.Cols.UUID, UUID.randomUUID().toString());
        values.put(BorrowTable.Cols.ITEM_ID, itemId.toString());
        values.put(BorrowTable.Cols.BORROWER_NAME, borrowerName);
        values.put(BorrowTable.Cols.QUANTITY, quantity);
        values.put(BorrowTable.Cols.DATE_BORROWED, dateBorrowed);
        values.put(BorrowTable.Cols.EXPECTED_RETURN_DATE, expectedReturnDate);
        values.put(BorrowTable.Cols.STATUS, "Borrowed");

        mDatabase.insert(BorrowTable.NAME, null, values);
        return true;
    }

    public boolean returnItem(BorrowRecord record, int returnQuantity) {
        SupplyItem item = getItem(record.getItemId());
        if (item == null) return false;

        // Increase quantity
        item.setQuantity(item.getQuantity() + returnQuantity);
        
        // Update borrow record
        ContentValues values = new ContentValues();
        int remainingQuantity = record.getQuantity() - returnQuantity;
        
        if (remainingQuantity <= 0) {
            values.put(BorrowTable.Cols.STATUS, "Returned");
            values.put(BorrowTable.Cols.ACTUAL_RETURN_DATE, System.currentTimeMillis());
            values.put(BorrowTable.Cols.QUANTITY, 0);
        } else {
            values.put(BorrowTable.Cols.QUANTITY, remainingQuantity);
        }

        mDatabase.update(BorrowTable.NAME, values,
                BorrowTable.Cols.UUID + " = ?",
                new String[]{record.getId().toString()});

        // Check if item is still borrowed by anyone
        List<BorrowRecord> activeBorrows = getActiveBorrowRecordsForItem(item.getId());
        if (activeBorrows.isEmpty()) {
            item.setBorrowed(false);
            item.setBorrower(null);
        } else {
            // Update primary borrower to someone still holding it
            item.setBorrower(activeBorrows.get(0).getBorrowerName());
        }
        
        updateSupply(item);
        return true;
    }

    public List<BorrowRecord> getActiveBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        Cursor cursor = mDatabase.query(
                BorrowTable.NAME,
                null,
                BorrowTable.Cols.STATUS + " = ?",
                new String[]{"Borrowed"},
                null, null, null
        );

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                records.add(getBorrowRecordFromCursor(cursor));
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return records;
    }

    private List<BorrowRecord> getActiveBorrowRecordsForItem(UUID itemId) {
        List<BorrowRecord> records = new ArrayList<>();
        Cursor cursor = mDatabase.query(
                BorrowTable.NAME,
                null,
                BorrowTable.Cols.ITEM_ID + " = ? AND " + BorrowTable.Cols.STATUS + " = ?",
                new String[]{itemId.toString(), "Borrowed"},
                null, null, null
        );

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                records.add(getBorrowRecordFromCursor(cursor));
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return records;
    }

    private BorrowRecord getBorrowRecordFromCursor(Cursor cursor) {
        String uuidStr = cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.UUID));
        String itemIdStr = cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.ITEM_ID));
        String borrower = cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.BORROWER_NAME));
        int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(BorrowTable.Cols.QUANTITY));
        long dateBorrowed = cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.DATE_BORROWED));
        long expectedReturn = cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.EXPECTED_RETURN_DATE));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.STATUS));

        BorrowRecord record = new BorrowRecord(UUID.fromString(uuidStr));
        record.setItemId(UUID.fromString(itemIdStr));
        record.setBorrowerName(borrower);
        record.setQuantity(quantity);
        record.setDateBorrowed(new Date(dateBorrowed));
        record.setExpectedReturnDate(new Date(expectedReturn));
        record.setStatus(status);
        return record;
    }

    public String findNameByBarcode(String barcode) {
        Cursor cursor = mDatabase.query(
                UserTable.NAME,
                null,
                UserTable.Cols.BARCODE + " = ?",
                new String[] { barcode },
                null, null, null
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndexOrThrow(UserTable.Cols.NAME));
        } finally {
            cursor.close();
        }
    }

    public boolean isUserAlreadyBorrowing(String borrowerName) {
        if (borrowerName == null) return false;
        
        Cursor cursor = mDatabase.query(
                SupplyTable.NAME,
                null,
                SupplyTable.Cols.BORROWER + " = ? AND " + SupplyTable.Cols.BORROWED + " = 1",
                new String[] { borrowerName },
                null, null, null
        );
        
        try {
            return cursor.getCount() > 0;
        } finally {
            cursor.close();
        }
    }

    public File getPhotoFile(SupplyItem item) {
        File filesDir = mContext.getFilesDir();
        File photoDir = new File(filesDir, "images");
        if (!photoDir.exists()) {
            photoDir.mkdirs();
        }
        return new File(photoDir, item.getPhotoFilename());
    }

    private SupplyCursorWrapper querySupplies(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                SupplyTable.NAME,
                null, // columns - null selects all columns
                whereClause,
                whereArgs,
                null, // groupBy
                null, // having
                null  // orderBy
        );

        return new SupplyCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(SupplyItem item) {
        ContentValues values = new ContentValues();
        values.put(SupplyTable.Cols.UUID, item.getId().toString());
        values.put(SupplyTable.Cols.TITLE, item.getName());
        values.put(SupplyTable.Cols.DATE, item.getDate().getTime());
        values.put(SupplyTable.Cols.BORROWED, item.isBorrowed() ? 1 : 0);
        values.put(SupplyTable.Cols.CATEGORY, item.getCategory() != null ? item.getCategory().name() : null);
        values.put(SupplyTable.Cols.BRAND, item.getBrand());
        values.put(SupplyTable.Cols.BORROWER, item.getBorrower());
        values.put(SupplyTable.Cols.ROOM, item.getRoom() != null ? item.getRoom().name() : null);
        values.put(SupplyTable.Cols.QUANTITY, item.getQuantity());
        values.put(SupplyTable.Cols.LOCATION, item.getLocation());
        return values;
    }
}
