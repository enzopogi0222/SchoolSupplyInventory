package com.example.schoolsupplyinventory;

import android.Manifest;
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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
    private static final String SAVED_ITEM_ID = "saved_item_id";
    private static final int REQUEST_PHOTO = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 4;
    private static final String ADD_NEW_OPTION = "+ ADD NEW";

    private SupplyItem mItem;
    private File mPhotoFile;
    private boolean mIsNewItem = false;
    
    private TextInputEditText mTitleField;
    private TextInputEditText mQuantityField;
    private TextInputEditText mBarcodeField;
    private TextInputEditText mDescriptionField;
    private MaterialAutoCompleteTextView mUnitDropdown;
    private MaterialAutoCompleteTextView mCategoryDropdown;
    private MaterialAutoCompleteTextView mConditionDropdown;
    private MaterialAutoCompleteTextView mStatusDropdown;
    private MaterialAutoCompleteTextView mRoomDropdown;
    private MaterialButton mDateButton;
    private MaterialSwitch mBorrowableSwitch;
    private FloatingActionButton mPhotoButton;
    private ExtendedFloatingActionButton mSaveFab;
    private ImageView mPhotoView;

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
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_ITEM_ID)) {
            itemId = (UUID) savedInstanceState.getSerializable(SAVED_ITEM_ID);
        } else if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            itemId = (UUID) getArguments().getSerializable(ARG_ITEM_ID);
        }

        if (itemId != null) {
            mItem = SupplyLab.get(getActivity()).getItem(itemId);
            if (mItem == null) {
                mItem = new SupplyItem(itemId);
                mIsNewItem = true;
            } else {
                mIsNewItem = false;
            }
        } else {
            mItem = new SupplyItem();
            mIsNewItem = true;
        }
        
        if (mItem != null) {
            mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mItem != null) {
            outState.putSerializable(SAVED_ITEM_ID, mItem.getId());
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

        // --- General Information ---
        mTitleField = v.findViewById(R.id.supply_title);
        mTitleField.setText(mItem.getName());
        mTitleField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setName(s)));

        mCategoryDropdown = v.findViewById(R.id.supply_category);
        updateCategoryList();

        mQuantityField = v.findViewById(R.id.supply_quantity);
        mQuantityField.setText(String.valueOf(mItem.getQuantity()));
        mQuantityField.addTextChangedListener(createSimpleTextWatcher(s -> {
            try { mItem.setQuantity(Integer.parseInt(s)); } catch (Exception e) { mItem.setQuantity(0); }
        }));

        mUnitDropdown = v.findViewById(R.id.supply_unit);
        setupStaticDropdown(mUnitDropdown, new String[]{"Piece", "Box", "Set", "Pack", "Unit"}, mItem.getUnit(), s -> mItem.setUnit(s));

        mDescriptionField = v.findViewById(R.id.supply_description);
        mDescriptionField.setText(mItem.getDescription());
        mDescriptionField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setDescription(s)));

        // --- Condition & Status ---
        mConditionDropdown = v.findViewById(R.id.supply_condition);
        setupStaticDropdown(mConditionDropdown, new String[]{"New", "Good", "Damaged", "Old"}, mItem.getCondition(), s -> mItem.setCondition(s));

        mStatusDropdown = v.findViewById(R.id.supply_status);
        setupStaticDropdown(mStatusDropdown, new String[]{"Available", "Borrowed", "Used", "Out of Stock"}, mItem.getStatus(), s -> mItem.setStatus(s));

        // --- Tracking & Location ---
        mBarcodeField = v.findViewById(R.id.supply_barcode);
        mBarcodeField.setText(mItem.getBarcode());
        mBarcodeField.addTextChangedListener(createSimpleTextWatcher(s -> mItem.setBarcode(s)));

        mRoomDropdown = v.findViewById(R.id.supply_room);
        updateRoomList();

        // --- Dates & Options ---
        mDateButton = v.findViewById(R.id.supply_date);
        updateDateButton();
        mDateButton.setOnClickListener(view -> showDatePicker("Date Added", mItem.getDate(), date -> {
            mItem.setDate(date);
            updateDateButton();
        }));

        mBorrowableSwitch = v.findViewById(R.id.supply_borrowable);
        mBorrowableSwitch.setChecked(mItem.isBorrowable());
        mBorrowableSwitch.setOnCheckedChangeListener((b, isChecked) -> mItem.setBorrowable(isChecked));

        // --- Media ---
        mPhotoButton = v.findViewById(R.id.supply_camera);
        mPhotoView = v.findViewById(R.id.supply_photo);
        mPhotoButton.setOnClickListener(v1 -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                launchCamera();
            }
        });
        updatePhotoView();

        mSaveFab = v.findViewById(R.id.save_supply_fab);
        mSaveFab.setOnClickListener(view -> saveNewItem());

        return v;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(getActivity(), "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateCategoryList() {
        SupplyLab.get(getActivity()).getCategoriesAsync(categories -> {
            if (!isAdded()) return;
            List<String> list = new ArrayList<>(categories);
            list.add(ADD_NEW_OPTION);
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

    private void updateRoomList() {
        SupplyLab.get(getActivity()).getRoomsAsync(rooms -> {
            if (!isAdded()) return;
            List<String> list = new ArrayList<>(rooms);
            list.add(ADD_NEW_OPTION);
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
                    else mRoomDropdown.setText(mItem.getRoom(), false);
                }).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_detail, menu);
        MenuItem deleteItem = menu.findItem(R.id.delete_supply);
        if (mIsNewItem) {
            if (deleteItem != null) deleteItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.delete_supply) { confirmDelete(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void saveNewItem() {
        if (mItem.getName() == null || mItem.getName().trim().isEmpty()) {
            Toast.makeText(getActivity(), "Please enter an item name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mIsNewItem) {
            SupplyLab.get(getActivity()).addSupply(mItem, result -> {
                Toast.makeText(getActivity(), "Supply saved", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            });
        } else {
            SupplyLab.get(getActivity()).updateSupply(mItem);
            Toast.makeText(getActivity(), "Supply updated", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle("Delete Supply")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    SupplyLab.get(getActivity()).deleteSupply(mItem);
                    getActivity().finish();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateDateButton() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        mDateButton.setText("Date Added: " + dateFormat.format(mItem.getDate()));
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
        }
    }
}
