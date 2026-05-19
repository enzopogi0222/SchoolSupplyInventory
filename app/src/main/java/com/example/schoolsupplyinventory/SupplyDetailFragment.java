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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final int REQUEST_GALLERY = 6;
    private static final String ADD_NEW_OPTION = "+ ADD NEW";

    private SupplyItem mItem;
    private File mPhotoFile;
    private boolean mIsNewItem = false;
    private boolean mIsAdmin = true;
    
    private TextInputEditText mTitleField;
    private TextInputEditText mQuantityField;
    private TextInputEditText mBarcodeField;
    private TextInputEditText mDescriptionField;
    private TextInputEditText mUnitPriceField;
    private TextInputEditText mTotalValueField;
    private TextInputEditText mReorderLevelField;
    private TextInputEditText mSupplierField;
    private TextInputEditText mRemarksField;
    private TextInputEditText mProductIdField;
    
    private MaterialAutoCompleteTextView mUnitDropdown;
    private MaterialAutoCompleteTextView mCategoryDropdown;
    private MaterialAutoCompleteTextView mStatusDropdown;
    private MaterialAutoCompleteTextView mRoomDropdown;
    
    private MaterialButton mDateButton;
    private MaterialButton mExpirationDateButton;
    private FloatingActionButton mPhotoButton;
    private ExtendedFloatingActionButton mSaveFab;
    private ImageView mPhotoView;
    private View mLoadingOverlay;
    
    private TextInputLayout mBarcodeLayout;

    // Staff action views
    private View mStaffActionsCard;
    private MaterialButton mRequestBorrowBtn;
    private MaterialButton mRequestConsumeBtn;
    private View mStockPricingCard;
    private View mLogisticsCard;
    private View mAdvancedCard;

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
        
        mIsAdmin = "ADMIN".equals(getActivity().getSharedPreferences("InventoSchool", Context.MODE_PRIVATE).getString("USER_ROLE", "ADMIN"));
        
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
        
        mProductIdField = v.findViewById(R.id.supply_item_id);
        mTitleField = v.findViewById(R.id.supply_title);
        mCategoryDropdown = v.findViewById(R.id.supply_category);
        mDescriptionField = v.findViewById(R.id.supply_description);
        
        mQuantityField = v.findViewById(R.id.supply_quantity);
        mUnitDropdown = v.findViewById(R.id.supply_unit);
        mUnitPriceField = v.findViewById(R.id.supply_unit_price);
        mTotalValueField = v.findViewById(R.id.supply_total_value);
        mReorderLevelField = v.findViewById(R.id.supply_reorder_level);
        
        mSupplierField = v.findViewById(R.id.supply_supplier);
        mRoomDropdown = v.findViewById(R.id.supply_room);
        mStatusDropdown = v.findViewById(R.id.supply_status);
        mExpirationDateButton = v.findViewById(R.id.supply_expiration_date);
        
        mBarcodeLayout = v.findViewById(R.id.supply_barcode_layout);
        mBarcodeField = v.findViewById(R.id.supply_barcode);
        mRemarksField = v.findViewById(R.id.supply_remarks);
        
        mDateButton = v.findViewById(R.id.supply_date);
        mPhotoButton = v.findViewById(R.id.supply_camera);
        mPhotoView = v.findViewById(R.id.supply_photo);
        mSaveFab = v.findViewById(R.id.save_supply_fab);

        // Staff Action views
        mStaffActionsCard = v.findViewById(R.id.card_staff_actions);
        mRequestBorrowBtn = v.findViewById(R.id.btn_request_borrow);
        mRequestConsumeBtn = v.findViewById(R.id.btn_request_consume);
        mStockPricingCard = v.findViewById(R.id.card_stock_pricing);
        mLogisticsCard = v.findViewById(R.id.card_logistics);
        mAdvancedCard = v.findViewById(R.id.card_advanced);

        setupListeners();
        updateFields();

        if (!mIsAdmin) {
            mSaveFab.setVisibility(View.GONE);
            mPhotoButton.setVisibility(View.GONE);
            mBarcodeLayout.setEndIconVisible(false);
            
            // Hide sensitive/unnecessary info for staff
            mStockPricingCard.setVisibility(View.GONE);
            mLogisticsCard.setVisibility(View.GONE);
            mAdvancedCard.setVisibility(View.GONE);
            mDateButton.setVisibility(View.GONE);
            
            // Disable remaining visible fields
            mProductIdField.setEnabled(false);
            mTitleField.setEnabled(false);
            mCategoryDropdown.setEnabled(false);
            mDescriptionField.setEnabled(false);

            // Show staff actions
            mStaffActionsCard.setVisibility(View.VISIBLE);
        } else {
            mStaffActionsCard.setVisibility(View.GONE);
        }

        View rootLayout = v.findViewById(R.id.supply_detail_root);
        if (rootLayout != null) {
            rootLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }

        return v;
    }

    private void setupListeners() {
        mProductIdField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setPropertyTag(s)));
        mTitleField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setName(s)));
        mDescriptionField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setDescription(s)));
        
        mQuantityField.addTextChangedListener(createSimpleTextWatcher(s -> {
            try { 
                int val = Integer.parseInt(s);
                mItem.setTotalQuantity(val);
                mItem.setAvailableQuantity(val); // Simplification for demo
                updateTotalValue();
            } catch (Exception e) { }
        }));
        
        mUnitPriceField.addTextChangedListener(createSimpleTextWatcher(s -> {
            try {
                double val = Double.parseDouble(s);
                mItem.setUnitPrice(val);
                updateTotalValue();
            } catch (Exception e) { }
        }));
        
        mReorderLevelField.addTextChangedListener(createSimpleTextWatcher(s -> {
            try {
                mItem.setReorderLevel(Integer.parseInt(s));
            } catch (Exception e) { }
        }));

        mSupplierField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setSupplier(s)));
        mBarcodeField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setBarcode(s)));
        mRemarksField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setRemarks(s)));
        
        mBarcodeLayout.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            startActivityForResult(intent, REQUEST_BARCODE_SCAN);
        });

        setupStaticDropdown(mStatusDropdown, new String[]{"Available", "Low Stock", "Out of Stock", "Damaged"}, mItem.getStatus(), s -> mItem.setStatus(s));

        mDateButton.setOnClickListener(view -> showDatePicker("Date Added", mItem.getDateAdded(), date -> {
            mItem.setDateAdded(date);
            updateDateButton();
        }));

        mExpirationDateButton.setOnClickListener(v -> {
            Date initial = mItem.getExpirationDate() != null ? mItem.getExpirationDate() : new Date();
            showDatePicker("Expiration Date", initial, date -> {
                mItem.setExpirationDate(date);
                updateExpirationDateButton();
            });
        });

        mPhotoButton.setOnClickListener(v1 -> {
            String[] options = {"Take Photo", "Choose from Gallery"};
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add Photo")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                            } else {
                                launchCamera();
                            }
                        } else {
                            launchGallery();
                        }
                    })
                    .show();
        });

        mSaveFab.setOnClickListener(view -> saveItem());

        mRequestBorrowBtn.setOnClickListener(v -> showRequestDialog(SupplyRequest.TYPE_BORROW));
        mRequestConsumeBtn.setOnClickListener(v -> showRequestDialog(SupplyRequest.TYPE_CONSUME));
    }

    private void showRequestDialog(String type) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_request_item, null);
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.request_dialog_title);
        title.setText(SupplyRequest.TYPE_BORROW.equals(type) ? "Request to Borrow" : "Request to Use");

        TextInputEditText qtyEdit = view.findViewById(R.id.request_qty_edit);
        TextInputEditText purposeEdit = view.findViewById(R.id.request_purpose_edit);
        TextInputLayout returnDateLayout = view.findViewById(R.id.request_return_date_layout);
        TextInputEditText returnDateEdit = view.findViewById(R.id.request_return_date_edit);
        MaterialButton submitBtn = view.findViewById(R.id.btn_submit_request);

        final Calendar expectedReturn = Calendar.getInstance();
        expectedReturn.add(Calendar.DAY_OF_YEAR, 7);
        
        if (SupplyRequest.TYPE_BORROW.equals(type)) {
            returnDateLayout.setVisibility(View.VISIBLE);
            returnDateEdit.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(expectedReturn.getTime()));
            returnDateEdit.setOnClickListener(v -> {
                MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                        .setSelection(expectedReturn.getTimeInMillis())
                        .build();
                picker.addOnPositiveButtonClickListener(selection -> {
                    expectedReturn.setTimeInMillis(selection);
                    returnDateEdit.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(expectedReturn.getTime()));
                });
                picker.show(getParentFragmentManager(), "DATE_PICKER");
            });
        } else {
            returnDateLayout.setVisibility(View.GONE);
        }

        submitBtn.setOnClickListener(v -> {
            String qtyStr = qtyEdit.getText().toString().trim();
            String purpose = purposeEdit.getText().toString().trim();

            if (qtyStr.isEmpty() || purpose.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int qty = Integer.parseInt(qtyStr);
            if (qty <= 0 || qty > mItem.getAvailableQuantity()) {
                Toast.makeText(getActivity(), "Invalid quantity. Max available: " + mItem.getAvailableQuantity(), Toast.LENGTH_SHORT).show();
                return;
            }

            SupplyRequest request = new SupplyRequest();
            request.setItemId(mItem.getId());
            request.setItemTitle(mItem.getName());
            request.setRequesterName(SupplyLab.get(getActivity()).getCurrentUser());
            request.setQuantity(qty);
            request.setRequestType(type);
            request.setPurpose(purpose);
            if (SupplyRequest.TYPE_BORROW.equals(type)) {
                request.setExpectedReturnDate(expectedReturn.getTime());
            }

            SupplyLab.get(getActivity()).submitSupplyRequestAsync(request, success -> {
                if (success) {
                    Toast.makeText(getActivity(), "Request submitted successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(), "Failed to submit request", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void updateFields() {
        if (mItem == null) return;
        
        mProductIdField.setText(mItem.getPropertyTag());
        mTitleField.setText(mItem.getName());
        mDescriptionField.setText(mItem.getDescription());
        
        mQuantityField.setText(String.valueOf(mItem.getTotalQuantity()));
        mUnitPriceField.setText(String.valueOf(mItem.getUnitPrice()));
        mReorderLevelField.setText(String.valueOf(mItem.getReorderLevel()));
        
        mSupplierField.setText(mItem.getSupplier());
        mBarcodeField.setText(mItem.getBarcode());
        mRemarksField.setText(mItem.getRemarks());
        
        mStatusDropdown.setText(mItem.getStatus(), false);
        
        updateCategoryList();
        updateUnitList();
        updateRoomList();
        updateDateButton();
        updateExpirationDateButton();
        updatePhotoView();
        updateTotalValue();

        if (!mIsAdmin) {
            if (SupplyItem.TYPE_BORROWABLE.equalsIgnoreCase(mItem.getItemType())) {
                mRequestBorrowBtn.setVisibility(View.VISIBLE);
                mRequestConsumeBtn.setVisibility(View.GONE);
            } else {
                mRequestBorrowBtn.setVisibility(View.GONE);
                mRequestConsumeBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateTotalValue() {
        if (mTotalValueField != null) {
            double total = mItem.getTotalQuantity() * mItem.getUnitPrice();
            mTotalValueField.setText(String.format(Locale.getDefault(), "%.2f", total));
        }
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
        Uri uri = FileProvider.getUriForFile(getActivity(),
                "com.example.schoolsupplyinventory.fileprovider", mPhotoFile);
        captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        captureImage.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(captureImage, REQUEST_PHOTO);
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void copyUriToFile(Uri uri, File destFile) throws IOException {
        try (InputStream in = getActivity().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
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
        MenuItem delete = menu.findItem(R.id.delete_supply);
        if (delete != null) {
            delete.setVisible(mIsAdmin && !mIsNewItem);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_supply) { confirmDelete(); return true; }
        return super.onOptionsItemSelected(item);
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

    private void updateExpirationDateButton() {
        if (mItem.getExpirationDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            mExpirationDateButton.setText("Expires: " + dateFormat.format(mItem.getExpirationDate()));
        } else {
            mExpirationDateButton.setText("No Expiration Date");
        }
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
            updatePhotoView();
        } else if (requestCode == REQUEST_GALLERY && data != null) {
            Uri selectedImage = data.getData();
            try {
                copyUriToFile(selectedImage, mPhotoFile);
                updatePhotoView();
            } catch (IOException e) {
                Toast.makeText(getActivity(), "Failed to copy image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BARCODE_SCAN && data != null) {
            String barcode = data.getStringExtra("SCANNED_BARCODE");
            mBarcodeField.setText(barcode);
            mItem.setBarcode(barcode);
        }
    }
}
