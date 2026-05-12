package com.example.schoolsupplyinventory;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
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
    private TextView mReturnedCountTextView;
    private View mViewInventoryButton;
    private View mAddItemButton;
    private View mBorrowButton;
    private View mReturnButton;
    private MaterialButton mReportsButton;
    private MaterialButton mLogoutButton;
    private ViewGroup mRecentActivityContainer;

    private Calendar mExpectedReturnDate = Calendar.getInstance();
    private SupplyItem mSelectedBorrowItem;
    private BorrowRecord mSelectedReturnRecord;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mTotalCountTextView = v.findViewById(R.id.dashboard_total_count);
        mAvailableCountTextView = v.findViewById(R.id.dashboard_available_count);
        mBorrowedCountTextView = v.findViewById(R.id.dashboard_borrowed_count);
        mReturnedCountTextView = v.findViewById(R.id.dashboard_returned_count);
        
        mViewInventoryButton = v.findViewById(R.id.dashboard_view_inventory);
        mAddItemButton = v.findViewById(R.id.dashboard_add_item);
        mBorrowButton = v.findViewById(R.id.dashboard_borrow_item);
        mReturnButton = v.findViewById(R.id.dashboard_return_item);
        mReportsButton = v.findViewById(R.id.dashboard_reports);
        mLogoutButton = v.findViewById(R.id.dashboard_logout);
        mRecentActivityContainer = v.findViewById(R.id.recent_activity_container);

        updateStats();
        loadRecentActivity();

        mViewInventoryButton.setOnClickListener(view -> {
            if (getActivity() instanceof InventoryActivity) {
                ((InventoryActivity) getActivity()).loadFragment(new SupplyListFragment());
            }
        });

        mAddItemButton.setOnClickListener(view -> {
            SupplyItem item = new SupplyItem();
            SupplyLab.get(getActivity()).addSupply(item);
            Intent intent = SupplyPagerActivity.newIntent(getActivity(), item.getId());
            startActivity(intent);
        });

        mBorrowButton.setOnClickListener(v1 -> showBorrowBottomSheet());
        
        mReturnButton.setOnClickListener(v1 -> showReturnBottomSheet());
        
        mReportsButton.setOnClickListener(v1 -> {
            if (getActivity() instanceof InventoryActivity) {
                ((InventoryActivity) getActivity()).loadFragment(new ReportsFragment());
            }
        });
        
        mLogoutButton.setOnClickListener(v1 -> {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return v;
    }

    private void showBorrowBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_borrow, null);
        bottomSheetDialog.setContentView(view);

        MaterialAutoCompleteTextView itemSearch = view.findViewById(R.id.borrow_item_autocomplete);
        TextView itemNameDisplay = view.findViewById(R.id.borrow_item_name_display);
        TextInputEditText borrowerNameEdit = view.findViewById(R.id.borrower_name_edit);
        TextInputEditText qtyEdit = view.findViewById(R.id.borrow_qty_edit);
        MaterialButton dateBtn = view.findViewById(R.id.btn_select_date);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_borrow);

        mExpectedReturnDate = Calendar.getInstance();
        mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 7);
        updateDateButtonText(dateBtn);

        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            List<SupplyItem> availableItems = items.stream()
                    .filter(item -> item.getQuantity() > 0)
                    .collect(Collectors.toList());

            String[] names = availableItems.stream()
                    .map(item -> (item.getName() != null ? item.getName() : "Unnamed") + " (" + item.getQuantity() + ")")
                    .toArray(String[]::new);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, names);
            itemSearch.setAdapter(adapter);

            itemSearch.setOnItemClickListener((parent, view1, position, id) -> {
                mSelectedBorrowItem = availableItems.get(position);
                itemNameDisplay.setText("Item: " + mSelectedBorrowItem.getName() + " (Stock: " + mSelectedBorrowItem.getQuantity() + ")");
                qtyEdit.setText("1");
            });
        });

        dateBtn.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view1, year, month, day) -> {
                mExpectedReturnDate.set(year, month, day);
                updateDateButtonText(dateBtn);
            }, mExpectedReturnDate.get(Calendar.YEAR), mExpectedReturnDate.get(Calendar.MONTH), mExpectedReturnDate.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis());
            dpd.show();
        });

        confirmBtn.setOnClickListener(v -> {
            String borrower = borrowerNameEdit.getText().toString().trim();
            String qtyStr = qtyEdit.getText().toString().trim();

            if (mSelectedBorrowItem == null || borrower.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedBorrowItem.getQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).borrowItemAsync(
                    mSelectedBorrowItem.getId(),
                    borrower,
                    qty,
                    System.currentTimeMillis(),
                    mExpectedReturnDate.getTimeInMillis(),
                    success -> {
                        if (success) {
                            Toast.makeText(getActivity(), "Item issued successfully", Toast.LENGTH_SHORT).show();
                            updateStats();
                            loadRecentActivity();
                            bottomSheetDialog.dismiss();
                        } else {
                            Toast.makeText(getActivity(), "Error issuing item", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        bottomSheetDialog.show();
    }

    private void showReturnBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_return, null);
        bottomSheetDialog.setContentView(view);

        MaterialAutoCompleteTextView recordSearch = view.findViewById(R.id.return_item_autocomplete);
        TextView returnInfoText = view.findViewById(R.id.return_info_text);
        TextInputEditText qtyEdit = view.findViewById(R.id.return_qty_edit);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_return);

        SupplyLab.get(getActivity()).getActiveBorrowRecordsAsync(records -> {
            if (records.isEmpty()) {
                Toast.makeText(getActivity(), "No active borrow records", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] displayStrings = new String[records.size()];
            for (int i = 0; i < records.size(); i++) {
                BorrowRecord record = records.get(i);
                SupplyItem item = SupplyLab.get(getActivity()).getItem(record.getItemId());
                String itemName = (item != null) ? item.getName() : "Unknown Item";
                displayStrings[i] = itemName + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, displayStrings);
            recordSearch.setAdapter(adapter);

            recordSearch.setOnItemClickListener((parent, view1, position, id) -> {
                mSelectedReturnRecord = records.get(position);
                returnInfoText.setText("Returning from: " + mSelectedReturnRecord.getBorrowerName());
                qtyEdit.setText(String.valueOf(mSelectedReturnRecord.getQuantity()));
            });
        });

        confirmBtn.setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText().toString().trim();
            if (mSelectedReturnRecord == null || qtyStr.isEmpty()) {
                Toast.makeText(getActivity(), "Please select a record", Toast.LENGTH_SHORT).show();
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedReturnRecord.getQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).returnItemAsync(mSelectedReturnRecord, qty, success -> {
                if (success) {
                    Toast.makeText(getActivity(), "Item returned successfully", Toast.LENGTH_SHORT).show();
                    updateStats();
                    loadRecentActivity();
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(getActivity(), "Error returning item", Toast.LENGTH_SHORT).show();
                }
            });
        });

        bottomSheetDialog.show();
    }

    private void showReturnItemDialog() {
        SupplyLab.get(getActivity()).getActiveBorrowRecordsAsync(activeBorrows -> {
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

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Select Item to Return")
                    .setItems(displayStrings, (dialog, which) -> {
                        showReturnQuantityDialog(activeBorrows.get(which));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showReturnQuantityDialog(BorrowRecord record) {
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(record.getQuantity()));
        
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setPadding(48, 24, 48, 0);
        layout.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Return Quantity")
                .setMessage("Enter quantity to return for " + record.getBorrowerName() + " (Max: " + record.getQuantity() + ")")
                .setView(layout)
                .setPositiveButton("Return", (dialog, which) -> {
                    String qtyStr = input.getText().toString();
                    if (qtyStr.isEmpty()) return;

                    try {
                        int returnQty = Integer.parseInt(qtyStr);
                        if (returnQty <= 0 || returnQty > record.getQuantity()) {
                            Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SupplyLab.get(getActivity()).returnItemAsync(record, returnQty, success -> {
                            if (success) {
                                Toast.makeText(getActivity(), "Item returned successfully", Toast.LENGTH_SHORT).show();
                                updateStats();
                                loadRecentActivity();
                            } else {
                                Toast.makeText(getActivity(), "Error returning item", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(getActivity(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadRecentActivity() {
        if (mRecentActivityContainer == null) return;
        mRecentActivityContainer.removeAllViews();
        
        SupplyLab.get(getActivity()).getActiveBorrowRecordsAsync(borrows -> {
            int count = 0;
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (BorrowRecord record : borrows) {
                if (count >= 3) break;
                
                View activityView = inflater.inflate(R.layout.list_item_activity, mRecentActivityContainer, false);
                TextView title = activityView.findViewById(R.id.activity_title);
                TextView subtitle = activityView.findViewById(R.id.activity_subtitle);
                TextView time = activityView.findViewById(R.id.activity_time);
                
                title.setText("Item On Loan");
                SupplyItem item = SupplyLab.get(getActivity()).getItem(record.getItemId());
                String itemName = item != null ? item.getName() : "Item";
                subtitle.setText(record.getBorrowerName() + " has " + record.getQuantity() + " " + itemName);
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                time.setText(sdf.format(record.getDateBorrowed()));
                
                mRecentActivityContainer.addView(activityView);
                count++;
            }
            
            if (count == 0) {
                TextView emptyText = new TextView(requireContext());
                emptyText.setText("No recent activity");
                emptyText.setTextColor(Color.parseColor("#B3B3C3"));
                emptyText.setPadding(0, 32, 0, 32);
                mRecentActivityContainer.addView(emptyText);
            }
        });
    }

    private void updateDateButtonText(MaterialButton button) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        button.setText("Due Date: " + dateFormat.format(mExpectedReturnDate.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
        loadRecentActivity();
    }

    private void updateStats() {
        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getItemsAsync(items -> {
            lab.getActiveBorrowRecordsAsync(borrows -> {
                int available = 0;
                for (SupplyItem item : items) {
                    available += item.getQuantity();
                }

                int borrowed = 0;
                for (BorrowRecord record : borrows) {
                    borrowed += record.getQuantity();
                }

                mTotalCountTextView.setText(String.valueOf(available + borrowed));
                mAvailableCountTextView.setText(String.valueOf(available));
                mBorrowedCountTextView.setText(String.valueOf(borrowed));
                mReturnedCountTextView.setText("24");
            });
        });
    }
}
