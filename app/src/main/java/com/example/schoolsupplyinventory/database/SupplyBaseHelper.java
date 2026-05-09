package com.example.schoolsupplyinventory.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.schoolsupplyinventory.database.SupplyDbSchema.BorrowTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;
import com.example.schoolsupplyinventory.database.SupplyDbSchema.UserTable;

public class SupplyBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 8;
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
                SupplyTable.Cols.LOCATION + ")"
        );

        db.execSQL("create table " + UserTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                UserTable.Cols.UUID + ", " +
                UserTable.Cols.NAME + ", " +
                UserTable.Cols.BARCODE + ")"
        );

        createBorrowTable(db);
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
    }
}
