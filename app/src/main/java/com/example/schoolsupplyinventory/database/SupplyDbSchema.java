package com.example.schoolsupplyinventory.database;

public class SupplyDbSchema {
    public static final class SupplyTable {
        public static final String NAME = "supplies";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String BRAND = "brand";
            public static final String DATE = "date";
            public static final String EXPIRATION_DATE = "expiration_date";
            public static final String BORROWED = "borrowed";
            public static final String CATEGORY = "category";
            public static final String SUPPLIER = "supplier";
            public static final String BORROWER = "borrower";
            public static final String ROOM = "room";
            public static final String QUANTITY = "quantity";
            public static final String UNIT = "unit";
            public static final String LOCATION = "location";
            public static final String BARCODE = "barcode";
            public static final String PROPERTY_TAG = "property_tag";
            public static final String IS_BORROWABLE = "is_borrowable";
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

    public static final class BorrowTable {
        public static final String NAME = "borrow_records";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String ITEM_ID = "item_id";
            public static final String BORROWER_NAME = "borrower_name";
            public static final String QUANTITY = "quantity";
            public static final String INITIAL_QUANTITY = "initial_quantity";
            public static final String DATE_BORROWED = "date_borrowed";
            public static final String EXPECTED_RETURN_DATE = "expected_return_date";
            public static final String ACTUAL_RETURN_DATE = "actual_return_date";
            public static final String STATUS = "status"; // "Borrowed" or "Returned"
        }
    }

    public static final class CategoryTable {
        public static final String NAME = "categories";
        public static final class Cols {
            public static final String NAME = "name";
        }
    }
    
    public static final class RoomTable { // Added for dynamic classrooms
        public static final String NAME = "rooms";
        public static final class Cols {
            public static final String NAME = "name";
        }
    }
}
