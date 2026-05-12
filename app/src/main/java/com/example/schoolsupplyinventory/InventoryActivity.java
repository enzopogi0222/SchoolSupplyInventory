package com.example.schoolsupplyinventory;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class InventoryActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_NAME = "com.example.schoolsupplyinventory.item_name";
    public static final String EXTRA_IS_BORROWED = "com.example.schoolsupplyinventory.is_borrowed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (itemId == R.id.nav_inventory) {
                fragment = new SupplyListFragment();
            } else if (itemId == R.id.nav_reports) {
                fragment = new ReportsFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}
