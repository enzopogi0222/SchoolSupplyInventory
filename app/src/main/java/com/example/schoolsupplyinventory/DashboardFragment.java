package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView mTotalCountTextView;
    private TextView mAvailableCountTextView;
    private TextView mBorrowedCountTextView;
    private MaterialButton mViewInventoryButton;
    private MaterialButton mAddItemButton;
    private MaterialButton mBorrowButton;
    private MaterialButton mReturnButton;
    private MaterialButton mReportsButton;
    private MaterialButton mLogoutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mTotalCountTextView = v.findViewById(R.id.dashboard_total_count);
        mAvailableCountTextView = v.findViewById(R.id.dashboard_available_count);
        mBorrowedCountTextView = v.findViewById(R.id.dashboard_borrowed_count);
        
        mViewInventoryButton = v.findViewById(R.id.dashboard_view_inventory);
        mAddItemButton = v.findViewById(R.id.dashboard_add_item);
        mBorrowButton = v.findViewById(R.id.dashboard_borrow_item);
        mReturnButton = v.findViewById(R.id.dashboard_return_item);
        mReportsButton = v.findViewById(R.id.dashboard_reports);
        mLogoutButton = v.findViewById(R.id.dashboard_logout);

        updateStats();

        mViewInventoryButton.setOnClickListener(view -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SupplyListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        mAddItemButton.setOnClickListener(view -> {
            SupplyItem item = new SupplyItem();
            SupplyLab.get(getActivity()).addSupply(item);
            Intent intent = SupplyPagerActivity.newIntent(getActivity(), item.getId());
            startActivity(intent);
        });

        // Placeholders for requested buttons
        mBorrowButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Borrow functionality coming soon", Toast.LENGTH_SHORT).show());
        mReturnButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Return functionality coming soon", Toast.LENGTH_SHORT).show());
        mReportsButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Reports coming soon", Toast.LENGTH_SHORT).show());
        mLogoutButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Logging out...", Toast.LENGTH_SHORT).show());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }

    private void updateStats() {
        List<SupplyItem> items = SupplyLab.get(getActivity()).getItems();
        int total = items.size();
        int borrowed = 0;
        for (SupplyItem item : items) {
            if (item.isBorrowed()) {
                borrowed++;
            }
        }
        int available = total - borrowed;

        mTotalCountTextView.setText(String.valueOf(total));
        mAvailableCountTextView.setText(String.valueOf(available));
        mBorrowedCountTextView.setText(String.valueOf(borrowed));
    }
}
