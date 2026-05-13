package com.example.schoolsupplyinventory.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.schoolsupplyinventory.database.SupplyDbSchema.BorrowTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.CategoryTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.HistoryTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RequestTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.RoomTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

public class SupplyBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 15;
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
                SupplyTable.Cols.BRAND + ", " +
                SupplyTable.Cols.DATE + ", " +
                SupplyTable.Cols.EXPIRATION_DATE + ", " +
                SupplyTable.Cols.BORROWED + ", " +
                SupplyTable.Cols.CATEGORY + ", " +
                SupplyTable.Cols.SUPPLIER + ", " +
                SupplyTable.Cols.BORROWER + ", " +
                SupplyTable.Cols.ROOM + ", " +
                SupplyTable.Cols.QUANTITY + ", " +
                SupplyTable.Cols.UNIT + ", " +
                SupplyTable.Cols.LOCATION + ", " +
                SupplyTable.Cols.BARCODE + ", " +
                SupplyTable.Cols.PROPERTY_TAG + ", " +
                SupplyTable.Cols.IS_BORROWABLE + ")"
        );

        db.execSQL("create table " + UserTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                UserTable.Cols.UUID + ", " +
                UserTable.Cols.NAME + ", " +
                UserTable.Cols.BARCODE + ", " +
                UserTable.Cols.EMAIL + ", " +
                UserTable.Cols.ROLE + ")"
        );

        createBorrowTable(db);
        createCategoryTable(db);
        createRoomTable(db);
        createHistoryTable(db);
        createRequestTable(db);
    }

    private void createBorrowTable(SQLiteDatabase db) {
        db.execSQL("create table " + BorrowTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                BorrowTable.Cols.UUID + ", " +
                BorrowTable.Cols.ITEM_ID + ", " +
                BorrowTable.Cols.BORROWER_NAME + ", " +
                BorrowTable.Cols.QUANTITY + ", " +
                BorrowTable.Cols.INITIAL_QUANTITY + ", " +
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
        // Initial categories based on user requirements
        String[] initials = {
            "BOND PAPER", "PENS & MARKERS", "CLEANING MATERIALS", 
            "LABORATORY EQUIPMENT", "SPORTS EQUIPMENT", "OFFICE SUPPLIES", 
            "ELECTRONICS", "STATIONERY", "BOOKS", "FURNITURE", "APPLIANCES"
        };
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

    private void createHistoryTable(SQLiteDatabase db) {
        db.execSQL("create table " + HistoryTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                HistoryTable.Cols.UUID + ", " +
                HistoryTable.Cols.ITEM_ID + ", " +
                HistoryTable.Cols.ITEM_NAME + ", " +
                HistoryTable.Cols.ACTION + ", " +
                HistoryTable.Cols.USER + ", " +
                HistoryTable.Cols.TIMESTAMP + ", " +
                HistoryTable.Cols.DETAILS + ")"
        );
    }

    private void createRequestTable(SQLiteDatabase db) {
        db.execSQL("create table " + RequestTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                RequestTable.Cols.UUID + ", " +
                RequestTable.Cols.ITEM_ID + ", " +
                RequestTable.Cols.REQUESTER_NAME + ", " +
                RequestTable.Cols.QUANTITY + ", " +
                RequestTable.Cols.DATE_REQUESTED + ", " +
                RequestTable.Cols.STATUS + ")"
        );
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
        if (oldVersion < 12) {
            db.execSQL("alter table " + BorrowTable.NAME + " add column " + BorrowTable.Cols.INITIAL_QUANTITY + " integer");
            db.execSQL("update " + BorrowTable.NAME + " set " + BorrowTable.Cols.INITIAL_QUANTITY + " = " + BorrowTable.Cols.QUANTITY);
        }
        if (oldVersion < 13) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.EXPIRATION_DATE + " integer");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.SUPPLIER + " text");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.UNIT + " text default 'pcs'");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.BARCODE + " text");
            db.execSQL("alter table " + UserTable.NAME + " add column " + UserTable.Cols.EMAIL + " text");
        }
        if (oldVersion < 14) {
            createHistoryTable(db);
        }
        if (oldVersion < 15) {
            db.execSQL("alter table " + UserTable.NAME + " add column " + UserTable.Cols.ROLE + " text default 'STAFF'");
            createRequestTable(db);
        }
    }
}
