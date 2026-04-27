package com.example.schoolsupplyinventory.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.schoolsupplyinventory.database.SupplyDbSchema.SupplyTable;

public class SupplyBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 3;
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
                SupplyTable.Cols.BRAND + ")"
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
    }
}
