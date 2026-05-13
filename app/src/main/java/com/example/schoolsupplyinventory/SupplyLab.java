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
import com.example.schoolsupplyinventory.database.SupplyDbSchema.HistoryTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RoomTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupplyLab {
    private static SupplyLab sSupplyLab;
    private Context mContext;
    private SQLiteDatabase mDatabase;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private String mCurrentUser = "admin@supplyflow.com"; // Default for demo

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

    public void setCurrentUser(String email) {
        mCurrentUser = email;
    }

    public void addSupply(SupplyItem s) {
        addSupply(s, null);
    }

    public void addSupply(SupplyItem s, Callback<Void> callback) {
        mExecutor.execute(() -> {
            ContentValues values = getContentValues(s);
            mDatabase.insert(SupplyTable.NAME, null, values);
            logHistory(s.getId(), s.getName(), "ADDED", "Item added to inventory. Initial quantity: " + s.getQuantity());
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(null));
            }
        });
    }

    public void deleteSupply(SupplyItem s) {
        mExecutor.execute(() -> {
            String uuidString = s.getId().toString();
            mDatabase.delete(SupplyTable.NAME, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
            logHistory(s.getId(), s.getName(), "DELETED", "Item removed from inventory");
            
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
            SupplyItem oldItem = getItem(item.getId());
            String details = "";
            if (oldItem != null) {
                if (oldItem.getQuantity() != item.getQuantity()) {
                    details = "Quantity changed from " + oldItem.getQuantity() + " to " + item.getQuantity();
                } else if (!oldItem.getName().equals(item.getName())) {
                    details = "Name changed to " + item.getName();
                } else {
                    details = "Item details updated";
                }
            }

            String uuidString = item.getId().toString();
            ContentValues values = getContentValues(item);
            mDatabase.update(SupplyTable.NAME, values,
                    SupplyTable.Cols.UUID + " = ?",
                    new String[]{uuidString});
            
            logHistory(item.getId(), item.getName(), "EDITED", details);
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

        int oldQty = item.getQuantity();
        item.setQuantity(oldQty - quantity);
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

        logHistory(itemId, item.getName(), "BORROWED", "Borrowed " + quantity + " by " + borrowerName + ". Stock: " + oldQty + " -> " + item.getQuantity());

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
        int oldQty = 0;
        if (item != null) {
            oldQty = item.getQuantity();
            item.setQuantity(oldQty + returnQuantity);
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
            
            logHistory(item.getId(), item.getName(), "RETURNED", "Returned " + returnQuantity + " from " + record.getBorrowerName() + ". Stock: " + oldQty + " -> " + item.getQuantity());
        }
        
        return true;
    }

    private void logHistory(UUID itemId, String itemName, String action, String details) {
        ContentValues values = new ContentValues();
        values.put(HistoryTable.Cols.UUID, UUID.randomUUID().toString());
        values.put(HistoryTable.Cols.ITEM_ID, itemId.toString());
        values.put(HistoryTable.Cols.ITEM_NAME, itemName);
        values.put(HistoryTable.Cols.ACTION, action);
        values.put(HistoryTable.Cols.USER, mCurrentUser);
        values.put(HistoryTable.Cols.TIMESTAMP, System.currentTimeMillis());
        values.put(HistoryTable.Cols.DETAILS, details);
        mDatabase.insert(HistoryTable.NAME, null, values);
    }

    public void getHistoryForItemAsync(UUID itemId, Callback<List<HistoryRecord>> callback) {
        mExecutor.execute(() -> {
            List<HistoryRecord> records = new ArrayList<>();
            Cursor cursor = mDatabase.query(HistoryTable.NAME, null, HistoryTable.Cols.ITEM_ID + " = ?", new String[]{itemId.toString()}, null, null, HistoryTable.Cols.TIMESTAMP + " DESC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    records.add(getHistoryRecordFromCursor(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(records));
        });
    }

    public void getAllHistoryAsync(Callback<List<HistoryRecord>> callback) {
        mExecutor.execute(() -> {
            List<HistoryRecord> records = new ArrayList<>();
            Cursor cursor = mDatabase.query(HistoryTable.NAME, null, null, null, null, null, HistoryTable.Cols.TIMESTAMP + " DESC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    records.add(getHistoryRecordFromCursor(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(records));
        });
    }

    private HistoryRecord getHistoryRecordFromCursor(Cursor cursor) {
        HistoryRecord record = new HistoryRecord(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.UUID))));
        record.setItemId(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.ITEM_ID))));
        record.setItemName(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.ITEM_NAME)));
        record.setAction(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.ACTION)));
        record.setUser(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.USER)));
        record.setTimestamp(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(HistoryTable.Cols.TIMESTAMP))));
        record.setDetails(cursor.getString(cursor.getColumnIndexOrThrow(HistoryTable.Cols.DETAILS)));
        return record;
    }

    public void getTopBorrowedItemsAsync(int limit, Callback<List<Map.Entry<String, Integer>>> callback) {
        mExecutor.execute(() -> {
            Map<String, Integer> usage = new HashMap<>();
            String query = "SELECT s." + SupplyTable.Cols.TITLE + ", SUM(b." + BorrowTable.Cols.INITIAL_QUANTITY + ") as total " +
                           "FROM " + BorrowTable.NAME + " b " +
                           "JOIN " + SupplyTable.NAME + " s ON b." + BorrowTable.Cols.ITEM_ID + " = s." + SupplyTable.Cols.UUID + " " +
                           "GROUP BY b." + BorrowTable.Cols.ITEM_ID + " " +
                           "ORDER BY total DESC LIMIT " + limit;
            Cursor cursor = mDatabase.rawQuery(query, null);
            try {
                if (cursor.moveToFirst()) {
                    do {
                        usage.put(cursor.getString(0), cursor.getInt(1));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            List<Map.Entry<String, Integer>> result = new ArrayList<>(usage.entrySet());
            result.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            mMainHandler.post(() -> callback.onComplete(result));
        });
    }

    public void getUsageByRoomAsync(Callback<Map<String, Integer>> callback) {
        mExecutor.execute(() -> {
            Map<String, Integer> roomUsage = new HashMap<>();
            String query = "SELECT s." + SupplyTable.Cols.ROOM + ", SUM(b." + BorrowTable.Cols.INITIAL_QUANTITY + ") " +
                           "FROM " + BorrowTable.NAME + " b " +
                           "JOIN " + SupplyTable.NAME + " s ON b." + BorrowTable.Cols.ITEM_ID + " = s." + SupplyTable.Cols.UUID + " " +
                           "GROUP BY s." + SupplyTable.Cols.ROOM;
            Cursor cursor = mDatabase.rawQuery(query, null);
            try {
                if (cursor.moveToFirst()) {
                    do {
                        roomUsage.put(cursor.getString(0), cursor.getInt(1));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(roomUsage));
        });
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
