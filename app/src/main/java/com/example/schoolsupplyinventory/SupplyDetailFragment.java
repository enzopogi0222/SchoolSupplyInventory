package com.example.schoolsupplyinventory;

import android.app.Activity;
import android.content.DialogInterface;
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
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SupplyDetailFragment extends Fragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final int REQUEST_ID_SCAN = 2;
    private static final int REQUEST_PHOTO = 3;

    private SupplyItem mItem;
    private File mPhotoFile;
    private TextInputEditText mTitleField;
    private TextInputEditText mBrandField;
    private MaterialAutoCompleteTextView mCategoryDropdown;
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
        args.putSerializable(ARG_ITEM_ID, itemId);

        SupplyDetailFragment fragment = new SupplyDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        UUID itemId = (UUID) getArguments().getSerializable(ARG_ITEM_ID);
        mItem = SupplyLab.get(getActivity()).getItem(itemId);
        mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mItem != null) {
            SupplyLab.get(getActivity()).updateSupply(mItem);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_supply_detail, container, false);

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
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, Category.values());
        mCategoryDropdown.setAdapter(adapter);
        mCategoryDropdown.setText(mItem.getCategory().toString(), false);
        mCategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            mItem.setCategory(Category.values()[position]);
            updateLastUpdated();
        });

        mDateButton = v.findViewById(R.id.supply_date);
        updateDate();
        mDateButton.setEnabled(false);

        mBorrowedSwitch = v.findViewById(R.id.supply_borrowed);
        mBorrowedSwitch.setChecked(mItem.isBorrowed());
        mBorrowedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mItem.setBorrowed(isChecked);
            if (!isChecked) {
                mItem.setBorrower(null);
                updateBorrowerDisplay();
            }
            updateDate();
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
        mReportButton.setOnClickListener(v1 -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, getSupplyReport());
            i.putExtra(Intent.EXTRA_SUBJECT, "School Supply Status Report");
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_supply_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_supply) {
            new AlertDialog.Builder(getActivity())
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        SupplyLab.get(getActivity()).deleteSupply(mItem);
                        mItem = null;
                        getActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        mDateButton.setText("Date: " + dateFormat.format(mItem.getDate()));
    }

    private void updateBorrowerDisplay() {
        if (mItem.getBorrower() != null) {
            mBorrowerDisplayTextView.setText(mItem.getBorrower());
        } else {
            mBorrowerDisplayTextView.setText("None");
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
        mLastUpdatedTextView.setText("Last updated: " + time);
    }

    private String getSupplyReport() {
        String borrowedString = mItem.isBorrowed() ?
            "Status: Borrowed by " + mItem.getBorrower() : "Status: Available";
        return "Item: " + mItem.getName() + "\nBrand: " + mItem.getBrand() + "\n" + borrowedString;
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
