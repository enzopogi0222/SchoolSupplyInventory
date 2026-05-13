package com.example.schoolsupplyinventory.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.schoolsupplyinventory.database.SupplyDbSchema.BorrowTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.CategoryTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RoomTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

public class SupplyBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 11;
    private static final String DATABASE_NAME = "supplyBase.db";

    public SupplyBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + SupplyTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                SupplyTable.Cols.UUID + ", " +
                SupplyTable.Cols.TITLE + ", " +
                SupplyTable.Cols.DATE + ", " +
                SupplyTable.Cols.BORROWED + ", " +
                SupplyTable.Cols.CATEGORY + ", " +
                SupplyTable.Cols.BRAND + ", " +
                SupplyTable.Cols.BORROWER + ", " +
                SupplyTable.Cols.ROOM + ", " +
                SupplyTable.Cols.QUANTITY + ", " +
                SupplyTable.Cols.LOCATION + ", " +
                SupplyTable.Cols.PROPERTY_TAG + ", " +
                SupplyTable.Cols.IS_BORROWABLE + ")"
        );

        db.execSQL("create table " + UserTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                UserTable.Cols.UUID + ", " +
                UserTable.Cols.NAME + ", " +
                UserTable.Cols.BARCODE + ")"
        );

        createBorrowTable(db);
        createCategoryTable(db);
        createRoomTable(db);
    }

    private void createBorrowTable(SQLiteDatabase db) {
        db.execSQL("create table " + BorrowTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                BorrowTable.Cols.UUID + ", " +
                BorrowTable.Cols.ITEM_ID + ", " +
                BorrowTable.Cols.BORROWER_NAME + ", " +
                BorrowTable.Cols.QUANTITY + ", " +
                BorrowTable.Cols.DATE_BORROWED + ", " +
                BorrowTable.Cols.EXPECTED_RETURN_DATE + ", " +
                BorrowTable.Cols.ACTUAL_RETURN_DATE + ", " +
                BorrowTable.Cols.STATUS + ")"
        );
    }

    private void createCategoryTable(SQLiteDatabase db) {
        db.execSQL("create table " + CategoryTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                CategoryTable.Cols.NAME + " UNIQUE)"
        );
        // Initial categories
        String[] initials = {"STATIONERY", "ELECTRONICS", "BOOKS", "FURNITURE", "APPLIANCES"};
        for (String cat : initials) {
            ContentValues values = new ContentValues();
            values.put(CategoryTable.Cols.NAME, cat);
            db.insert(CategoryTable.NAME, null, values);
        }
    }

    private void createRoomTable(SQLiteDatabase db) {
        db.execSQL("create table " + RoomTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                RoomTable.Cols.NAME + " UNIQUE)"
        );
        // Initial rooms
        String[] initials = {"ITE OFFICE", "COMLAB-A", "COMLAB-B", "COMLAB-C", "CLASSROOM 101", "CLASSROOM 102"};
        for (String room : initials) {
            ContentValues values = new ContentValues();
            values.put(RoomTable.Cols.NAME, room);
            db.insert(RoomTable.NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.CATEGORY);
        }
        if (oldVersion < 3) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.BRAND);
        }
        if (oldVersion < 4) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.BORROWER);
        }
        if (oldVersion < 5) {
            db.execSQL("create table " + UserTable.NAME + "(" +
                    " _id integer primary key autoincrement, " +
                    UserTable.Cols.UUID + ", " +
                    UserTable.Cols.NAME + ", " +
                    UserTable.Cols.BARCODE + ")"
            );
        }
        if (oldVersion < 6) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.ROOM);
        }
        if (oldVersion < 7) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.QUANTITY);
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.LOCATION);
        }
        if (oldVersion < 8) {
            createBorrowTable(db);
        }
        if (oldVersion < 9) {
            createCategoryTable(db);
        }
        if (oldVersion < 10) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.PROPERTY_TAG);
            createRoomTable(db);
        }
        if (oldVersion < 11) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.IS_BORROWABLE + " integer default 1");
        }
    }
}
