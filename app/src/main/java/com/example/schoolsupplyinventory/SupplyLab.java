package com.example.schoolsupplyinventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.example.schoolsupplyinventory.database.SupplyBaseHelper;
import com.example.schoolsupplyinventory.database.SupplyCursorWrapper;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.BorrowTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.CategoryTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RoomTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupplyLab {
    private static SupplyLab sSupplyLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onComplete(T result);
    }

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
        addSupply(s, null);
    }

    public void addSupply(SupplyItem s, Callback<Void> callback) {
        mExecutor.execute(() -> {
            ContentValues values = getContentValues(s);
            mDatabase.insert(SupplyTable.NAME, null, values);
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(null));
            }
        });
    }

    public void deleteSupply(SupplyItem s) {
        mExecutor.execute(() -> {
            String uuidString = s.getId().toString();
            mDatabase.delete(SupplyTable.NAME, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
            
            File photoFile = getPhotoFile(s);
            if (photoFile != null && photoFile.exists()) {
                photoFile.delete();
            }
        });
    }

    public void getItemsAsync(Callback<List<SupplyItem>> callback) {
        mExecutor.execute(() -> {
            List<SupplyItem> items = getItems();
            mMainHandler.post(() -> callback.onComplete(items));
        });
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

    public void getItemAsync(UUID id, Callback<SupplyItem> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(id);
            mMainHandler.post(() -> callback.onComplete(item));
        });
    }

    public SupplyItem getItem(UUID id) {
        SupplyCursorWrapper cursor = querySupplies(
                SupplyTable.Cols.UUID + " = ?",
                new String[]{id.toString()}
        );
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getSupply();
        } finally {
            cursor.close();
        }
    }

    public void updateSupply(SupplyItem item) {
        mExecutor.execute(() -> {
            String uuidString = item.getId().toString();
            ContentValues values = getContentValues(item);
            mDatabase.update(SupplyTable.NAME, values,
                    SupplyTable.Cols.UUID + " = ?",
                    new String[]{uuidString});
        });
    }

    public void borrowItemAsync(UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            boolean success = borrowItem(itemId, borrowerName, quantity, dateBorrowed, expectedReturnDate);
            mMainHandler.post(() -> callback.onComplete(success));
        });
    }

    private boolean borrowItem(UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate) {
        SupplyItem item = getItem(itemId);
        if (item == null || item.getQuantity() < quantity) return false;

        item.setQuantity(item.getQuantity() - quantity);
        item.setBorrowed(true);
        item.setBorrower(borrowerName);
        
        ContentValues itemValues = getContentValues(item);
        mDatabase.update(SupplyTable.NAME, itemValues, SupplyTable.Cols.UUID + " = ?", new String[]{itemId.toString()});

        ContentValues values = new ContentValues();
        values.put(BorrowTable.Cols.UUID, UUID.randomUUID().toString());
        values.put(BorrowTable.Cols.ITEM_ID, itemId.toString());
        values.put(BorrowTable.Cols.BORROWER_NAME, borrowerName);
        values.put(BorrowTable.Cols.QUANTITY, quantity);
        values.put(BorrowTable.Cols.INITIAL_QUANTITY, quantity);
        values.put(BorrowTable.Cols.DATE_BORROWED, dateBorrowed);
        values.put(BorrowTable.Cols.EXPECTED_RETURN_DATE, expectedReturnDate);
        values.put(BorrowTable.Cols.STATUS, "Borrowed");

        mDatabase.insert(BorrowTable.NAME, null, values);
        return true;
    }

    public void returnItemAsync(BorrowRecord record, int returnQuantity, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            boolean success = returnItem(record, returnQuantity);
            mMainHandler.post(() -> callback.onComplete(success));
        });
    }

    private boolean returnItem(BorrowRecord record, int returnQuantity) {
        SupplyItem item = getItem(record.getItemId());
        
        if (item != null) {
            item.setQuantity(item.getQuantity() + returnQuantity);
        }
        
        ContentValues values = new ContentValues();
        int remainingQuantity = record.getQuantity() - returnQuantity;
        if (remainingQuantity <= 0) {
            values.put(BorrowTable.Cols.STATUS, "Returned");
            values.put(BorrowTable.Cols.ACTUAL_RETURN_DATE, System.currentTimeMillis());
            values.put(BorrowTable.Cols.QUANTITY, 0);
        } else {
            values.put(BorrowTable.Cols.QUANTITY, remainingQuantity);
        }

        mDatabase.update(BorrowTable.NAME, values, BorrowTable.Cols.UUID + " = ?", new String[]{record.getId().toString()});

        if (item != null) {
            List<BorrowRecord> activeBorrows = getActiveBorrowRecordsForItem(item.getId());
            if (activeBorrows.isEmpty()) {
                item.setBorrowed(false);
                item.setBorrower(null);
            } else {
                item.setBorrower(activeBorrows.get(0).getBorrowerName());
            }
            
            ContentValues itemValues = getContentValues(item);
            mDatabase.update(SupplyTable.NAME, itemValues, SupplyTable.Cols.UUID + " = ?", new String[]{item.getId().toString()});
        }
        
        return true;
    }

    public void updateBorrowRecordAsync(BorrowRecord record, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(BorrowTable.Cols.EXPECTED_RETURN_DATE, record.getExpectedReturnDate().getTime());
            values.put(BorrowTable.Cols.BORROWER_NAME, record.getBorrowerName());
            values.put(BorrowTable.Cols.QUANTITY, record.getQuantity());
            
            int rows = mDatabase.update(BorrowTable.NAME, values, 
                    BorrowTable.Cols.UUID + " = ?", 
                    new String[]{record.getId().toString()});
            
            mMainHandler.post(() -> callback.onComplete(rows > 0));
        });
    }

    public void getActiveBorrowRecordsAsync(Callback<List<BorrowRecord>> callback) {
        mExecutor.execute(() -> {
            List<BorrowRecord> result = getActiveBorrowRecords();
            mMainHandler.post(() -> callback.onComplete(result));
        });
    }

    public List<BorrowRecord> getActiveBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        Cursor cursor = mDatabase.query(BorrowTable.NAME, null, BorrowTable.Cols.STATUS + " = ?", new String[]{"Borrowed"}, null, null, null);
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

    public void getAllBorrowRecordsAsync(Callback<List<BorrowRecord>> callback) {
        mExecutor.execute(() -> {
            List<BorrowRecord> records = new ArrayList<>();
            Cursor cursor = mDatabase.query(BorrowTable.NAME, null, null, null, null, null, BorrowTable.Cols.DATE_BORROWED + " ASC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    records.add(getBorrowRecordFromCursor(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(records));
        });
    }

    public void getReturnedCountAsync(Callback<Integer> callback) {
        mExecutor.execute(() -> {
            int count = 0;
            Cursor cursor = mDatabase.query(BorrowTable.NAME, new String[]{"SUM(" + BorrowTable.Cols.INITIAL_QUANTITY + " - " + BorrowTable.Cols.QUANTITY + ")"},
                BorrowTable.Cols.STATUS + " = 'Returned' OR " + BorrowTable.Cols.QUANTITY + " < " + BorrowTable.Cols.INITIAL_QUANTITY, null, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            } finally {
                cursor.close();
            }
            final int result = count;
            mMainHandler.post(() -> callback.onComplete(result));
        });
    }

    public void getCategoriesAsync(Callback<List<String>> callback) {
        mExecutor.execute(() -> {
            List<String> categories = new ArrayList<>();
            Cursor cursor = mDatabase.query(CategoryTable.NAME, null, null, null, null, null, CategoryTable.Cols.NAME + " ASC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    categories.add(cursor.getString(cursor.getColumnIndexOrThrow(CategoryTable.Cols.NAME)));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(categories));
        });
    }

    public void addCategoryAsync(String category, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(CategoryTable.Cols.NAME, category.toUpperCase());
            long result = mDatabase.insertWithOnConflict(CategoryTable.NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            mMainHandler.post(() -> callback.onComplete(result != -1));
        });
    }

    public void getRoomsAsync(Callback<List<String>> callback) {
        mExecutor.execute(() -> {
            List<String> rooms = new ArrayList<>();
            Cursor cursor = mDatabase.query(RoomTable.NAME, null, null, null, null, null, RoomTable.Cols.NAME + " ASC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    rooms.add(cursor.getString(cursor.getColumnIndexOrThrow(RoomTable.Cols.NAME)));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(rooms));
        });
    }

    public void addRoomAsync(String room, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(RoomTable.Cols.NAME, room.toUpperCase());
            long result = mDatabase.insertWithOnConflict(RoomTable.NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            mMainHandler.post(() -> callback.onComplete(result != -1));
        });
    }

    private List<BorrowRecord> getActiveBorrowRecordsForItem(UUID itemId) {
        List<BorrowRecord> records = new ArrayList<>();
        Cursor cursor = mDatabase.query(BorrowTable.NAME, null, BorrowTable.Cols.ITEM_ID + " = ? AND " + BorrowTable.Cols.STATUS + " = ?", new String[]{itemId.toString(), "Borrowed"}, null, null, null);
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
        int initialQuantity = cursor.getInt(cursor.getColumnIndexOrThrow(BorrowTable.Cols.INITIAL_QUANTITY));
        long dateBorrowed = cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.DATE_BORROWED));
        long expectedReturn = cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.EXPECTED_RETURN_DATE));
        String status = cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.STATUS));

        BorrowRecord record = new BorrowRecord(UUID.fromString(uuidStr));
        record.setItemId(UUID.fromString(itemIdStr));
        record.setBorrowerName(borrower);
        record.setQuantity(quantity);
        record.setInitialQuantity(initialQuantity);
        record.setDateBorrowed(new Date(dateBorrowed));
        record.setExpectedReturnDate(new Date(expectedReturn));
        record.setStatus(status);
        return record;
    }

    public File getPhotoFile(SupplyItem item) {
        if (item == null) return null;
        File filesDir = mContext.getFilesDir();
        File photoDir = new File(filesDir, "images");
        if (!photoDir.exists()) photoDir.mkdirs();
        return new File(photoDir, item.getPhotoFilename());
    }

    private SupplyCursorWrapper querySupplies(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(SupplyTable.NAME, null, whereClause, whereArgs, null, null, null);
        return new SupplyCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(SupplyItem item) {
        ContentValues values = new ContentValues();
        values.put(SupplyTable.Cols.UUID, item.getId().toString());
        values.put(SupplyTable.Cols.TITLE, item.getName());
        values.put(SupplyTable.Cols.BRAND, item.getBrand());
        values.put(SupplyTable.Cols.DATE, item.getDate().getTime());
        if (item.getExpirationDate() != null) {
            values.put(SupplyTable.Cols.EXPIRATION_DATE, item.getExpirationDate().getTime());
        }
        values.put(SupplyTable.Cols.BORROWED, item.isBorrowed() ? 1 : 0);
        values.put(SupplyTable.Cols.CATEGORY, item.getCategory());
        values.put(SupplyTable.Cols.SUPPLIER, item.getSupplier());
        values.put(SupplyTable.Cols.BORROWER, item.getBorrower());
        values.put(SupplyTable.Cols.ROOM, item.getRoom());
        values.put(SupplyTable.Cols.QUANTITY, item.getQuantity());
        values.put(SupplyTable.Cols.UNIT, item.getUnit());
        values.put(SupplyTable.Cols.LOCATION, item.getLocation());
        values.put(SupplyTable.Cols.BARCODE, item.getBarcode());
        values.put(SupplyTable.Cols.PROPERTY_TAG, item.getPropertyTag());
        values.put(SupplyTable.Cols.IS_BORROWABLE, item.isBorrowable() ? 1 : 0);
        return values;
    }

    public String findNameByBarcode(String barcode) {
        Cursor cursor = mDatabase.query(UserTable.NAME, null, UserTable.Cols.BARCODE + " = ?", new String[] { barcode }, null, null, null);
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getString(cursor.getColumnIndexOrThrow(UserTable.Cols.NAME));
        } finally {
            cursor.close();
        }
    }
}
