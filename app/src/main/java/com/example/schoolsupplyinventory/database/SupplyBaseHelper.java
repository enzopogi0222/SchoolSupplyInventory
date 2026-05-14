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
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UnitTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

public class SupplyBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 21;
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
                SupplyTable.Cols.CATEGORY + ", " +
                SupplyTable.Cols.TYPE + ", " +
                SupplyTable.Cols.SUPPLIER + ", " +
                SupplyTable.Cols.ROOM + ", " +
                SupplyTable.Cols.TOTAL_QUANTITY + " integer, " +
                SupplyTable.Cols.AVAILABLE_QUANTITY + " integer, " +
                SupplyTable.Cols.BORROWED_QUANTITY + " integer, " +
                SupplyTable.Cols.USED_QUANTITY + " integer, " +
                SupplyTable.Cols.UNIT + ", " +
                SupplyTable.Cols.LOCATION + ", " +
                SupplyTable.Cols.BARCODE + ", " +
                SupplyTable.Cols.PROPERTY_TAG + ", " +
                SupplyTable.Cols.IS_BORROWABLE + ", " +
                SupplyTable.Cols.DESCRIPTION + ", " +
                SupplyTable.Cols.CONDITION + ", " +
                SupplyTable.Cols.STATUS + ", " +
                SupplyTable.Cols.UNIT_IDENTIFIERS + ")"
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
        createUnitTable(db);
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
                BorrowTable.Cols.STATUS + ", " +
                BorrowTable.Cols.UNIT_ID + ")"
        );
    }

    private void createCategoryTable(SQLiteDatabase db) {
        db.execSQL("create table " + CategoryTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                CategoryTable.Cols.NAME + " UNIQUE)"
        );
        String[] initials = {
            "OFFICE SUPPLIES", "ICT EQUIPMENT", "CLEANING MATERIALS",
            "SPORTS EQUIPMENT", "LABORATORY TOOLS", "FURNITURE AND FIXTURES",
            "STATIONERY", "BOOKS", "APPLIANCES"
        };
        for (String cat : initials) {
            ContentValues values = new ContentValues();
            values.put(CategoryTable.Cols.NAME, cat);
            db.insert(CategoryTable.NAME, null, values);
        }
    }

    private void createUnitTable(SQLiteDatabase db) {
        db.execSQL("create table " + UnitTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                UnitTable.Cols.NAME + " UNIQUE)"
        );
        String[] initials = {"PIECE", "BOX", "SET", "PACK", "UNIT", "REAM"};
        for (String unit : initials) {
            ContentValues values = new ContentValues();
            values.put(UnitTable.Cols.NAME, unit);
            db.insert(UnitTable.NAME, null, values);
        }
    }

    private void createRoomTable(SQLiteDatabase db) {
        db.execSQL("create table " + RoomTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                RoomTable.Cols.NAME + " UNIQUE)"
        );
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
                RequestTable.Cols.ITEM_TITLE + ", " +
                RequestTable.Cols.REQUESTER_NAME + ", " +
                RequestTable.Cols.QUANTITY + ", " +
                RequestTable.Cols.DATE_REQUESTED + ", " +
                RequestTable.Cols.STATUS + ", " +
                RequestTable.Cols.REQUEST_TYPE + ", " +
                RequestTable.Cols.PURPOSE + ", " +
                RequestTable.Cols.EXPECTED_RETURN_DATE + " integer default 0, " +
                RequestTable.Cols.UNIT_ID + ", " +
                RequestTable.Cols.BORROW_RECORD_ID + ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 16) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.DESCRIPTION + " text");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.CONDITION + " text default 'New'");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.STATUS + " text default 'Available'");
        }
        if (oldVersion < 17) {
            createUnitTable(db);
        }
        if (oldVersion < 18) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.TYPE + " text default 'Consumable'");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.TOTAL_QUANTITY + " integer");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.AVAILABLE_QUANTITY + " integer");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.BORROWED_QUANTITY + " integer default 0");
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.USED_QUANTITY + " integer default 0");
        }
        if (oldVersion < 19) {
            db.execSQL("alter table " + SupplyTable.NAME + " add column " + SupplyTable.Cols.UNIT_IDENTIFIERS + " text");
            db.execSQL("alter table " + BorrowTable.NAME + " add column " + BorrowTable.Cols.UNIT_ID + " text");
        }
        if (oldVersion < 20) {
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.ITEM_TITLE + " text");
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.REQUEST_TYPE + " text default 'CONSUME'");
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.PURPOSE + " text");
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.EXPECTED_RETURN_DATE + " integer default 0");
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.UNIT_ID + " text");
        }
        if (oldVersion < 21) {
            db.execSQL("alter table " + RequestTable.NAME + " add column " + RequestTable.Cols.BORROW_RECORD_ID + " text");
        }
    }
}
