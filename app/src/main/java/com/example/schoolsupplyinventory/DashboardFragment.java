package com.example.schoolsupplyinventory;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    private Calendar mExpectedReturnDate = Calendar.getInstance();

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

        mBorrowButton.setOnClickListener(v1 -> showItemSelectionDialog());
        
        mReturnButton.setOnClickListener(v1 -> showReturnItemDialog());
        
        mReportsButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Reports coming soon", Toast.LENGTH_SHORT).show());
        mLogoutButton.setOnClickListener(v1 -> Toast.makeText(getActivity(), "Logging out...", Toast.LENGTH_SHORT).show());

        return v;
    }

    private void showItemSelectionDialog() {
        List<SupplyItem> allItems = SupplyLab.get(getActivity()).getItems();
        List<SupplyItem> availableItems = allItems.stream()
                .filter(item -> item.getQuantity() > 0)
                .collect(Collectors.toList());

        if (availableItems.isEmpty()) {
            Toast.makeText(getActivity(), "No items available to borrow", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] itemNames = availableItems.stream()
                .map(item -> (item.getName() != null && !item.getName().isEmpty() ? item.getName() : "Unnamed Item") 
                        + " (Stock: " + item.getQuantity() + ")")
                .toArray(String[]::new);

        new AlertDialog.Builder(getActivity())
                .setTitle("Select Item to Borrow")
                .setItems(itemNames, (dialog, which) -> {
                    showBorrowDetailsDialog(availableItems.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBorrowDetailsDialog(SupplyItem item) {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_borrow_item, null);
        
        TextView itemNameTextView = v.findViewById(R.id.borrow_item_name);
        TextInputEditText borrowerNameField = v.findViewById(R.id.borrow_borrower_name);
        TextInputEditText quantityField = v.findViewById(R.id.borrow_quantity);
        TextInputLayout quantityLayout = v.findViewById(R.id.borrow_quantity_layout);
        MaterialButton dateButton = v.findViewById(R.id.borrow_expected_return_date);

        itemNameTextView.setText("Borrowing: " + (item.getName() != null ? item.getName() : "Unnamed Item"));
        quantityLayout.setHelperText("Available: " + item.getQuantity());

        // Default expected return date: 7 days from now
        mExpectedReturnDate = Calendar.getInstance();
        mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 7);
        updateDateButtonText(dateButton);

        dateButton.setOnClickListener(v1 -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
                    (view, year, month, dayOfMonth) -> {
                        mExpectedReturnDate.set(Calendar.YEAR, year);
                        mExpectedReturnDate.set(Calendar.MONTH, month);
                        mExpectedReturnDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButtonText(dateButton);
                    },
                    mExpectedReturnDate.get(Calendar.YEAR),
                    mExpectedReturnDate.get(Calendar.MONTH),
                    mExpectedReturnDate.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        new AlertDialog.Builder(getActivity())
                .setTitle("Borrow Item Details")
                .setView(v)
                .setPositiveButton("Confirm Borrow", (dialog, which) -> {
                    String borrowerName = borrowerNameField.getText().toString().trim();
                    String qtyString = quantityField.getText().toString().trim();

                    if (borrowerName.isEmpty() || qtyString.isEmpty()) {
                        Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = Integer.parseInt(qtyString);
                    if (quantity <= 0) {
                        Toast.makeText(getActivity(), "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean success = SupplyLab.get(getActivity()).borrowItem(
                            item.getId(),
                            borrowerName,
                            quantity,
                            System.currentTimeMillis(),
                            mExpectedReturnDate.getTimeInMillis()
                    );

                    if (success) {
                        Toast.makeText(getActivity(), "Item borrowed successfully", Toast.LENGTH_SHORT).show();
                        updateStats();
                    } else {
                        Toast.makeText(getActivity(), "Insufficient stock!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReturnItemDialog() {
        List<BorrowRecord> activeBorrows = SupplyLab.get(getActivity()).getActiveBorrowRecords();

        if (activeBorrows.isEmpty()) {
            Toast.makeText(getActivity(), "No active borrow records found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] displayStrings = new String[activeBorrows.size()];
        for (int i = 0; i < activeBorrows.size(); i++) {
            BorrowRecord record = activeBorrows.get(i);
            SupplyItem item = SupplyLab.get(getActivity()).getItem(record.getItemId());
            String itemName = (item != null && item.getName() != null) ? item.getName() : "Unknown Item";
            displayStrings[i] = itemName + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
        }

        new AlertDialog.Builder(getActivity())
                .setTitle("Select Item to Return")
                .setItems(displayStrings, (dialog, which) -> {
                    showReturnQuantityDialog(activeBorrows.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReturnQuantityDialog(BorrowRecord record) {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(record.getQuantity()));
        
        // Add padding to make it look better
        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        new AlertDialog.Builder(getActivity())
                .setTitle("Return Quantity")
                .setMessage("Enter quantity to return for " + record.getBorrowerName() + " (Max: " + record.getQuantity() + ")")
                .setView(input)
                .setPositiveButton("Return", (dialog, which) -> {
                    String qtyStr = input.getText().toString();
                    if (qtyStr.isEmpty()) return;

                    try {
                        int returnQty = Integer.parseInt(qtyStr);
                        if (returnQty <= 0 || returnQty > record.getQuantity()) {
                            Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean success = SupplyLab.get(getActivity()).returnItem(record, returnQty);
                        if (success) {
                            Toast.makeText(getActivity(), "Item returned successfully", Toast.LENGTH_SHORT).show();
                            updateStats();
                        } else {
                            Toast.makeText(getActivity(), "Error returning item", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(getActivity(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDateButtonText(MaterialButton button) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        button.setText("Expected Return: " + dateFormat.format(mExpectedReturnDate.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }

    private void updateStats() {
        List<SupplyItem> items = SupplyLab.get(getActivity()).getItems();
        int total = items.size();
        int borrowedCount = 0;
        for (SupplyItem item : items) {
            if (item.isBorrowed()) {
                borrowedCount++;
            }
        }
        int available = total - borrowedCount;

        mTotalCountTextView.setText(String.valueOf(total));
        mAvailableCountTextView.setText(String.valueOf(available));
        mBorrowedCountTextView.setText(String.valueOf(borrowedCount));
    }
}
