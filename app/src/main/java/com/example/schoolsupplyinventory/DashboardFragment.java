package com.example.schoolsupplyinventory;

import android.content.Intent;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {

    private static final String ACTION_REQUEST_SUBMITTED = "REQUEST_SUBMITTED";
    private static final String ACTION_RETURN_REQUESTED = "RETURN_REQUESTED";
    private static final String ACTION_REQUEST_APPROVED = "REQUEST_APPROVED";
    private static final String ACTION_REQUEST_REJECTED = "REQUEST_REJECTED";

    private TextView mTotalCountText, mConsumableCountText, mBorrowableCountText, mBorrowedCountText, mLowStockCountText, mUserNameText;
    private TextView mActivitySectionTitle;
    private ViewGroup mRecentActivityContainer;
    private View mAddItemCard;
    private MaterialButton mReportsButton;

    private Calendar mExpectedReturnDate = Calendar.getInstance();
    private SupplyItem mSelectedBorrowItem, mSelectedConsumableItem;
    private BorrowRecord mSelectedReturnRecord;
    private boolean mIsAdmin = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsAdmin = SupplyLab.get(getActivity()).isAdmin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mTotalCountText = v.findViewById(R.id.dashboard_total_count);
        mConsumableCountText = v.findViewById(R.id.dashboard_consumable_count);
        mBorrowableCountText = v.findViewById(R.id.dashboard_borrowable_count);
        mBorrowedCountText = v.findViewById(R.id.dashboard_borrowed_count);
        mLowStockCountText = v.findViewById(R.id.dashboard_low_stock_count);
        mUserNameText = v.findViewById(R.id.dashboard_user_name);
        mActivitySectionTitle = v.findViewById(R.id.dashboard_activity_section_title);
        mActivitySectionTitle.setText(mIsAdmin ? "Recent Inventory Activity" : "Request history");
        mRecentActivityContainer = v.findViewById(R.id.recent_activity_container);

        mAddItemCard = v.findViewById(R.id.dashboard_add_item);
        mReportsButton = v.findViewById(R.id.dashboard_reports);

        View borrowCard = v.findViewById(R.id.dashboard_borrow_item);
        View returnCard = v.findViewById(R.id.dashboard_return_item);
        View useCard = v.findViewById(R.id.dashboard_use_consumable);
        View myRequestsCard = v.findViewById(R.id.dashboard_my_requests);
        View pendingRequestsCard = v.findViewById(R.id.dashboard_pending_requests);

        // Reflect Role in Header
        if (mIsAdmin) {
            mUserNameText.setText("System Admin");
        } else {
            mUserNameText.setText(SupplyLab.get(getActivity()).getCurrentUser());
        }

        // Action: View Inventory (Available for both)
        v.findViewById(R.id.dashboard_view_inventory).setOnClickListener(view -> loadFragment(new SupplyListFragment()));
        
        // Return is available for both roles (Staff requests, Admin issues)
        returnCard.setVisibility(View.VISIBLE);

        if (mIsAdmin) {
            mAddItemCard.setVisibility(View.VISIBLE);
            mAddItemCard.setOnClickListener(view -> startActivity(SupplyPagerActivity.newIntent(getActivity(), null)));
            mReportsButton.setVisibility(View.VISIBLE);
            mReportsButton.setOnClickListener(v1 -> loadFragment(new ReportsFragment()));
            borrowCard.setVisibility(View.VISIBLE);
            useCard.setVisibility(View.VISIBLE);
            myRequestsCard.setVisibility(View.GONE);
            pendingRequestsCard.setVisibility(View.VISIBLE);
            pendingRequestsCard.setOnClickListener(v1 -> loadFragment(new AdminPendingRequestsFragment()));
            
            returnCard.setOnClickListener(v1 -> showReturnBottomSheet());
            borrowCard.setOnClickListener(v1 -> showBorrowBottomSheet());
            useCard.setOnClickListener(v1 -> showUseConsumableDialog());
        } else {
            mAddItemCard.setVisibility(View.GONE);
            mReportsButton.setVisibility(View.GONE);
            borrowCard.setVisibility(View.GONE);
            useCard.setVisibility(View.GONE);
            myRequestsCard.setVisibility(View.VISIBLE);
            myRequestsCard.setOnClickListener(v1 -> loadFragment(new StaffMyRequestsFragment()));
            pendingRequestsCard.setVisibility(View.GONE);
            
            returnCard.setOnClickListener(v1 -> showStaffReturnBottomSheet());
        }
        
        v.findViewById(R.id.dashboard_logout).setOnClickListener(v1 -> {
            SupplyLab lab = SupplyLab.get(getActivity());
            lab.setCurrentUser("");
            lab.setCurrentRole("");
            requireActivity().getSharedPreferences("InventoSchool", android.content.Context.MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        updateStats();
        loadRecentActivity();

        return v;
    }

    private void loadFragment(Fragment fragment) {
        if (getActivity() instanceof InventoryActivity) {
            ((InventoryActivity) getActivity()).loadFragment(fragment);
        }
    }

    private void showUseConsumableDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_return, null);
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.bottom_sheet_title);
        title.setText("Use Consumable Supply");
        
        MaterialAutoCompleteTextView itemSearch = view.findViewById(R.id.return_item_autocomplete);
        itemSearch.setHint("Search consumable item...");
        itemSearch.setThreshold(0);
        
        TextView infoText = view.findViewById(R.id.return_info_text);
        TextInputEditText qtyEdit = view.findViewById(R.id.return_qty_edit);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_return);
        confirmBtn.setText("Confirm Use");

        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            List<SupplyItem> consumables = items.stream()
                    .filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType()))
                    .sorted(Comparator.comparing(SupplyItem::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            List<String> names = consumables.stream()
                    .map(i -> i.getName() + " (Avail: " + i.getAvailableQuantity() + " " + i.getUnit() + ")")
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            itemSearch.setAdapter(adapter);
            itemSearch.setOnClickListener(v -> itemSearch.showDropDown());
            itemSearch.setOnItemClickListener((parent, v, position, id) -> {
                mSelectedConsumableItem = consumables.get(position);
                infoText.setText("Consuming: " + mSelectedConsumableItem.getName());
                qtyEdit.setText("1");
            });
        });

        confirmBtn.setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText().toString().trim();
            if (mSelectedConsumableItem == null || qtyStr.isEmpty()) return;

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedConsumableItem.getAvailableQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).useConsumableAsync(mSelectedConsumableItem.getId(), qty, success -> {
                if (success) {
                    updateStats();
                    loadRecentActivity();
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
    }

    private void showBorrowBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_borrow, null);
        bottomSheetDialog.setContentView(view);

        MaterialAutoCompleteTextView itemSearch = view.findViewById(R.id.borrow_item_autocomplete);
        itemSearch.setThreshold(0);

        TextInputLayout unitIdLayout = view.findViewById(R.id.borrow_unit_id_layout);
        MaterialAutoCompleteTextView unitIdAutocomplete = view.findViewById(R.id.borrow_unit_id_autocomplete);
        unitIdAutocomplete.setThreshold(0);

        TextView itemNameDisplay = view.findViewById(R.id.borrow_item_name_display);
        TextInputEditText borrowerNameEdit = view.findViewById(R.id.borrower_name_edit);
        TextInputEditText qtyEdit = view.findViewById(R.id.borrow_qty_edit);
        MaterialButton dateBtn = view.findViewById(R.id.btn_select_date);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_borrow);

        mExpectedReturnDate = Calendar.getInstance();
        mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 7);
        updateDateButtonText(dateBtn);

        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            List<SupplyItem> borrowables = items.stream()
                    .filter(item -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(item.getItemType()))
                    .sorted(Comparator.comparing(SupplyItem::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            List<String> names = borrowables.stream()
                    .map(item -> item.getName() + " (Stock: " + item.getAvailableQuantity() + ")")
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            itemSearch.setAdapter(adapter);
            itemSearch.setOnClickListener(v -> itemSearch.showDropDown());
            itemSearch.setOnItemClickListener((parent, view1, position, id) -> {
                mSelectedBorrowItem = borrowables.get(position);
                itemNameDisplay.setText("Item: " + mSelectedBorrowItem.getName() + " (Avail: " + mSelectedBorrowItem.getAvailableQuantity() + ")");
                qtyEdit.setText("1");

                List<String> allUnits = mSelectedBorrowItem.getUnitIdentifiersList();
                if (allUnits.isEmpty()) {
                    unitIdLayout.setVisibility(View.GONE);
                } else {
                    unitIdLayout.setVisibility(View.VISIBLE);
                    SupplyLab.get(getActivity()).getActiveBorrowRecordsAsync(records -> {
                        List<String> borrowed = records.stream()
                                .filter(r -> r.getItemId().equals(mSelectedBorrowItem.getId()))
                                .map(BorrowRecord::getUnitId).collect(Collectors.toList());
                        
                        List<String> available = allUnits.stream()
                                .filter(u -> !borrowed.contains(u)).collect(Collectors.toList());
                        
                        unitIdAutocomplete.setAdapter(new ArrayAdapter<>(requireContext(), 
                                android.R.layout.simple_dropdown_item_1line, available));
                        unitIdAutocomplete.setOnClickListener(v1 -> unitIdAutocomplete.showDropDown());
                    });
                }
            });
        });

        dateBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Return Date")
                    .setSelection(mExpectedReturnDate.getTimeInMillis()).build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                mExpectedReturnDate.setTimeInMillis(selection);
                updateDateButtonText(dateBtn);
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        confirmBtn.setOnClickListener(v -> {
            String borrower = borrowerNameEdit.getText().toString().trim();
            String qtyStr = qtyEdit.getText().toString().trim();
            String unitId = unitIdAutocomplete.getText().toString().trim();
            
            if (mSelectedBorrowItem == null || borrower.isEmpty() || qtyStr.isEmpty()) return;

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedBorrowItem.getAvailableQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).borrowItemAsync(mSelectedBorrowItem.getId(), borrower, qty, 
                System.currentTimeMillis(), mExpectedReturnDate.getTimeInMillis(), unitId, success -> {
                    if (success) {
                        updateStats();
                        loadRecentActivity();
                        bottomSheetDialog.dismiss();
                    }
                });
        });
        bottomSheetDialog.show();
    }

    private void showReturnBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_return, null);
        bottomSheetDialog.setContentView(view);

        MaterialAutoCompleteTextView recordSearch = view.findViewById(R.id.return_item_autocomplete);
        recordSearch.setThreshold(0);

        TextView returnInfoText = view.findViewById(R.id.return_info_text);
        TextInputEditText qtyEdit = view.findViewById(R.id.return_qty_edit);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_return);

        SupplyLab lab = SupplyLab.get(getActivity());
        lab.getActiveBorrowRecordsAsync(records -> {
            lab.getItemsAsync(items -> {
                List<String> displayStrings = records.stream()
                        .map(record -> {
                            SupplyItem item = items.stream()
                                    .filter(i -> i.getId().equals(record.getItemId()))
                                    .findFirst().orElse(null);
                            String unitInfo = (record.getUnitId() != null && !record.getUnitId().isEmpty()) 
                                    ? " [" + record.getUnitId() + "]" : "";
                            return (item != null ? item.getName() : "Item") + unitInfo + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
                        }).collect(Collectors.toList());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, displayStrings);
                recordSearch.setAdapter(adapter);
                recordSearch.setOnClickListener(v -> recordSearch.showDropDown());
                recordSearch.setOnItemClickListener((parent, view1, position, id) -> {
                    mSelectedReturnRecord = records.get(position);
                    returnInfoText.setText("Returning: " + recordSearch.getAdapter().getItem(position));
                    qtyEdit.setText(String.valueOf(mSelectedReturnRecord.getQuantity()));
                });
            });
        });

        confirmBtn.setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText().toString().trim();
            if (mSelectedReturnRecord == null || qtyStr.isEmpty()) return;
            int qty = Integer.parseInt(qtyStr);
            SupplyLab.get(getActivity()).returnItemAsync(mSelectedReturnRecord, qty, success -> {
                if (success) {
                    updateStats();
                    loadRecentActivity();
                    bottomSheetDialog.dismiss();
                }
            });
        });
        bottomSheetDialog.show();
    }

    private void showStaffReturnBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_return, null);
        bottomSheetDialog.setContentView(view);

        TextView title = view.findViewById(R.id.bottom_sheet_title);
        title.setText("Request Item Return");

        MaterialAutoCompleteTextView recordSearch = view.findViewById(R.id.return_item_autocomplete);
        recordSearch.setHint("Select borrowed item to return...");
        recordSearch.setThreshold(0);

        TextView returnInfoText = view.findViewById(R.id.return_info_text);
        TextInputEditText qtyEdit = view.findViewById(R.id.return_qty_edit);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_return);
        confirmBtn.setText("Submit Return Request");

        SupplyLab lab = SupplyLab.get(getActivity());
        String currentUser = lab.getCurrentUser();

        lab.getActiveBorrowRecordsForUserAsync(currentUser, records -> {
            lab.getItemsAsync(items -> {
                List<String> displayStrings = records.stream()
                        .map(record -> {
                            SupplyItem item = items.stream()
                                    .filter(i -> i.getId().equals(record.getItemId()))
                                    .findFirst().orElse(null);
                            String unitInfo = (record.getUnitId() != null && !record.getUnitId().isEmpty()) 
                                    ? " [" + record.getUnitId() + "]" : "";
                            return (item != null ? item.getName() : "Item") + unitInfo + " (" + record.getQuantity() + ")";
                        }).collect(Collectors.toList());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, displayStrings);
                recordSearch.setAdapter(adapter);
                recordSearch.setOnClickListener(v -> recordSearch.showDropDown());
                recordSearch.setOnItemClickListener((parent, view1, position, id) -> {
                    mSelectedReturnRecord = records.get(position);
                    returnInfoText.setText("Requesting return for: " + recordSearch.getAdapter().getItem(position));
                    qtyEdit.setText(String.valueOf(mSelectedReturnRecord.getQuantity()));
                });
            });
        });

        confirmBtn.setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText().toString().trim();
            if (mSelectedReturnRecord == null || qtyStr.isEmpty()) return;
            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedReturnRecord.getQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyRequest request = new SupplyRequest();
            request.setItemId(mSelectedReturnRecord.getItemId());
            request.setRequesterName(currentUser);
            request.setQuantity(qty);
            request.setRequestType(SupplyRequest.TYPE_RETURN);
            request.setBorrowRecordId(mSelectedReturnRecord.getId());
            request.setUnitId(mSelectedReturnRecord.getUnitId());

            lab.submitSupplyRequestAsync(request, success -> {
                if (success) {
                    Toast.makeText(getActivity(), "Return request submitted for verification", Toast.LENGTH_SHORT).show();
                    loadRecentActivity();
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(getActivity(), "Failed to submit request", Toast.LENGTH_SHORT).show();
                }
            });
        });
        bottomSheetDialog.show();
    }

    private void updateStats() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            if (!isAdded()) return;
            int total = items.size();
            long consumable = items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType())).count();
            long borrowable = items.stream().filter(i -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(i.getItemType())).count();
            int borrowed = items.stream().mapToInt(SupplyItem::getBorrowedQuantity).sum();
            long lowStock = items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType()) && i.getAvailableQuantity() <= 5 && i.getAvailableQuantity() > 0).count();

            mTotalCountText.setText(String.valueOf(total));
            mConsumableCountText.setText(String.valueOf(consumable));
            mBorrowableCountText.setText(String.valueOf(borrowable));
            mBorrowedCountText.setText(String.valueOf(borrowed));
            mLowStockCountText.setText(String.valueOf(lowStock));
        });
    }

    private void loadRecentActivity() {
        mRecentActivityContainer.removeAllViews();
        SupplyLab.get(getActivity()).getAllHistoryAsync(records -> {
            if (!isAdded()) return;
            List<HistoryRecord> toShow;
            if (mIsAdmin) {
                toShow = new ArrayList<>();
                for (int i = 0; i < records.size() && i < 5; i++) {
                    toShow.add(records.get(i));
                }
            } else {
                String me = SupplyLab.get(requireContext()).getCurrentUser();
                toShow = records.stream()
                        .filter(r -> isStaffRequestHistoryEntry(r, me))
                        .limit(12)
                        .collect(Collectors.toList());
            }
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            SimpleDateFormat fmt = new SimpleDateFormat("MMM dd", Locale.getDefault());
            for (HistoryRecord record : toShow) {
                View activityView = inflater.inflate(R.layout.list_item_activity, mRecentActivityContainer, false);
                ((TextView) activityView.findViewById(R.id.activity_title)).setText(formatActivityTitle(record));
                ((TextView) activityView.findViewById(R.id.activity_subtitle)).setText(record.getDetails());
                ((TextView) activityView.findViewById(R.id.activity_time)).setText(fmt.format(record.getTimestamp()));
                mRecentActivityContainer.addView(activityView);
            }
        });
    }

    /**
     * Staff dashboard: only request workflow (their submissions + admin approve/reject on their requests).
     */
    private static boolean isStaffRequestHistoryEntry(HistoryRecord r, String staffEmail) {
        if (r.getAction() == null || staffEmail == null || staffEmail.isEmpty()) {
            return false;
        }
        switch (r.getAction()) {
            case ACTION_REQUEST_SUBMITTED:
            case ACTION_RETURN_REQUESTED:
                return staffEmail.equalsIgnoreCase(r.getUser());
            case ACTION_REQUEST_APPROVED:
            case ACTION_REQUEST_REJECTED:
                String details = r.getDetails() != null ? r.getDetails() : "";
                return details.contains(staffEmail);
            default:
                return false;
        }
    }

    private static String formatActivityTitle(HistoryRecord record) {
        String action = record.getAction() != null ? record.getAction() : "";
        String item = record.getItemName() != null ? record.getItemName() : "";
        if (ACTION_REQUEST_SUBMITTED.equals(action)) {
            return "Request submitted: " + item;
        }
        if (ACTION_RETURN_REQUESTED.equals(action)) {
            return "Return requested: " + item;
        }
        if (ACTION_REQUEST_APPROVED.equals(action)) {
            return "Request approved: " + item;
        }
        if (ACTION_REQUEST_REJECTED.equals(action)) {
            return "Request rejected: " + item;
        }
        return action + ": " + item;
    }

    private void updateDateButtonText(MaterialButton button) {
        button.setText("Due Date: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(mExpectedReturnDate.getTime()));
    }

    @Override
    public void onResume() { super.onResume(); updateStats(); loadRecentActivity(); }
}
