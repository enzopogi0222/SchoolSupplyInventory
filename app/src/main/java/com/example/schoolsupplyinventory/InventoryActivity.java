package com.example.schoolsupplyinventory;

import androidx.fragment.app.Fragment;

public class InventoryActivity extends SingleFragmentActivity {

    public static final String EXTRA_ITEM_NAME = "com.example.schoolsupplyinventory.item_name";
    public static final String EXTRA_IS_BORROWED = "com.example.schoolsupplyinventory.is_borrowed";

    @Override
    protected Fragment createFragment() {
        return new SupplyListFragment();
    }
}
