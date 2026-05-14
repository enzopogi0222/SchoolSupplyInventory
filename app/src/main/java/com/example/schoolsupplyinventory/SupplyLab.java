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
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RequestTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RoomTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UnitTable;
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
    private String mCurrentUser = "admin@school.com"; // Single role: Admin

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

    public String getCurrentUser() {
        return mCurrentUser;
    }

    public void setCurrentUser(String currentUser) {
        mCurrentUser = currentUser;
    }

    public void addSupply(SupplyItem s, Callback<Void> callback) {
        mExecutor.execute(() -> {
            ContentValues values = getContentValues(s);
            mDatabase.insert(SupplyTable.NAME, null, values);
            logHistory(s.getId(), s.getName(), "ADDED", "Item added to inventory. Initial quantity: " + s.getTotalQuantity());
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(null));
            }
        });
    }

    public void updateSupply(SupplyItem item) {
        mExecutor.execute(() -> {
            String uuidString = item.getId().toString();
            ContentValues values = getContentValues(item);
            mDatabase.update(SupplyTable.NAME, values,
                    SupplyTable.Cols.UUID + " = ?",
                    new String[]{uuidString});
            logHistory(item.getId(), item.getName(), "EDITED", "Item details updated");
        });
    }

    public void deleteSupply(SupplyItem s) {
        mExecutor.execute(() -> {
            String uuidString = s.getId().toString();
            mDatabase.delete(SupplyTable.NAME, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
            logHistory(s.getId(), s.getName(), "DELETED", "Item removed from inventory");
        });
    }

    public void useConsumableAsync(UUID itemId, int quantity, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(itemId);
            if (item == null || !SupplyItem.TYPE_CONSUMABLE.equals(item.getItemType()) || item.getAvailableQuantity() < quantity) {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }

            item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
            item.setUsedQuantity(item.getUsedQuantity() + quantity);
            
            updateSupply(item);
            logHistory(item.getId(), item.getName(), "USED", "Consumed " + quantity + " " + item.getUnit());
            mMainHandler.post(() -> callback.onComplete(true));
        });
    }

    public void borrowItemAsync(UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(itemId);
            if (item == null || !SupplyItem.TYPE_BORROWABLE.equals(item.getItemType()) || item.getAvailableQuantity() < quantity) {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }

            item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
            item.setBorrowedQuantity(item.getBorrowedQuantity() + quantity);
            item.setStatus("Borrowed");
            
            updateSupply(item);

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

            logHistory(itemId, item.getName(), "BORROWED", "Borrowed " + quantity + " units by " + borrowerName);
            mMainHandler.post(() -> callback.onComplete(true));
        });
    }

    public void returnItemAsync(BorrowRecord record, int returnQuantity, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(record.getItemId());
            if (item != null) {
                item.setAvailableQuantity(item.getAvailableQuantity() + returnQuantity);
                item.setBorrowedQuantity(Math.max(0, item.getBorrowedQuantity() - returnQuantity));
                if (item.getBorrowedQuantity() == 0) {
                    item.setStatus("Available");
                }
                updateSupply(item);
            }

            ContentValues values = new ContentValues();
            int remainingInRecord = record.getQuantity() - returnQuantity;
            if (remainingInRecord <= 0) {
                values.put(BorrowTable.Cols.STATUS, "Returned");
                values.put(BorrowTable.Cols.ACTUAL_RETURN_DATE, System.currentTimeMillis());
                values.put(BorrowTable.Cols.QUANTITY, 0);
            } else {
                values.put(BorrowTable.Cols.QUANTITY, remainingInRecord);
            }
            mDatabase.update(BorrowTable.NAME, values, BorrowTable.Cols.UUID + " = ?", new String[]{record.getId().toString()});

            logHistory(record.getItemId(), item != null ? item.getName() : "Unknown", "RETURNED", "Returned " + returnQuantity + " units");
            mMainHandler.post(() -> callback.onComplete(true));
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

    public void getItemsAsync(Callback<List<SupplyItem>> callback) {
        mExecutor.execute(() -> {
            List<SupplyItem> items = getItems();
            mMainHandler.post(() -> callback.onComplete(items));
        });
    }

    public SupplyItem getItem(UUID id) {
        SupplyCursorWrapper cursor = querySupplies(SupplyTable.Cols.UUID + " = ?", new String[]{id.toString()});
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return cursor.getSupply();
        } finally {
            cursor.close();
        }
    }

    public void getItemAsync(UUID id, Callback<SupplyItem> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(id);
            mMainHandler.post(() -> callback.onComplete(item));
        });
    }

    private SupplyCursorWrapper querySupplies(String where, String[] args) {
        Cursor cursor = mDatabase.query(SupplyTable.NAME, null, where, args, null, null, null);
        return new SupplyCursorWrapper(cursor);
    }

    private ContentValues getContentValues(SupplyItem item) {
        ContentValues values = new ContentValues();
        values.put(SupplyTable.Cols.UUID, item.getId().toString());
        values.put(SupplyTable.Cols.TITLE, item.getName());
        values.put(SupplyTable.Cols.CATEGORY, item.getCategory());
        values.put(SupplyTable.Cols.TYPE, item.getItemType());
        values.put(SupplyTable.Cols.TOTAL_QUANTITY, item.getTotalQuantity());
        values.put(SupplyTable.Cols.AVAILABLE_QUANTITY, item.getAvailableQuantity());
        values.put(SupplyTable.Cols.BORROWED_QUANTITY, item.getBorrowedQuantity());
        values.put(SupplyTable.Cols.USED_QUANTITY, item.getUsedQuantity());
        values.put(SupplyTable.Cols.UNIT, item.getUnit());
        values.put(SupplyTable.Cols.DESCRIPTION, item.getDescription());
        values.put(SupplyTable.Cols.CONDITION, item.getCondition());
        values.put(SupplyTable.Cols.STATUS, item.getStatus());
        values.put(SupplyTable.Cols.DATE, item.getDateAdded().getTime());
        values.put(SupplyTable.Cols.BRAND, item.getBrand());
        values.put(SupplyTable.Cols.BARCODE, item.getBarcode());
        values.put(SupplyTable.Cols.ROOM, item.getRoom());
        values.put(SupplyTable.Cols.PROPERTY_TAG, item.getPropertyTag());
        return values;
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

    public List<BorrowRecord> getAllBorrowRecords() {
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
        return records;
    }

    public void getAllBorrowRecordsAsync(Callback<List<BorrowRecord>> callback) {
        mExecutor.execute(() -> {
            List<BorrowRecord> records = getAllBorrowRecords();
            mMainHandler.post(() -> callback.onComplete(records));
        });
    }

    public List<BorrowRecord> getActiveBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        Cursor cursor = mDatabase.query(BorrowTable.NAME, null,
                BorrowTable.Cols.STATUS + " = ?", new String[]{"Borrowed"},
                null, null, BorrowTable.Cols.DATE_BORROWED + " ASC");
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

    public void getActiveBorrowRecordsAsync(Callback<List<BorrowRecord>> callback) {
        mExecutor.execute(() -> {
            List<BorrowRecord> records = getActiveBorrowRecords();
            mMainHandler.post(() -> callback.onComplete(records));
        });
    }

    private BorrowRecord getBorrowRecordFromCursor(Cursor cursor) {
        BorrowRecord record = new BorrowRecord(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.UUID))));
        record.setItemId(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.ITEM_ID))));
        record.setBorrowerName(cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.BORROWER_NAME)));
        record.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(BorrowTable.Cols.QUANTITY)));
        record.setInitialQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(BorrowTable.Cols.INITIAL_QUANTITY)));
        record.setDateBorrowed(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.DATE_BORROWED))));
        record.setExpectedReturnDate(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(BorrowTable.Cols.EXPECTED_RETURN_DATE))));
        record.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(BorrowTable.Cols.STATUS)));
        return record;
    }

    public List<SupplyRequest> getPendingRequests() {
        List<SupplyRequest> requests = new ArrayList<>();
        Cursor cursor = mDatabase.query(RequestTable.NAME, null,
                RequestTable.Cols.STATUS + " = ?", new String[]{"PENDING"},
                null, null, RequestTable.Cols.DATE_REQUESTED + " ASC");
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                requests.add(getRequestFromCursor(cursor));
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
        return requests;
    }

    private SupplyRequest getRequestFromCursor(Cursor cursor) {
        SupplyRequest request = new SupplyRequest(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.UUID))));
        request.setItemId(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.ITEM_ID))));
        request.setRequesterName(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.REQUESTER_NAME)));
        request.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(RequestTable.Cols.QUANTITY)));
        request.setDateRequested(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(RequestTable.Cols.DATE_REQUESTED))));
        request.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.STATUS)));
        return request;
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

    public void getUnitsAsync(Callback<List<String>> callback) {
        mExecutor.execute(() -> {
            List<String> units = new ArrayList<>();
            Cursor cursor = mDatabase.query(UnitTable.NAME, null, null, null, null, null, UnitTable.Cols.NAME + " ASC");
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    units.add(cursor.getString(cursor.getColumnIndexOrThrow(UnitTable.Cols.NAME)));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            mMainHandler.post(() -> callback.onComplete(units));
        });
    }

    public void addUnitAsync(String unit, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(UnitTable.Cols.NAME, unit.toUpperCase());
            long result = mDatabase.insertWithOnConflict(UnitTable.NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
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

    public void getUsageByRoomAsync(Callback<Map<String, Integer>> callback) {
        mExecutor.execute(() -> {
            Map<String, Integer> roomUsage = new HashMap<>();
            String query = "SELECT " + SupplyTable.Cols.ROOM + ", SUM(" + SupplyTable.Cols.TOTAL_QUANTITY + " - " + SupplyTable.Cols.AVAILABLE_QUANTITY + ") " +
                           "FROM " + SupplyTable.NAME + " GROUP BY " + SupplyTable.Cols.ROOM;
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

    public File getPhotoFile(SupplyItem item) {
        if (item == null) return null;
        File filesDir = mContext.getFilesDir();
        File photoDir = new File(filesDir, "images");
        if (!photoDir.exists()) photoDir.mkdirs();
        return new File(photoDir, item.getPhotoFilename());
    }
}
