package com.example.schoolsupplyinventory;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SupplyLab {
    private static SupplyLab sSupplyLab;
    private List<SupplyItem> mItems;

    public static SupplyLab get(Context context) {
        if (sSupplyLab == null) {
            sSupplyLab = new SupplyLab(context);
        }
        return sSupplyLab;
    }

    private SupplyLab(Context context) {
        mItems = new ArrayList<>();
        // Logic: Generating dummy data for testing
        for (int i = 0; i < 20; i++) {
            SupplyItem item = new SupplyItem("Supply Item #" + i, i % 2 == 0);
            mItems.add(item);
        }
    }

    public void addSupply(SupplyItem s) {
        mItems.add(s);
    }

    public List<SupplyItem> getItems() { return mItems; }

    public SupplyItem getItem(UUID id) {
        for (SupplyItem item : mItems) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }
}
