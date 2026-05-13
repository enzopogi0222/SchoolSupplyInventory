package com.example.schoolsupplyinventory;

import android.app.Activity;
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

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SupplyDetailFragment extends Fragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final int REQUEST_ID_SCAN = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final String ADD_NEW_OPTION = "+ ADD NEW";

    private SupplyItem mItem;
    private File mPhotoFile;
    private boolean mIsNewItem = false;
    
    private TextInputEditText mTitleField;
    private TextInputEditText mBrandField;
    private TextInputEditText mPropertyTagField;
    private MaterialAutoCompleteTextView mCategoryDropdown;
    private MaterialAutoCompleteTextView mRoomDropdown;
    private TextInputEditText mQuantityField;
    private TextInputEditText mLocationField;
    private MaterialButton mDateButton;
    private MaterialSwitch mBorrowedSwitch;
    private TextView mBorrowerDisplayTextView;
    private MaterialButton mScanIdButton;
    private MaterialButton mReportButton;
    private FloatingActionButton mPhotoButton;
    private ImageView mPhotoView;
    private TextView mLastUpdatedTextView;

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
        
        UUID itemId = null;
        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            itemId = (UUID) getArguments().getSerializable(ARG_ITEM_ID);
        }

        if (itemId != null) {
            mItem = SupplyLab.get(getActivity()).getItem(itemId);
            mIsNewItem = false;
        } else {
            mItem = new SupplyItem();
            mIsNewItem = true;
        }
        
        if (mItem != null) {
            mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mItem != null && !mIsNewItem) {
            SupplyLab.get(getActivity()).updateSupply(mItem);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mItem == null) {
            Toast.makeText(getActivity(), "Item not found", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return new View(getActivity());
        }

        View v = inflater.inflate(R.layout.fragment_supply_detail, container, false);

        View rootLayout = v.findViewById(R.id.supply_detail_root);
        if (rootLayout != null) {
            rootLayout.startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }

        mTitleField = v.findViewById(R.id.supply_title);
        mTitleField.setText(mItem.getName());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setName(s.toString());
                updateLastUpdated();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mPropertyTagField = v.findViewById(R.id.supply_property_tag);
        mPropertyTagField.setText(mItem.getPropertyTag());
        mPropertyTagField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setPropertyTag(s.toString());
                updateLastUpdated();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mBrandField = v.findViewById(R.id.supply_brand);
        mBrandField.setText(mItem.getBrand());
        mBrandField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setBrand(s.toString());
                updateLastUpdated();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mCategoryDropdown = v.findViewById(R.id.supply_category);
        updateCategoryList();

        mRoomDropdown = v.findViewById(R.id.supply_room);
        updateRoomList();

        mQuantityField = v.findViewById(R.id.supply_quantity);
        mQuantityField.setText(String.valueOf(mItem.getQuantity()));
        mQuantityField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mItem.setQuantity(Integer.parseInt(s.toString()));
                } catch (NumberFormatException e) {
                    mItem.setQuantity(0);
                }
                updateLastUpdated();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mLocationField = v.findViewById(R.id.supply_location);
        mLocationField.setText(mItem.getLocation());
        mLocationField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setLocation(s.toString());
                updateLastUpdated();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mDateButton = v.findViewById(R.id.supply_date);
        updateDate();
        mDateButton.setOnClickListener(view -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Registered Date")
                    .setSelection(mItem.getDate().getTime())
                    .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
                    .build();
            
            datePicker.addOnPositiveButtonClickListener(selection -> {
                mItem.setDate(new Date(selection));
                updateDate();
                updateLastUpdated();
            });
            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        mBorrowedSwitch = v.findViewById(R.id.supply_borrowed);
        mBorrowedSwitch.setChecked(mItem.isBorrowed());
        mBorrowedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mItem.setBorrowed(isChecked);
            if (!isChecked) {
                mItem.setBorrower(null);
                updateBorrowerDisplay();
            }
            updateLastUpdated();
        });

        mBorrowerDisplayTextView = v.findViewById(R.id.supply_borrower_display);
        updateBorrowerDisplay();

        mScanIdButton = v.findViewById(R.id.supply_scan_id);
        mScanIdButton.setOnClickListener(v1 -> {
            Intent intent = new Intent(getActivity(), ScannerActivity.class);
            startActivityForResult(intent, REQUEST_ID_SCAN);
        });

        mReportButton = v.findViewById(R.id.supply_report);
        if (mIsNewItem) {
            mReportButton.setVisibility(View.GONE);
        }
        mReportButton.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, getSupplyReport());
            i.putExtra(Intent.EXTRA_SUBJECT, "Asset Tracking Report: " + mItem.getName());
            i = Intent.createChooser(i, "Send report via:");
            startActivity(i);
        });

        mPhotoButton = v.findViewById(R.id.supply_camera);
        mPhotoView = v.findViewById(R.id.supply_photo);

        mPhotoButton.setOnClickListener(v1 -> {
            final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.example.schoolsupplyinventory.fileprovider",
                    mPhotoFile);
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            List<ResolveInfo> cameraActivities = getActivity()
                    .getPackageManager().queryIntentActivities(captureImage,
                            PackageManager.MATCH_DEFAULT_ONLY);

            for (ResolveInfo activity : cameraActivities) {
                getActivity().grantUriPermission(activity.activityInfo.packageName,
                        uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            startActivityForResult(captureImage, REQUEST_PHOTO);
        });

        updatePhotoView();

        mLastUpdatedTextView = v.findViewById(R.id.last_updated_status);
        updateLastUpdated();

        return v;
    }

    private void updateCategoryList() {
        SupplyLab.get(getActivity()).getCategoriesAsync(categories -> {
            if (!isAdded()) return;
            
            List<String> list = new ArrayList<>(categories);
            list.add(ADD_NEW_OPTION);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, list);
            mCategoryDropdown.setAdapter(adapter);
            mCategoryDropdown.setText(mItem.getCategory(), false);
            
            mCategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selection = adapter.getItem(position);
                if (ADD_NEW_OPTION.equals(selection)) {
                    showAddOptionDialog("Category", (newOption) -> {
                        SupplyLab.get(getActivity()).addCategoryAsync(newOption, success -> {
                            if (success) {
                                mItem.setCategory(newOption);
                                updateCategoryList();
                            } else {
                                Toast.makeText(getActivity(), "Category already exists", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    mItem.setCategory(selection);
                    updateLastUpdated();
                }
            });
        });
    }

    private void updateRoomList() {
        SupplyLab.get(getActivity()).getRoomsAsync(rooms -> {
            if (!isAdded()) return;
            
            List<String> list = new ArrayList<>(rooms);
            list.add(ADD_NEW_OPTION);
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, list);
            mRoomDropdown.setAdapter(adapter);
            mRoomDropdown.setText(mItem.getRoom(), false);
            
            mRoomDropdown.setOnItemClickListener((parent, view, position, id) -> {
                String selection = adapter.getItem(position);
                if (ADD_NEW_OPTION.equals(selection)) {
                    showAddOptionDialog("Room/Classroom", (newOption) -> {
                        SupplyLab.get(getActivity()).addRoomAsync(newOption, success -> {
                            if (success) {
                                mItem.setRoom(newOption);
                                updateRoomList();
                            } else {
                                Toast.makeText(getActivity(), "Room already exists", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                } else {
                    mItem.setRoom(selection);
                    updateLastUpdated();
                }
            });
        });
    }

    private interface OnOptionAdded {
        void onAdded(String option);
    }

    private void showAddOptionDialog(String title, OnOptionAdded callback) {
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint(title + " Name");
        input.setSingleLine(true);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New " + title)
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newOption = input.getText().toString().trim().toUpperCase();
                    if (!newOption.isEmpty()) {
                        callback.onAdded(newOption);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (title.equals("Category")) {
                        mCategoryDropdown.setText(mItem.getCategory(), false);
                    } else {
                        mRoomDropdown.setText(mItem.getRoom(), false);
                    }
                })
                .show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_detail, menu);
        
        MenuItem deleteItem = menu.findItem(R.id.delete_supply);
        MenuItem saveItem = menu.findItem(R.id.save_supply);
        
        if (mIsNewItem) {
            if (deleteItem != null) deleteItem.setVisible(false);
            if (saveItem != null) saveItem.setVisible(true);
        } else {
            if (deleteItem != null) deleteItem.setVisible(true);
            if (saveItem != null) saveItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_supply) {
            confirmDelete();
            return true;
        } else if (id == R.id.save_supply) {
            saveNewItem();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNewItem() {
        if (mItem.getName() == null || mItem.getName().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Please enter an item name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SupplyLab.get(getActivity()).addSupply(mItem, result -> {
            Toast.makeText(getActivity(), "Asset saved successfully", Toast.LENGTH_SHORT).show();
            mIsNewItem = false;
            getActivity().invalidateOptionsMenu();
            getActivity().finish();
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(getActivity(), R.style.Base_Theme_SchoolSupplyInventory)
                .setTitle("Delete Asset")
                .setMessage("Are you sure you want to delete this asset?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SupplyLab.get(getActivity()).deleteSupply(mItem);
                    mItem = null;
                    getActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        mDateButton.setText("Registered: " + dateFormat.format(mItem.getDate()));
    }

    private void updateBorrowerDisplay() {
        if (mItem.getBorrower() != null && !mItem.getBorrower().isEmpty()) {
            mBorrowerDisplayTextView.setText(mItem.getBorrower());
            mBorrowerDisplayTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            mBorrowerDisplayTextView.setText("In Storage / Assigned");
            mBorrowerDisplayTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void updatePhotoView() {
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            mPhotoView.setImageDrawable(null);
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(
                    mPhotoFile.getPath(), getActivity());
            mPhotoView.setImageBitmap(bitmap);
        }
    }

    private void updateLastUpdated() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        mLastUpdatedTextView.setText("Last change synced: " + time);
    }

    private String getSupplyReport() {
        StringBuilder report = new StringBuilder();
        report.append("ASSET REPORT\n");
        report.append("----------\n");
        report.append("Item: ").append(mItem.getName()).append("\n");
        report.append("Tag/Serial: ").append(mItem.getPropertyTag()).append("\n");
        report.append("Brand: ").append(mItem.getBrand()).append("\n");
        report.append("Assigned Room: ").append(mItem.getRoom()).append("\n");
        report.append("Status: ").append(mItem.isBorrowed() ? "Borrowed by " + mItem.getBorrower() : "Assigned/Available").append("\n");
        report.append("Location: ").append(mItem.getLocation());
        return report.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_ID_SCAN && data != null) {
            String scannedId = data.getStringExtra("SCANNED_BARCODE");
            String name = SupplyLab.get(getActivity()).findNameByBarcode(scannedId);

            mItem.setBorrowed(true);
            mBorrowedSwitch.setChecked(true);

            if (name != null) {
                mItem.setBorrower(name);
            } else {
                mItem.setBorrower(scannedId);
            }
            updateBorrowerDisplay();
            updateLastUpdated();
        } else if (requestCode == REQUEST_PHOTO) {
            updatePhotoView();
            updateLastUpdated();
        }
    }
}
