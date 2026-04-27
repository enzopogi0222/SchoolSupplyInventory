package com.example.schoolsupplyinventory;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class InventoryLab {
    private static InventoryLab sInventoryLab;

    private List<SupplyItem> mItems;

    public static InventoryLab get(Context context) {
        if (sInventoryLab == null) {
            sInventoryLab = new InventoryLab(context);
        }
        return sInventoryLab;
    }

    private InventoryLab(Context context) {
        mItems = new ArrayList<>();
    }
}
