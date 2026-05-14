package com.example.schoolsupplyinventory;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
    private String mCurrentUser = "admin@supplyflow.com";
    private String mCurrentRole = "ADMIN";

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
        refreshFromPreferences();
    }

    public void refreshFromPreferences() {
        SharedPreferences prefs = mContext.getSharedPreferences("SupplyFlow", Context.MODE_PRIVATE);
        String email = prefs.getString("USER_EMAIL", "");
        if (!email.isEmpty()) {
            mCurrentUser = email;
        }
        String role = prefs.getString("USER_ROLE", "");
        if (!role.isEmpty()) {
            mCurrentRole = role;
        }
    }

    public String getCurrentUser() {
        return mCurrentUser;
    }

    public void setCurrentUser(String currentUser) {
        mCurrentUser = currentUser;
    }

    public String getCurrentRole() {
        return mCurrentRole;
    }

    public void setCurrentRole(String currentRole) {
        mCurrentRole = currentRole;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(mCurrentRole);
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
        updateSupply(item, null);
    }

    public void updateSupply(SupplyItem item, Callback<Void> callback) {
        mExecutor.execute(() -> {
            String uuidString = item.getId().toString();
            ContentValues values = getContentValues(item);
            mDatabase.update(SupplyTable.NAME, values,
                    SupplyTable.Cols.UUID + " = ?",
                    new String[]{uuidString});
            logHistory(item.getId(), item.getName(), "EDITED", "Item details updated");
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(null));
            }
        });
    }

    public void deleteSupply(SupplyItem s) {
        deleteSupply(s, null);
    }

    public void deleteSupply(SupplyItem s, Callback<Void> callback) {
        mExecutor.execute(() -> {
            String uuidString = s.getId().toString();
            mDatabase.delete(SupplyTable.NAME, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
            logHistory(s.getId(), s.getName(), "DELETED", "Item removed from inventory");
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(null));
            }
        });
    }

    /**
     * Applies a consumable deduction inside an active DB transaction.
     */
    private boolean applyConsumeInTransaction(SupplyItem item, int quantity) {
        if (item == null || !SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(item.getItemType()) || item.getAvailableQuantity() < quantity) {
            return false;
        }
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        item.setUsedQuantity(item.getUsedQuantity() + quantity);
        String uuidString = item.getId().toString();
        ContentValues values = getContentValues(item);
        mDatabase.update(SupplyTable.NAME, values, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
        logHistory(item.getId(), item.getName(), "USED", "Consumed " + quantity + " " + item.getUnit());
        return true;
    }

    /**
     * Applies a borrow inside an active DB transaction.
     */
    private boolean applyBorrowInTransaction(SupplyItem item, UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate, String unitId) {
        if (item == null || !SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(item.getItemType()) || item.getAvailableQuantity() < quantity) {
            return false;
        }
        item.setAvailableQuantity(item.getAvailableQuantity() - quantity);
        item.setBorrowedQuantity(item.getBorrowedQuantity() + quantity);
        item.setStatus("Borrowed");
        String uuidString = item.getId().toString();
        ContentValues supplyValues = getContentValues(item);
        mDatabase.update(SupplyTable.NAME, supplyValues, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});

        ContentValues borrowValues = new ContentValues();
        borrowValues.put(BorrowTable.Cols.UUID, UUID.randomUUID().toString());
        borrowValues.put(BorrowTable.Cols.ITEM_ID, itemId.toString());
        borrowValues.put(BorrowTable.Cols.BORROWER_NAME, borrowerName);
        borrowValues.put(BorrowTable.Cols.QUANTITY, quantity);
        borrowValues.put(BorrowTable.Cols.INITIAL_QUANTITY, quantity);
        borrowValues.put(BorrowTable.Cols.DATE_BORROWED, dateBorrowed);
        borrowValues.put(BorrowTable.Cols.EXPECTED_RETURN_DATE, expectedReturnDate);
        borrowValues.put(BorrowTable.Cols.STATUS, "Borrowed");
        borrowValues.put(BorrowTable.Cols.UNIT_ID, unitId != null ? unitId : "");
        mDatabase.insert(BorrowTable.NAME, null, borrowValues);

        logHistory(itemId, item.getName(), "BORROWED", "Borrowed " + quantity + " units (" + (unitId != null && !unitId.isEmpty() ? unitId : "Generic") + ") by " + borrowerName);
        return true;
    }

    public void useConsumableAsync(UUID itemId, int quantity, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            mDatabase.beginTransaction();
            try {
                SupplyItem item = getItem(itemId);
                if (!applyConsumeInTransaction(item, quantity)) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
        });
    }

    public void borrowItemAsync(UUID itemId, String borrowerName, int quantity, long dateBorrowed, long expectedReturnDate, String unitId, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            mDatabase.beginTransaction();
            try {
                SupplyItem item = getItem(itemId);
                if (!applyBorrowInTransaction(item, itemId, borrowerName, quantity, dateBorrowed, expectedReturnDate, unitId)) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
        });
    }

    public void returnItemAsync(BorrowRecord record, int returnQuantity, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            mDatabase.beginTransaction();
            try {
                SupplyItem item = getItem(record.getItemId());
                if (item != null) {
                    item.setAvailableQuantity(item.getAvailableQuantity() + returnQuantity);
                    item.setBorrowedQuantity(Math.max(0, item.getBorrowedQuantity() - returnQuantity));
                    if (item.getBorrowedQuantity() == 0) {
                        item.setStatus("Available");
                    }
                    String uuidString = item.getId().toString();
                    ContentValues supplyValues = getContentValues(item);
                    mDatabase.update(SupplyTable.NAME, supplyValues, SupplyTable.Cols.UUID + " = ?", new String[]{uuidString});
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
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
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
        values.put(SupplyTable.Cols.UNIT_IDENTIFIERS, item.getUnitIdentifiers());
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
        int unitIdIdx = cursor.getColumnIndex(BorrowTable.Cols.UNIT_ID);
        if (unitIdIdx != -1) {
            record.setUnitId(cursor.getString(unitIdIdx));
        }
        return record;
    }

    public List<SupplyRequest> getPendingRequests() {
        List<SupplyRequest> requests = new ArrayList<>();
        Cursor cursor = mDatabase.query(RequestTable.NAME, null,
                RequestTable.Cols.STATUS + " = ?", new String[]{SupplyRequest.STATUS_PENDING},
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

    public void getPendingRequestsAsync(Callback<List<SupplyRequest>> callback) {
        mExecutor.execute(() -> {
            List<SupplyRequest> list = getPendingRequests();
            mMainHandler.post(() -> callback.onComplete(list));
        });
    }

    public SupplyRequest getSupplyRequest(UUID id) {
        Cursor cursor = mDatabase.query(RequestTable.NAME, null,
                RequestTable.Cols.UUID + " = ?", new String[]{id.toString()},
                null, null, null);
        try {
            if (cursor.getCount() == 0) return null;
            cursor.moveToFirst();
            return getRequestFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    public List<SupplyRequest> getSupplyRequestsForRequester(String requesterEmail) {
        List<SupplyRequest> requests = new ArrayList<>();
        Cursor cursor = mDatabase.query(RequestTable.NAME, null,
                RequestTable.Cols.REQUESTER_NAME + " = ?", new String[]{requesterEmail},
                null, null, RequestTable.Cols.DATE_REQUESTED + " DESC");
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

    public void getSupplyRequestsForRequesterAsync(String requesterEmail, Callback<List<SupplyRequest>> callback) {
        mExecutor.execute(() -> {
            List<SupplyRequest> list = getSupplyRequestsForRequester(requesterEmail);
            mMainHandler.post(() -> callback.onComplete(list));
        });
    }

    public void submitSupplyRequestAsync(SupplyRequest request, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            SupplyItem item = getItem(request.getItemId());
            if (item == null || request.getQuantity() <= 0) {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }
            String purpose = request.getPurpose() != null ? request.getPurpose().trim() : "";
            if (purpose.isEmpty()) {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }
            if (SupplyRequest.TYPE_BORROW.equals(request.getRequestType())) {
                if (!SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(item.getItemType())) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
                if (request.getExpectedReturnDate() == null) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
            } else if (SupplyRequest.TYPE_CONSUME.equals(request.getRequestType())) {
                if (!SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(item.getItemType())) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
            } else {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }
            if (item.getAvailableQuantity() < request.getQuantity()) {
                mMainHandler.post(() -> callback.onComplete(false));
                return;
            }

            request.setStatus(SupplyRequest.STATUS_PENDING);
            request.setDateRequested(new Date());
            if (request.getItemTitle() == null || request.getItemTitle().isEmpty()) {
                request.setItemTitle(item.getName());
            }

            mDatabase.beginTransaction();
            try {
                mDatabase.insert(RequestTable.NAME, null, getRequestContentValues(request));
                logHistory(request.getItemId(), item.getName(), "REQUEST_SUBMITTED",
                        "Pending " + request.getRequestType() + " request: qty " + request.getQuantity() + ". " + purpose);
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
        });
    }

    public void approveSupplyRequestAsync(UUID requestId, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            mDatabase.beginTransaction();
            try {
                SupplyRequest req = getSupplyRequest(requestId);
                if (req == null || !SupplyRequest.STATUS_PENDING.equals(req.getStatus())) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
                SupplyItem item = getItem(req.getItemId());
                if (item == null || item.getAvailableQuantity() < req.getQuantity()) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }

                boolean applied;
                if (SupplyRequest.TYPE_BORROW.equals(req.getRequestType())) {
                    long due = req.getExpectedReturnDate() != null
                            ? req.getExpectedReturnDate().getTime()
                            : System.currentTimeMillis() + 604800000L;
                    applied = applyBorrowInTransaction(item, req.getItemId(), req.getRequesterName(),
                            req.getQuantity(), System.currentTimeMillis(), due, req.getUnitId());
                } else {
                    applied = applyConsumeInTransaction(item, req.getQuantity());
                }
                if (!applied) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }

                ContentValues cv = new ContentValues();
                cv.put(RequestTable.Cols.STATUS, SupplyRequest.STATUS_APPROVED);
                mDatabase.update(RequestTable.NAME, cv, RequestTable.Cols.UUID + " = ?",
                        new String[]{requestId.toString()});
                logHistory(req.getItemId(), item.getName(), "REQUEST_APPROVED",
                        "Approved " + req.getRequestType() + " for " + req.getRequesterName() + " (qty " + req.getQuantity() + ")");
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
        });
    }

    public void rejectSupplyRequestAsync(UUID requestId, Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            mDatabase.beginTransaction();
            try {
                SupplyRequest req = getSupplyRequest(requestId);
                if (req == null || !SupplyRequest.STATUS_PENDING.equals(req.getStatus())) {
                    mMainHandler.post(() -> callback.onComplete(false));
                    return;
                }
                SupplyItem item = getItem(req.getItemId());
                ContentValues cv = new ContentValues();
                cv.put(RequestTable.Cols.STATUS, SupplyRequest.STATUS_REJECTED);
                mDatabase.update(RequestTable.NAME, cv, RequestTable.Cols.UUID + " = ?",
                        new String[]{requestId.toString()});
                logHistory(req.getItemId(), item != null ? item.getName() : "Unknown", "REQUEST_REJECTED",
                        "Rejected " + req.getRequestType() + " for " + req.getRequesterName() + " (qty " + req.getQuantity() + ")");
                mDatabase.setTransactionSuccessful();
                mMainHandler.post(() -> callback.onComplete(true));
            } finally {
                mDatabase.endTransaction();
            }
        });
    }

    private ContentValues getRequestContentValues(SupplyRequest r) {
        ContentValues values = new ContentValues();
        values.put(RequestTable.Cols.UUID, r.getId().toString());
        values.put(RequestTable.Cols.ITEM_ID, r.getItemId().toString());
        values.put(RequestTable.Cols.ITEM_TITLE, r.getItemTitle() != null ? r.getItemTitle() : "");
        values.put(RequestTable.Cols.REQUESTER_NAME, r.getRequesterName());
        values.put(RequestTable.Cols.QUANTITY, r.getQuantity());
        values.put(RequestTable.Cols.DATE_REQUESTED, r.getDateRequested().getTime());
        values.put(RequestTable.Cols.STATUS, r.getStatus());
        values.put(RequestTable.Cols.REQUEST_TYPE, r.getRequestType());
        values.put(RequestTable.Cols.PURPOSE, r.getPurpose());
        long exp = r.getExpectedReturnDate() != null ? r.getExpectedReturnDate().getTime() : 0L;
        values.put(RequestTable.Cols.EXPECTED_RETURN_DATE, exp);
        values.put(RequestTable.Cols.UNIT_ID, r.getUnitId() != null ? r.getUnitId() : "");
        return values;
    }

    private SupplyRequest getRequestFromCursor(Cursor cursor) {
        SupplyRequest request = new SupplyRequest(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.UUID))));
        request.setItemId(UUID.fromString(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.ITEM_ID))));
        int titleIdx = cursor.getColumnIndex(RequestTable.Cols.ITEM_TITLE);
        if (titleIdx >= 0 && !cursor.isNull(titleIdx)) {
            request.setItemTitle(cursor.getString(titleIdx));
        }
        request.setRequesterName(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.REQUESTER_NAME)));
        request.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(RequestTable.Cols.QUANTITY)));
        request.setDateRequested(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(RequestTable.Cols.DATE_REQUESTED))));
        request.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(RequestTable.Cols.STATUS)));
        int typeIdx = cursor.getColumnIndex(RequestTable.Cols.REQUEST_TYPE);
        if (typeIdx >= 0 && !cursor.isNull(typeIdx)) {
            request.setRequestType(cursor.getString(typeIdx));
        } else {
            request.setRequestType(SupplyRequest.TYPE_CONSUME);
        }
        int purposeIdx = cursor.getColumnIndex(RequestTable.Cols.PURPOSE);
        if (purposeIdx >= 0 && !cursor.isNull(purposeIdx)) {
            request.setPurpose(cursor.getString(purposeIdx));
        }
        int expIdx = cursor.getColumnIndex(RequestTable.Cols.EXPECTED_RETURN_DATE);
        if (expIdx >= 0 && !cursor.isNull(expIdx)) {
            long exp = cursor.getLong(expIdx);
            if (exp > 0) {
                request.setExpectedReturnDate(new Date(exp));
            }
        }
        int unitIdx = cursor.getColumnIndex(RequestTable.Cols.UNIT_ID);
        if (unitIdx >= 0 && !cursor.isNull(unitIdx)) {
            String uid = cursor.getString(unitIdx);
            if (uid != null && !uid.isEmpty()) {
                request.setUnitId(uid);
            }
        }
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
                        String room = cursor.getString(0);
                        if (room != null) {
                            roomUsage.put(room, cursor.getInt(1));
                        }
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
