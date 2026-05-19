package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.UUID;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {

    private static final String ACTION_REQUEST_SUBMITTED = "REQUEST_SUBMITTED";
    private static final String ACTION_RETURN_REQUESTED = "RETURN_REQUESTED";
    private static final String ACTION_REQUEST_APPROVED = "REQUEST_APPROVED";
    private static final String ACTION_REQUEST_REJECTED = "REQUEST_REJECTED";

    private TextView mTotalCountText, mConsumableCountText, mBorrowableCountText, mBorrowedCountText, mLowStockCountText;
    private TextView mGreetingText, mUserNameText, mUserRoleText;
    private TextView mActivitySectionTitle;
    private RecyclerView mRecentActivityRecycler;
    private HistoryAdapter mHistoryAdapter;
    private View mAddItemCard;
    private MaterialButton mReportsButton;
    private MaterialButton mClearActivityButton;

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
        
        mGreetingText = v.findViewById(R.id.dashboard_greeting);
        mUserNameText = v.findViewById(R.id.dashboard_user_name);
        mUserRoleText = v.findViewById(R.id.dashboard_user_role);
        
        mActivitySectionTitle = v.findViewById(R.id.dashboard_activity_section_title);
        mActivitySectionTitle.setText(mIsAdmin ? "Recent Inventory Activity" : "Request history");
        
        mRecentActivityRecycler = v.findViewById(R.id.recent_activity_recycler);
        mRecentActivityRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mHistoryAdapter = new HistoryAdapter(new ArrayList<>());
        mRecentActivityRecycler.setAdapter(mHistoryAdapter);

        mClearActivityButton = v.findViewById(R.id.dashboard_clear_activity);
        mClearActivityButton.setVisibility(View.VISIBLE);

        mAddItemCard = v.findViewById(R.id.dashboard_add_item);
        mReportsButton = v.findViewById(R.id.dashboard_reports);

        View borrowCard = v.findViewById(R.id.dashboard_borrow_item);
        View returnCard = v.findViewById(R.id.dashboard_return_item);
        View useCard = v.findViewById(R.id.dashboard_use_consumable);
        View myRequestsCard = v.findViewById(R.id.dashboard_my_requests);
        View pendingRequestsCard = v.findViewById(R.id.dashboard_pending_requests);

        // Formal Header Content
        setupFormalHeader();

        v.findViewById(R.id.dashboard_view_inventory).setOnClickListener(view -> loadFragment(new SupplyListFragment()));
        
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

        mClearActivityButton.setOnClickListener(v1 -> {
            String message = mIsAdmin ? "Are you sure you want to clear all inventory activity history?" : "Are you sure you want to clear your request history?";
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear Activity History")
                    .setMessage(message)
                    .setPositiveButton("Clear", (dialog, which) -> {
                        SupplyLab lab = SupplyLab.get(getActivity());
                        if (mIsAdmin) {
                            lab.clearHistoryAsync(result -> {
                                loadRecentActivity();
                                Toast.makeText(getActivity(), "All activity history cleared", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            lab.clearHistoryForUserAsync(lab.getCurrentUser(), result -> {
                                loadRecentActivity();
                                Toast.makeText(getActivity(), "Your history cleared", Toast.LENGTH_SHORT).show();
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        setupSwipeToDelete();
        updateStats();
        loadRecentActivity();

        return v;
    }

    private void setupFormalHeader() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else greeting = "Good evening,";
        
        mGreetingText.setText(greeting);

        if (mIsAdmin) {
            mUserNameText.setText("Administrator");
            mUserRoleText.setText("System & Inventory Control");
        } else {
            String email = SupplyLab.get(getActivity()).getCurrentUser();
            // Extracted name from email formally
            if (email.startsWith("jerica")) {
                mUserNameText.setText("Jerica");
            } else {
                mUserNameText.setText("Staff Member");
            }
            mUserRoleText.setText("Inventory Logistics Personnel");
        }
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                HistoryRecord record = mHistoryAdapter.getRecords().get(position);
                SupplyLab.get(getActivity()).deleteHistoryRecordAsync(record.getId(), result -> {
                    mHistoryAdapter.getRecords().remove(position);
                    mHistoryAdapter.notifyItemRemoved(position);
                });
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(mRecentActivityRecycler);
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
        SupplyLab.get(getActivity()).getAllHistoryAsync(records -> {
            if (!isAdded()) return;
            List<HistoryRecord> toShow;
            if (mIsAdmin) {
                toShow = new ArrayList<>();
                for (int i = 0; i < records.size() && i < 20; i++) {
                    toShow.add(records.get(i));
                }
            } else {
                String me = SupplyLab.get(requireContext()).getCurrentUser();
                toShow = records.stream()
                        .filter(r -> isStaffRequestHistoryEntry(r, me))
                        .limit(20)
                        .collect(Collectors.toList());
            }
            mHistoryAdapter.setRecords(toShow);
            mHistoryAdapter.notifyDataSetChanged();
        });
    }

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

    private class HistoryHolder extends RecyclerView.ViewHolder {
        private TextView title, subtitle, time;
        private HistoryRecord record;

        public HistoryHolder(View v) {
            super(v);
            title = v.findViewById(R.id.activity_title);
            subtitle = v.findViewById(R.id.activity_subtitle);
            time = v.findViewById(R.id.activity_time);
        }

        public void bind(HistoryRecord r) {
            record = r;
            title.setText(formatActivityTitle(record));
            subtitle.setText(record.getDetails());
            SimpleDateFormat fmt = new SimpleDateFormat("MMM dd", Locale.getDefault());
            time.setText(fmt.format(record.getTimestamp()));
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
        private List<HistoryRecord> records;
        public HistoryAdapter(List<HistoryRecord> records) { this.records = records; }
        @NonNull @Override public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HistoryHolder(LayoutInflater.from(requireContext()).inflate(R.layout.list_item_activity, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull HistoryHolder holder, int position) { holder.bind(records.get(position)); }
        @Override public int getItemCount() { return records.size(); }
        public void setRecords(List<HistoryRecord> records) { this.records = records; }
        public List<HistoryRecord> getRecords() { return records; }
    }

    @Override
    public void onResume() { super.onResume(); updateStats(); loadRecentActivity(); }
}
