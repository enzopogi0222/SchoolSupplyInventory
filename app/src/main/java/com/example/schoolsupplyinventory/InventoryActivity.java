package com.example.schoolsupplyinventory;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

        SupplyLab.get(this).refreshFromPreferences();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        
        // Retrieve role
        String role = getSharedPreferences("SupplyFlow", Context.MODE_PRIVATE).getString("USER_ROLE", "ADMIN");
        boolean isAdmin = "ADMIN".equals(role);

        // Hide Reports tab if not admin
        if (!isAdmin) {
            Menu menu = bottomNav.getMenu();
            MenuItem reportsItem = menu.findItem(R.id.nav_reports);
            if (reportsItem != null) {
                reportsItem.setVisible(false);
            }
        }
        
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
