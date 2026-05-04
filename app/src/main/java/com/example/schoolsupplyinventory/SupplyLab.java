package com.example.schoolsupplyinventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.schoolsupplyinventory.database.SupplyBaseHelper;
import com.example.schoolsupplyinventory.database.SupplyCursorWrapper;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

import java.io.File;
import java.util.ArrayList;
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

    public List<SupplyItem> getSuppliesInRoom(String roomName) {
        List<SupplyItem> items = new ArrayList<>();
        SupplyCursorWrapper cursor = querySupplies(
                SupplyTable.Cols.ROOM + " = ?",
                new String[]{roomName}
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
            return cursor.getString(cursor.getColumnIndex(UserTable.Cols.NAME));
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
        values.put(SupplyTable.Cols.ROOM, item.getRoom());
        return values;
    }
}
