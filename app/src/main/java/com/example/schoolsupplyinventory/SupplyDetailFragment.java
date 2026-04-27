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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class SupplyDetailFragment extends Fragment {

    private static final String ARG_ITEM_ID = "item_id";
    private static final int REQUEST_ID_SCAN = 2;
    private static final int REQUEST_PHOTO = 3;
    private static final int PERMISSION_REQUEST_CAMERA = 101;

    private SupplyItem mItem;
    private File mPhotoFile;
    private EditText mTitleField;
    private EditText mBrandField;
    private Spinner mCategorySpinner;
    private Button mDateButton;
    private CheckBox mBorrowedCheckBox;
    private TextView mBorrowerDisplayTextView;
    private Button mScanIdButton;
    private Button mReportButton;
    private Button mPhotoButton;
    private ImageView mPhotoView;

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
        UUID itemId = (UUID) getArguments().getSerializable(ARG_ITEM_ID);
        mItem = SupplyLab.get(getActivity()).getItem(itemId);
        mPhotoFile = SupplyLab.get(getActivity()).getPhotoFile(mItem);
    }

    @Override
    public void onPause() {
        super.onPause();
        SupplyLab.get(getActivity()).updateSupply(mItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_supply_detail, container, false);

        mTitleField = (EditText) v.findViewById(R.id.supply_title);
        mTitleField.setText(mItem.getName());
        mTitleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mBrandField = (EditText) v.findViewById(R.id.supply_brand);
        mBrandField.setText(mItem.getBrand());
        mBrandField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setBrand(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mCategorySpinner = (Spinner) v.findViewById(R.id.supply_category);
        mCategorySpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, Category.values()));
        mCategorySpinner.setSelection(mItem.getCategory().ordinal());
        mCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mItem.setCategory(Category.values()[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mDateButton = (Button) v.findViewById(R.id.supply_date);
        mDateButton.setText(mItem.getDate().toString());
        mDateButton.setEnabled(false);

        mBorrowedCheckBox = (CheckBox) v.findViewById(R.id.supply_borrowed);
        mBorrowedCheckBox.setChecked(mItem.isBorrowed());
        mBorrowedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mItem.setBorrowed(isChecked);
            }
        });

        mBorrowerDisplayTextView = (TextView) v.findViewById(R.id.supply_borrower_display);
        updateBorrowerDisplay();

        mScanIdButton = (Button) v.findViewById(R.id.supply_scan_id);
        mScanIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ScannerActivity.class);
                startActivityForResult(intent, REQUEST_ID_SCAN);
            }
        });

        mReportButton = (Button) v.findViewById(R.id.supply_report);
        mReportButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, getSupplyReport());
                i.putExtra(Intent.EXTRA_SUBJECT, "School Supply Status Report");
                i = Intent.createChooser(i, "Send report via:");
                startActivity(i);
            }
        });

        mPhotoButton = (Button) v.findViewById(R.id.supply_camera);
        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                } else {
                    launchCamera();
                }
            }
        });

        mPhotoView = (ImageView) v.findViewById(R.id.supply_photo);
        updatePhotoView();

        return v;
    }

    private void launchCamera() {
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(getActivity(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateBorrowerDisplay() {
        if (mItem.getBorrower() != null) {
            mBorrowerDisplayTextView.setText(mItem.getBorrower());
        } else {
            mBorrowerDisplayTextView.setText("None");
        }
    }

    private String getSupplyReport() {
        String borrowedString = null;
        if (mItem.isBorrowed()) {
            borrowedString = "Status: Borrowed by " + mItem.getBorrower();
        } else {
            borrowedString = "Status: In Stock";
        }

        String report = "Item: " + mItem.getName() + " (Brand: " + mItem.getBrand() + ")\n" +
                borrowedString;

        return report;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_ID_SCAN && data != null) {
            String scannedId = data.getStringExtra("SCANNED_BARCODE");
            String name = SupplyLab.get(getActivity()).findNameByBarcode(scannedId);
            
            if (name != null) {
                mItem.setBorrower(name);
                Toast.makeText(getActivity(), "Found user: " + name, Toast.LENGTH_SHORT).show();
            } else {
                mItem.setBorrower("ID: " + scannedId);
                Toast.makeText(getActivity(), "ID scanned: " + scannedId, Toast.LENGTH_SHORT).show();
            }
            updateBorrowerDisplay();
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.example.schoolsupplyinventory.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            updatePhotoView();
        }
    }
}
