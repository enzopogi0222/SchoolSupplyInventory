package com.example.schoolsupplyinventory.database;

public class SupplyDbSchema {
    public static final class SupplyTable {
        public static final String NAME = "supplies";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String DATE = "date";
            public static final String BORROWED = "borrowed";
            public static final String CATEGORY = "category";
            public static final String BRAND = "brand";
            public static final String BORROWER = "borrower";
        }
    }

    public static final class UserTable {
        public static final String NAME = "users";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String NAME = "name";
            public static final String BARCODE = "barcode";
        }
    }
}
