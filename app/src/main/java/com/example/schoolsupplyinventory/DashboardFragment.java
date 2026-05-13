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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFrag";

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

    private static final String CHANNEL_ID = "supply_alerts";
    private static final int NOTIFY_OVERDUE = 101;
    private static final int NOTIFY_LOW_STOCK = 102;
    private static final int NOTIFY_EXPIRING = 103;
    private static final int NOTIFY_PENDING_REQUESTS = 104;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> mScanIdLauncher;
    private TextInputEditText mBorrowerNameEditRef;

    private Calendar mExpectedReturnDate = Calendar.getInstance();
    private SupplyItem mSelectedBorrowItem;
    private BorrowRecord mSelectedReturnRecord;

    private static boolean sCheckedAlertsThisSession = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        checkInventoryAlerts();
                    }
                }
        );

        mScanIdLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String scannedId = result.getData().getStringExtra("SCANNED_BARCODE");
                        String name = SupplyLab.get(getActivity()).findNameByBarcode(scannedId);
                        
                        if (mBorrowerNameEditRef != null) {
                            mBorrowerNameEditRef.setText(name != null ? name : scannedId);
                        }
                    }
                }
        );
    }

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
        checkAndRequestNotificationPermission();

        mViewInventoryButton.setOnClickListener(view -> {
            if (getActivity() instanceof InventoryActivity) {
                ((InventoryActivity) getActivity()).loadFragment(new SupplyListFragment());
            }
        });

        mAddItemButton.setOnClickListener(view -> {
            Intent intent = SupplyPagerActivity.newIntent(getActivity(), null);
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
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }

    private void showBorrowBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_borrow, null);
        bottomSheetDialog.setContentView(view);

        MaterialAutoCompleteTextView itemSearch = view.findViewById(R.id.borrow_item_autocomplete);
        TextView itemNameDisplay = view.findViewById(R.id.borrow_item_name_display);
        TextInputLayout borrowerNameLayout = view.findViewById(R.id.borrower_name_layout);
        TextInputEditText borrowerNameEdit = view.findViewById(R.id.borrower_name_edit);
        mBorrowerNameEditRef = borrowerNameEdit;
        
        borrowerNameLayout.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            mScanIdLauncher.launch(intent);
        });

        TextInputEditText qtyEdit = view.findViewById(R.id.borrow_qty_edit);
        MaterialButton dateBtn = view.findViewById(R.id.btn_select_date);
        MaterialButton confirmBtn = view.findViewById(R.id.btn_confirm_borrow);
        com.google.android.material.chip.ChipGroup dateChips = view.findViewById(R.id.quick_date_chips);

        mExpectedReturnDate = Calendar.getInstance();
        mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 7);
        updateDateButtonText(dateBtn);

        dateChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            mExpectedReturnDate = Calendar.getInstance();
            
            if (checkedId == R.id.chip_1day) {
                mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 1);
            } else if (checkedId == R.id.chip_3days) {
                mExpectedReturnDate.add(Calendar.DAY_OF_YEAR, 3);
            } else if (checkedId == R.id.chip_1week) {
                mExpectedReturnDate.add(Calendar.WEEK_OF_YEAR, 1);
            } else if (checkedId == R.id.chip_1month) {
                mExpectedReturnDate.add(Calendar.MONTH, 1);
            }
            
            updateDateButtonText(dateBtn);
        });

        SupplyLab.get(getActivity()).getItemsAsync(items -> {
            List<SupplyItem> availableItems = items.stream()
                    .filter(item -> item.getQuantity() > 0 && item.isBorrowable())
                    .collect(Collectors.toList());

            List<String> names = availableItems.stream()
                    .map(item -> {
                        String name = item.getName() != null ? item.getName() : "Unnamed";
                        String brand = item.getBrand() != null && !item.getBrand().isEmpty() ? " - " + item.getBrand() : "";
                        String tag = item.getPropertyTag() != null && !item.getPropertyTag().isEmpty() ? " [" + item.getPropertyTag() + "]" : "";
                        return name + brand + tag + " (" + item.getQuantity() + ")";
                    })
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, names);
            itemSearch.setAdapter(adapter);

            itemSearch.setOnItemClickListener((parent, view1, position, id) -> {
                String selectedValue = (String) parent.getItemAtPosition(position);
                int index = names.indexOf(selectedValue);
                if (index != -1) {
                    mSelectedBorrowItem = availableItems.get(index);
                    String brandInfo = mSelectedBorrowItem.getBrand() != null && !mSelectedBorrowItem.getBrand().isEmpty() ? 
                            " - " + mSelectedBorrowItem.getBrand() : "";
                    itemNameDisplay.setText("Item: " + mSelectedBorrowItem.getName() + brandInfo + " (Stock: " + mSelectedBorrowItem.getQuantity() + ")");
                    qtyEdit.setText("1");
                }
            });
        });

        dateBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Return Date")
                    .setSelection(mExpectedReturnDate.getTimeInMillis())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                mExpectedReturnDate.setTimeInMillis(selection);
                updateDateButtonText(dateBtn);
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        confirmBtn.setOnClickListener(v -> {
            String borrower = borrowerNameEdit.getText().toString().trim();
            String qtyStr = qtyEdit.getText().toString().trim();

            if (mSelectedBorrowItem == null || borrower.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if borrower already has an active loan
            SupplyLab.get(getActivity()).getActiveBorrowRecordsAsync(activeRecords -> {
                boolean alreadyHasLoan = activeRecords.stream()
                        .anyMatch(record -> record.getBorrowerName().equalsIgnoreCase(borrower));

                if (alreadyHasLoan) {
                    Toast.makeText(getActivity(), "Action Refused: User already has an unreturned item.", Toast.LENGTH_LONG).show();
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

            List<String> displayStrings = records.stream()
                    .map(record -> {
                        SupplyItem item = SupplyLab.get(getActivity()).getItem(record.getItemId());
                        String itemName = (item != null) ? item.getName() : "Unknown Item";
                        String brand = (item != null && item.getBrand() != null && !item.getBrand().isEmpty()) ? " - " + item.getBrand() : "";
                        String tag = (item != null && item.getPropertyTag() != null && !item.getPropertyTag().isEmpty()) ? " [" + item.getPropertyTag() + "]" : "";
                        return itemName + brand + tag + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
                    })
                    .collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, displayStrings);
            recordSearch.setAdapter(adapter);

            recordSearch.setOnItemClickListener((parent, view1, position, id) -> {
                String selectedValue = (String) parent.getItemAtPosition(position);
                int index = displayStrings.indexOf(selectedValue);
                if (index != -1) {
                    mSelectedReturnRecord = records.get(index);
                    returnInfoText.setText("Returning from: " + mSelectedReturnRecord.getBorrowerName());
                    qtyEdit.setText(String.valueOf(mSelectedReturnRecord.getQuantity()));
                }
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
                String brand = (item != null && item.getBrand() != null && !item.getBrand().isEmpty()) ? " - " + item.getBrand() : "";
                String tag = (item != null && item.getPropertyTag() != null && !item.getPropertyTag().isEmpty()) ? " [" + item.getPropertyTag() + "]" : "";
                displayStrings[i] = itemName + brand + tag + " - " + record.getBorrowerName() + " (" + record.getQuantity() + ")";
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
            long currentTime = System.currentTimeMillis();

            for (BorrowRecord record : borrows) {
                if (count >= 5) break; // Increased to show more activity
                
                View activityView = inflater.inflate(R.layout.list_item_activity, mRecentActivityContainer, false);
                TextView title = activityView.findViewById(R.id.activity_title);
                TextView subtitle = activityView.findViewById(R.id.activity_subtitle);
                TextView time = activityView.findViewById(R.id.activity_time);
                View icon = activityView.findViewById(R.id.activity_icon);
                
                boolean isOverdue = record.getExpectedReturnDate() != null && 
                                   record.getExpectedReturnDate().getTime() < currentTime;

                if (isOverdue) {
                    title.setText("OVERDUE ITEM");
                    title.setTextColor(Color.RED);
                    subtitle.setTextColor(Color.RED);
                    icon.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.color_error));
                } else {
                    title.setText("Item On Loan");
                    title.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    subtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    icon.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_purple));
                }

                SupplyItem item = SupplyLab.get(getActivity()).getItem(record.getItemId());
                String itemName = item != null ? item.getName() : "Item";
                String brand = (item != null && item.getBrand() != null && !item.getBrand().isEmpty()) ? " (" + item.getBrand() + ")" : "";
                subtitle.setText(record.getBorrowerName() + " has " + record.getQuantity() + " " + itemName + brand);
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                time.setText(sdf.format(record.getDateBorrowed()));

                activityView.setOnClickListener(v1 -> {
                    long currentSelection = record.getExpectedReturnDate() != null ? 
                            record.getExpectedReturnDate().getTime() : System.currentTimeMillis();
                            
                    MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Update Due Date")
                            .setSelection(currentSelection)
                            .build();

                    datePicker.addOnPositiveButtonClickListener(selection -> {
                        record.setExpectedReturnDate(new java.util.Date(selection));
                        SupplyLab.get(getActivity()).updateBorrowRecordAsync(record, success -> {
                            if (success) {
                                Toast.makeText(getActivity(), "Due date updated", Toast.LENGTH_SHORT).show();
                                loadRecentActivity();
                                checkInventoryAlerts();
                            }
                        });
                    });
                    datePicker.show(getParentFragmentManager(), "UPDATE_DATE_PICKER");
                });

                mRecentActivityContainer.addView(activityView);
                count++;
            }
            
            if (count == 0) {
                TextView emptyText = new TextView(requireContext());
                emptyText.setText("No recent activity");
                emptyText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                emptyText.setPadding(0, 32, 0, 32);
                mRecentActivityContainer.addView(emptyText);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SupplyFlow Inventory Alerts";
            String description = "Alerts for low stock, expiring items, and overdue borrows";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                checkInventoryAlerts();
            }
        } else {
            checkInventoryAlerts();
        }
    }

    private void checkInventoryAlerts() {
        if (!isAdded() || sCheckedAlertsThisSession) return;
        sCheckedAlertsThisSession = true;

        SupplyLab lab = SupplyLab.get(requireActivity());
        long currentTime = System.currentTimeMillis();

        // 1. Check for Overdue
        lab.getActiveBorrowRecordsAsync(borrows -> {
            if (!isAdded()) return;
            List<BorrowRecord> overdueRecords = borrows.stream()
                    .filter(r -> r.getExpectedReturnDate() != null && r.getExpectedReturnDate().getTime() < currentTime)
                    .collect(Collectors.toList());
            if (!overdueRecords.isEmpty()) {
                sendLocalNotification(NOTIFY_OVERDUE, "Overdue Items Alert", "There are " + overdueRecords.size() + " overdue items.");
            }
        });

        // 2. Check for Low Stock & Expiring
        lab.getItemsAsync(items -> {
            if (!isAdded()) return;
            
            List<SupplyItem> lowStock = items.stream().filter(i -> i.getQuantity() > 0 && i.getQuantity() <= 5).collect(Collectors.toList());
            if (!lowStock.isEmpty()) {
                sendLocalNotification(NOTIFY_LOW_STOCK, "Low Stock Warning", lowStock.size() + " items are running low.");
            }

            long thirtyDays = 30L * 24 * 60 * 60 * 1000;
            List<SupplyItem> expiring = items.stream().filter(i -> i.getExpirationDate() != null && 
                i.getExpirationDate().getTime() > currentTime && i.getExpirationDate().getTime() < (currentTime + thirtyDays)).collect(Collectors.toList());
            if (!expiring.isEmpty()) {
                sendLocalNotification(NOTIFY_EXPIRING, "Expiring Supplies", expiring.size() + " items will expire soon.");
            }
        });

        // 3. Check for Pending Requests
        lab.getPendingRequestsAsync(requests -> {
            if (!isAdded()) return;
            if (!requests.isEmpty()) {
                sendLocalNotification(NOTIFY_PENDING_REQUESTS, "Pending Requests", requests.size() + " requests need approval.");
            }
        });
    }

    private void sendLocalNotification(int id, String title, String text) {
        Intent intent = new Intent(getActivity(), InventoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), id, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#673AB7"));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        try {
            notificationManager.notify(id, builder.build());
        } catch (SecurityException e) {
            // Permission not granted
        }
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
        checkAndRequestNotificationPermission();
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
                
                lab.getReturnedCountAsync(count -> {
                    mReturnedCountTextView.setText(String.valueOf(count));
                });
            });
        });
    }
}
