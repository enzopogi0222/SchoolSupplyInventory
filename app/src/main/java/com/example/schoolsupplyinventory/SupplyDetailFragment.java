package com.example.schoolsupplyinventory;

import android.Manifest;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SupplyDetailFragment extends Fragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final String SAVED_ITEM_ID = "saved_item_id";
    private static final int REQUEST_PHOTO = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 4;
    private static final int REQUEST_BARCODE_SCAN = 5;
    private static final String ADD_NEW_OPTION = "+ ADD NEW";

    private SupplyItem mItem;
    private File mPhotoFile;
    private boolean mIsNewItem = false;
    private boolean mIsAdmin = true;
    
    private TextInputLayout mBarcodeLayout;
    private TextInputEditText mTitleField;
    private TextInputEditText mQuantityField;
    private TextInputEditText mBarcodeField;
    private TextInputEditText mDescriptionField;
    private TextInputEditText mUnitIdentifiersField;
    private MaterialAutoCompleteTextView mUnitDropdown;
    private MaterialAutoCompleteTextView mCategoryDropdown;
    private MaterialAutoCompleteTextView mTypeDropdown;
    private MaterialAutoCompleteTextView mConditionDropdown;
    private MaterialAutoCompleteTextView mStatusDropdown;
    private MaterialAutoCompleteTextView mRoomDropdown;
    private MaterialButton mDateButton;
    private FloatingActionButton mPhotoButton;
    private ExtendedFloatingActionButton mSaveFab;
    private ImageView mPhotoView;
    private View mLoadingOverlay;
    
    private TextView mAvailableStockText, mBorrowedStockText, mUsedStockText;

    public static SupplyDetailFragment newInstance(UUID itemId) {
        Bundle args = new Bundle();
        if (itemId != null) {
            args.putSerializable(ARG_ITEM_ID, itemId);
        }

        SupplyDetailFragment fragment = new SupplyDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        
        mIsAdmin = "ADMIN".equals(getActivity().getSharedPreferences("SupplyFlow", Context.MODE_PRIVATE).getString("USER_ROLE", "ADMIN"));
        
        UUID itemId = null;
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_ITEM_ID)) {
            itemId = (UUID) savedInstanceState.getSerializable(SAVED_ITEM_ID);
        } else if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            itemId = (UUID) getArguments().getSerializable(ARG_ITEM_ID);
        }

        if (itemId != null) {
            mItem = new SupplyItem(itemId);
            loadItemAsync(itemId);
        } else {
            mItem = new SupplyItem();
            mIsNewItem = true;
            mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
        }
    }

    private void loadItemAsync(UUID itemId) {
        SupplyLab.get(getActivity()).getItemAsync(itemId, item -> {
            if (item != null) {
                mItem = item;
                mIsNewItem = false;
                mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
                if (getView() != null) {
                    updateFields();
                }
            } else {
                mIsNewItem = true;
                mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mItem != null) {
            outState.putSerializable(SAVED_ITEM_ID, mItem.getId());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_supply_detail, container, false);

        mLoadingOverlay = v.findViewById(R.id.loading_overlay);
        
        mBarcodeLayout = v.findViewById(R.id.supply_barcode_layout);
        mBarcodeField = v.findViewById(R.id.supply_barcode);
        mTitleField = v.findViewById(R.id.supply_title);
        mCategoryDropdown = v.findViewById(R.id.supply_category);
        mTypeDropdown = v.findViewById(R.id.supply_type);
        mQuantityField = v.findViewById(R.id.supply_quantity);
        mUnitDropdown = v.findViewById(R.id.supply_unit);
        mUnitIdentifiersField = v.findViewById(R.id.supply_unit_identifiers);
        mDescriptionField = v.findViewById(R.id.supply_description);
        mConditionDropdown = v.findViewById(R.id.supply_condition);
        mStatusDropdown = v.findViewById(R.id.supply_status);
        mAvailableStockText = v.findViewById(R.id.available_stock_display);
        mBorrowedStockText = v.findViewById(R.id.borrowed_stock_display);
        mUsedStockText = v.findViewById(R.id.used_stock_display);
        mRoomDropdown = v.findViewById(R.id.supply_room);
        mDateButton = v.findViewById(R.id.supply_date);
        mPhotoButton = v.findViewById(R.id.supply_camera);
        mPhotoView = v.findViewById(R.id.supply_photo);
        mSaveFab = v.findViewById(R.id.save_supply_fab);

        setupListeners();
        updateFields();

        if (!mIsAdmin) {
            mSaveFab.setVisibility(View.GONE);
            mPhotoButton.setVisibility(View.GONE);
            mBarcodeLayout.setEndIconVisible(false);
            // Disable editing for staff
            mTitleField.setEnabled(false);
            mBarcodeField.setEnabled(false);
            mQuantityField.setEnabled(false);
            mCategoryDropdown.setEnabled(false);
            mTypeDropdown.setEnabled(false);
            mUnitDropdown.setEnabled(false);
            mConditionDropdown.setEnabled(false);
            mStatusDropdown.setEnabled(false);
            mRoomDropdown.setEnabled(false);
            mDescriptionField.setEnabled(false);
            mUnitIdentifiersField.setEnabled(false);
            mDateButton.setEnabled(false);
        }

        View rootLayout = v.findViewById(R.id.supply_detail_root);
        if (rootLayout != null) {
            rootLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }

        return v;
    }

    private void setupListeners() {
        mBarcodeField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setBarcode(s)));
        
        mBarcodeLayout.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            startActivityForResult(intent, REQUEST_BARCODE_SCAN);
        });

        mTitleField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setName(s)));

        setupStaticDropdown(mTypeDropdown, new String[]{SupplyItem.TYPE_CONSUMABLE, SupplyItem.TYPE_BORROWABLE}, mItem.getItemType(), s -> mItem.setItemType(s));

        mQuantityField.addTextChangedListener(createSimpleTextWatcher(s -> {
            try { 
                int val = Integer.parseInt(s);
                if (mIsNewItem) {
                    mItem.setTotalQuantity(val);
                    mItem.setAvailableQuantity(val);
                } else {
                    int diff = val - mItem.getTotalQuantity();
                    mItem.setTotalQuantity(val);
                    mItem.setAvailableQuantity(Math.max(0, mItem.getAvailableQuantity() + diff));
                }
                updateStockDisplays();
            } catch (Exception e) { }
        }));

        mUnitIdentifiersField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setUnitIdentifiers(s)));
        mDescriptionField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setDescription(s)));

        setupStaticDropdown(mConditionDropdown, new String[]{"New", "Good", "Damaged", "Old"}, mItem.getCondition(), s -> mItem.setCondition(s));
        setupStaticDropdown(mStatusDropdown, new String[]{"Available", "Borrowed", "Used", "Out of Stock"}, mItem.getStatus(), s -> mItem.setStatus(s));

        mDateButton.setOnClickListener(view -> showDatePicker("Date Added", mItem.getDateAdded(), date -> {
            mItem.setDateAdded(date);
            updateDateButton();
        }));

        mPhotoButton.setOnClickListener(v1 -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                launchCamera();
            }
        });

        mSaveFab.setOnClickListener(view -> saveItem());
    }

    private void updateFields() {
        if (mItem == null) return;
        
        mBarcodeField.setText(mItem.getBarcode());
        mTitleField.setText(mItem.getName());
        mQuantityField.setText(String.valueOf(mItem.getTotalQuantity()));
        mUnitIdentifiersField.setText(mItem.getUnitIdentifiers());
        mDescriptionField.setText(mItem.getDescription());
        
        mTypeDropdown.setText(mItem.getItemType(), false);
        mConditionDropdown.setText(mItem.getCondition(), false);
        mStatusDropdown.setText(mItem.getStatus(), false);
        
        updateCategoryList();
        updateUnitList();
        updateRoomList();
        updateStockDisplays();
        updateDateButton();
        updatePhotoView();
    }

    private void updateStockDisplays() {
        mAvailableStockText.setText("Available: " + mItem.getAvailableQuantity());
        mBorrowedStockText.setText("Borrowed: " + mItem.getBorrowedQuantity());
        mUsedStockText.setText("Used: " + mItem.getUsedQuantity());
    }

    private void setupStaticDropdown(MaterialAutoCompleteTextView dropdown, String[] options, String currentSelection, java.util.function.Consumer<String> callback) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, options);
        dropdown.setAdapter(adapter);
        dropdown.setText(currentSelection, false);
        dropdown.setOnItemClickListener((parent, view, position, id) -> callback.accept(options[position]));
    }

    private TextWatcher createSimpleTextWatcher(java.util.function.Consumer<String> action) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                action.accept(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void showDatePicker(String title, Date initialDate, java.util.function.Consumer<Date> callback) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select " + title)
                .setSelection(initialDate.getTime())
                .build();
        
        datePicker.addOnPositiveButtonClickListener(selection -> {
            callback.accept(new Date(selection));
        });
        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void launchCamera() {
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager packageManager = getActivity().getPackageManager();
        if (captureImage.resolveActivity(packageManager) == null) {
            Toast.makeText(getActivity(), "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(getActivity(),
                "com.example.schoolsupplyinventory.fileprovider", mPhotoFile);
        captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        captureImage.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        List<ResolveInfo> cameraActivities = packageManager
                .queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo activity : cameraActivities) {
            getActivity().grantUriPermission(activity.activityInfo.packageName,
                    uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        startActivityForResult(captureImage, REQUEST_PHOTO);
    }

    private void updateCategoryList() {
        SupplyLab.get(getActivity()).getCategoriesAsync(categories -> {
            if (!isAdded()) return;
            List<String> list = new ArrayList<>(categories);
            if (mIsAdmin) list.add(ADD_NEW_OPTION);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, list);
            mCategoryDropdown.setAdapter(adapter);
            mCategoryDropdown.setText(mItem.getCategory(), false);
            mCategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selection = adapter.getItem(position);
                if (ADD_NEW_OPTION.equals(selection)) {
                    showAddOptionDialog("Category", (newOption) -> {
                        SupplyLab.get(getActivity()).addCategoryAsync(newOption, success -> {
                            if (success) { mItem.setCategory(newOption); updateCategoryList(); }
                        });
                    });
                } else { mItem.setCategory(selection); }
            });
        });
    }

    private void updateUnitList() {
        SupplyLab.get(getActivity()).getUnitsAsync(units -> {
            if (!isAdded()) return;
            List<String> list = new ArrayList<>(units);
            if (mIsAdmin) list.add(ADD_NEW_OPTION);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, list);
            mUnitDropdown.setAdapter(adapter);
            mUnitDropdown.setText(mItem.getUnit(), false);
            mUnitDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selection = adapter.getItem(position);
                if (ADD_NEW_OPTION.equals(selection)) {
                    showAddOptionDialog("Unit", (newOption) -> {
                        SupplyLab.get(getActivity()).addUnitAsync(newOption, success -> {
                            if (success) { mItem.setUnit(newOption); updateUnitList(); }
                        });
                    });
                } else { mItem.setUnit(selection); }
            });
        });
    }

    private void updateRoomList() {
        SupplyLab.get(getActivity()).getRoomsAsync(rooms -> {
            if (!isAdded()) return;
            List<String> list = new ArrayList<>(rooms);
            if (mIsAdmin) list.add(ADD_NEW_OPTION);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, list);
            mRoomDropdown.setAdapter(adapter);
            mRoomDropdown.setText(mItem.getRoom(), false);
            mRoomDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selection = adapter.getItem(position);
                if (ADD_NEW_OPTION.equals(selection)) {
                    showAddOptionDialog("Room", (newOption) -> {
                        SupplyLab.get(getActivity()).addRoomAsync(newOption, success -> {
                            if (success) { mItem.setRoom(newOption); updateRoomList(); }
                        });
                    });
                } else { mItem.setRoom(selection); }
            });
        });
    }

    private void showAddOptionDialog(String title, java.util.function.Consumer<String> callback) {
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint(title + " Name");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New " + title)
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newOption = input.getText().toString().trim().toUpperCase();
                    if (!newOption.isEmpty()) callback.accept(newOption);
                })
                .setNegativeButton("Cancel", (d, w) -> {
                    if (title.equals("Category")) mCategoryDropdown.setText(mItem.getCategory(), false);
                    else if (title.equals("Unit")) mUnitDropdown.setText(mItem.getUnit(), false);
                    else mRoomDropdown.setText(mItem.getRoom(), false);
                }).show();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem borrow = menu.findItem(R.id.action_borrow);
        MenuItem use = menu.findItem(R.id.action_use);
        MenuItem request = menu.findItem(R.id.action_request);
        MenuItem delete = menu.findItem(R.id.delete_supply);
        if (borrow != null) {
            borrow.setVisible(mIsAdmin && !mIsNewItem);
        }
        if (use != null) {
            use.setVisible(mIsAdmin && !mIsNewItem);
        }
        if (request != null) {
            request.setVisible(!mIsAdmin && !mIsNewItem);
        }
        if (delete != null) {
            delete.setVisible(mIsAdmin && !mIsNewItem);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_detail, menu);
        MenuItem deleteItem = menu.findItem(R.id.delete_supply);
        if (deleteItem != null) {
            deleteItem.setVisible(mIsAdmin && !mIsNewItem);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_supply) { confirmDelete(); return true; }
        if (id == R.id.action_request) { showStaffRequestDialog(); return true; }
        if (id == R.id.action_borrow) { showBorrowDialog(); return true; }
        if (id == R.id.action_use) { showUseDialog(); return true; }
        if (id == R.id.action_history) { showHistoryDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void showStaffRequestDialog() {
        if (mItem == null || mIsNewItem || getActivity() == null) {
            return;
        }

        View dlgView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_staff_request, null);
        TextView typeLabel = dlgView.findViewById(R.id.staff_request_type_label);
        TextInputEditText qtyEdit = dlgView.findViewById(R.id.staff_request_quantity);
        TextInputEditText purposeEdit = dlgView.findViewById(R.id.staff_request_purpose);
        TextInputLayout unitLayout = dlgView.findViewById(R.id.staff_request_unit_layout);
        TextInputEditText unitEdit = dlgView.findViewById(R.id.staff_request_unit_id);
        MaterialButton dueBtn = dlgView.findViewById(R.id.staff_request_due_date_btn);

        final boolean borrow = SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(mItem.getItemType());
        final Calendar[] dueHolder = new Calendar[]{Calendar.getInstance()};
        dueHolder[0].add(Calendar.DAY_OF_YEAR, 7);
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        if (borrow) {
            typeLabel.setText("Borrow request (returnable item)");
            dueBtn.setVisibility(View.VISIBLE);
            dueBtn.setText("Return by: " + fmt.format(dueHolder[0].getTime()));
            dueBtn.setOnClickListener(v -> {
                MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Expected return date")
                        .setSelection(dueHolder[0].getTimeInMillis())
                        .build();
                picker.addOnPositiveButtonClickListener(sel -> {
                    dueHolder[0].setTimeInMillis(sel);
                    dueBtn.setText("Return by: " + fmt.format(dueHolder[0].getTime()));
                });
                picker.show(getParentFragmentManager(), "STAFF_RETURN_DATE");
            });
            if (!mItem.getUnitIdentifiersList().isEmpty()) {
                unitLayout.setVisibility(View.VISIBLE);
            }
        } else {
            typeLabel.setText("Consume request (consumable item)");
        }
        qtyEdit.setText("1");

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Submit request")
                .setView(dlgView)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText() != null ? qtyEdit.getText().toString().trim() : "";
            String purpose = purposeEdit.getText() != null ? purposeEdit.getText().toString().trim() : "";
            if (qtyStr.isEmpty() || purpose.isEmpty()) {
                Toast.makeText(getActivity(), "Enter quantity and purpose", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException ex) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            if (qty <= 0 || qty > mItem.getAvailableQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            SupplyRequest req = new SupplyRequest();
            req.setItemId(mItem.getId());
            req.setItemTitle(mItem.getName());
            req.setRequesterName(SupplyLab.get(requireContext()).getCurrentUser());
            req.setQuantity(qty);
            req.setPurpose(purpose);
            if (borrow) {
                req.setRequestType(SupplyRequest.TYPE_BORROW);
                req.setExpectedReturnDate(dueHolder[0].getTime());
                String uid = unitEdit.getText() != null ? unitEdit.getText().toString().trim() : "";
                if (!uid.isEmpty()) {
                    req.setUnitId(uid);
                }
            } else {
                req.setRequestType(SupplyRequest.TYPE_CONSUME);
            }
            SupplyLab.get(requireContext()).submitSupplyRequestAsync(req, ok -> {
                Activity act = getActivity();
                if (act == null) {
                    return;
                }
                act.runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(act, "Request submitted (pending approval)", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(act, "Could not submit (check item type and stock)", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }));
        dialog.show();
    }

    private void showBorrowDialog() {
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_borrow, null);
        TextInputEditText borrowerField = v.findViewById(R.id.borrower_name);
        TextInputEditText qtyField = v.findViewById(R.id.borrow_quantity);
        TextInputEditText unitIdField = v.findViewById(R.id.borrow_unit_id);
        
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Borrow Item")
                .setView(v)
                .setPositiveButton("Borrow", (d, w) -> {
                    String name = borrowerField.getText().toString();
                    String qtyStr = qtyField.getText().toString();
                    String unitId = unitIdField.getText().toString();
                    if (name.isEmpty() || qtyStr.isEmpty()) return;
                    int qty = Integer.parseInt(qtyStr);
                    SupplyLab.get(getActivity()).borrowItemAsync(mItem.getId(), name, qty, System.currentTimeMillis(), System.currentTimeMillis() + 604800000L, unitId, success -> {
                        if (success) {
                            Toast.makeText(getActivity(), "Item borrowed", Toast.LENGTH_SHORT).show();
                            loadItemAsync(mItem.getId());
                        } else {
                            Toast.makeText(getActivity(), "Failed to borrow (insufficient stock)", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showUseDialog() {
        final TextInputEditText input = new TextInputEditText(getActivity());
        input.setHint("Quantity to consume");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Consume Item")
                .setView(input)
                .setPositiveButton("Use", (d, w) -> {
                    String qtyStr = input.getText().toString();
                    if (qtyStr.isEmpty()) return;
                    int qty = Integer.parseInt(qtyStr);
                    SupplyLab.get(getActivity()).useConsumableAsync(mItem.getId(), qty, success -> {
                        if (success) {
                            Toast.makeText(getActivity(), "Quantity updated", Toast.LENGTH_SHORT).show();
                            loadItemAsync(mItem.getId());
                        } else {
                            Toast.makeText(getActivity(), "Failed to consume (insufficient stock)", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHistoryDialog() {
        SupplyLab.get(getActivity()).getAllHistoryAsync(records -> {
            List<HistoryRecord> itemRecords = new ArrayList<>();
            for (HistoryRecord r : records) {
                if (r.getItemId().equals(mItem.getId())) itemRecords.add(r);
            }
            
            if (itemRecords.isEmpty()) {
                Toast.makeText(getActivity(), "No history found for this item", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] historyItems = new String[itemRecords.size()];
            SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            for (int i = 0; i < itemRecords.size(); i++) {
                HistoryRecord r = itemRecords.get(i);
                historyItems[i] = fmt.format(r.getTimestamp()) + ": " + r.getAction() + " - " + r.getDetails();
            }

            new MaterialAlertDialogBuilder(getActivity())
                    .setTitle("Item History")
                    .setItems(historyItems, null)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    private void saveItem() {
        if (mItem.getName() == null || mItem.getName().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Please enter an item name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.VISIBLE);
        
        if (mIsNewItem) {
            SupplyLab.get(getActivity()).addSupply(mItem, result -> {
                if (!isAdded()) return;
                Toast.makeText(getActivity(), "Supply saved", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            });
        } else {
            SupplyLab.get(getActivity()).updateSupply(mItem, result -> {
                if (!isAdded()) return;
                Toast.makeText(getActivity(), "Supply updated", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            });
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Delete Supply")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (mLoadingOverlay != null) mLoadingOverlay.setVisibility(View.VISIBLE);
                    SupplyLab.get(getActivity()).deleteSupply(mItem, result -> {
                        if (!isAdded()) return;
                        getActivity().finish();
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateDateButton() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        mDateButton.setText("Date Added: " + dateFormat.format(mItem.getDateAdded()));
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) return;
        if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.example.schoolsupplyinventory.fileprovider", mPhotoFile);
            getActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            updatePhotoView();
        } else if (requestCode == REQUEST_BARCODE_SCAN && data != null) {
            String barcode = data.getStringExtra("SCANNED_BARCODE");
            mBarcodeField.setText(barcode);
            mItem.setBarcode(barcode);
        }
    }
}
