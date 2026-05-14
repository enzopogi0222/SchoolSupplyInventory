package com.example.schoolsupplyinventory;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {

    private TextView mTotalCountText, mConsumableCountText, mBorrowableCountText, mBorrowedCountText, mLowStockCountText;
    private ViewGroup mRecentActivityContainer;
    private View mUseConsumableButton;

    private static final String CHANNEL_ID = "supply_alerts";
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Calendar mExpectedReturnDate = Calendar.getInstance();
    private SupplyItem mSelectedBorrowItem, mSelectedConsumableItem;
    private BorrowRecord mSelectedReturnRecord;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> { if (isGranted) checkInventoryAlerts(); }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        mTotalCountText = v.findViewById(R.id.dashboard_total_count);
        mConsumableCountText = v.findViewById(R.id.dashboard_consumable_count);
        mBorrowableCountText = v.findViewById(R.id.dashboard_borrowable_count);
        mBorrowedCountText = v.findViewById(R.id.dashboard_borrowed_count);
        mLowStockCountText = v.findViewById(R.id.dashboard_low_stock_count);
        mRecentActivityContainer = v.findViewById(R.id.recent_activity_container);

        v.findViewById(R.id.dashboard_view_inventory).setOnClickListener(view -> loadFragment(new SupplyListFragment()));
        v.findViewById(R.id.dashboard_add_item).setOnClickListener(view -> startActivity(SupplyPagerActivity.newIntent(getActivity(), null)));
        v.findViewById(R.id.dashboard_borrow_item).setOnClickListener(v1 -> showBorrowBottomSheet());
        v.findViewById(R.id.dashboard_return_item).setOnClickListener(v1 -> showReturnBottomSheet());
        
        mUseConsumableButton = v.findViewById(R.id.dashboard_use_consumable);
        mUseConsumableButton.setOnClickListener(v1 -> showUseConsumableDialog());
        
        v.findViewById(R.id.dashboard_reports).setOnClickListener(v1 -> loadFragment(new ReportsFragment()));
        v.findViewById(R.id.dashboard_logout).setOnClickListener(v1 -> {
            startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        updateStats();
        loadRecentActivity();
        checkAndRequestNotificationPermission();

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
                Toast.makeText(getActivity(), "Invalid quantity or out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).useConsumableAsync(mSelectedConsumableItem.getId(), qty, success -> {
                if (success) {
                    Toast.makeText(getActivity(), "Stock updated permanently", Toast.LENGTH_SHORT).show();
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
            if (mSelectedBorrowItem == null || borrower.isEmpty() || qtyStr.isEmpty()) return;

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mSelectedBorrowItem.getAvailableQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity or out of stock", Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyLab.get(getActivity()).borrowItemAsync(mSelectedBorrowItem.getId(), borrower, qty, 
                System.currentTimeMillis(), mExpectedReturnDate.getTimeInMillis(), success -> {
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
                            return (item != null ? item.getName() : "Item") + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
                        }).collect(Collectors.toList());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, displayStrings);
                recordSearch.setAdapter(adapter);
                recordSearch.setOnClickListener(v -> recordSearch.showDropDown());
                recordSearch.setOnItemClickListener((parent, view1, position, id) -> {
                    mSelectedReturnRecord = records.get(position);
                    returnInfoText.setText("Returning from: " + mSelectedReturnRecord.getBorrowerName());
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

    private void updateStats() {
        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            int total = items.size();
            long consumable = items.stream().filter(i -> SupplyItem.TYPE_CONSUMABLE.equalsIgnoreCase(i.getItemType())).count();
            long borrowable = items.stream().filter(i -> SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(i.getItemType())).count();
            int borrowed = items.stream().mapToInt(SupplyItem::getBorrowedQuantity).sum();
            long lowStock = items.stream().filter(i -> i.getAvailableQuantity() <= 5).count();

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
            int count = 0;
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (HistoryRecord record : records) {
                if (count >= 5) break;
                View activityView = inflater.inflate(R.layout.list_item_activity, mRecentActivityContainer, false);
                ((TextView) activityView.findViewById(R.id.activity_title)).setText(record.getAction() + ": " + record.getItemName());
                ((TextView) activityView.findViewById(R.id.activity_subtitle)).setText(record.getDetails());
                ((TextView) activityView.findViewById(R.id.activity_time)).setText(new SimpleDateFormat("MMM dd", Locale.getDefault()).format(record.getTimestamp()));
                mRecentActivityContainer.addView(activityView);
                count++;
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Inventory Alerts", NotificationManager.IMPORTANCE_HIGH);
            getActivity().getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else checkInventoryAlerts();
        } else checkInventoryAlerts();
    }

    private void checkInventoryAlerts() {
        SupplyLab.get(requireActivity()).getItemsAsync(items -> {
            long lowStock = items.stream().filter(i -> i.getAvailableQuantity() > 0 && i.getAvailableQuantity() <= 5).count();
            if (lowStock > 0) sendLocalNotification("Low Stock Alert", lowStock + " items are running low.");
        });
    }

    private void sendLocalNotification(String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle(title).setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);
        try { NotificationManagerCompat.from(requireContext()).notify(1, builder.build()); } catch (SecurityException e) {}
    }

    private void updateDateButtonText(MaterialButton button) {
        button.setText("Due Date: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(mExpectedReturnDate.getTime()));
    }

    @Override
    public void onResume() { super.onResume(); updateStats(); loadRecentActivity(); }
}
