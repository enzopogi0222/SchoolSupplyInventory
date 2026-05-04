package com.example.schoolsupplyinventory;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

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
    private EditText mTitleField;
    private EditText mBrandField;
    private EditText mRoomField;
    private Spinner mCategorySpinner;
    private Button mDateButton;
    private TextView mBorrowerDisplayTextView;
    private Button mScanIdButton;
    private Button mDeleteButton;
    private ImageView mPhotoView;
    private Button mPhotoButton;
    
    private Button mReturnButton;
    private TextView mLastUpdatedTextView;
    private TextView mOverdueTextView;

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
        if (mItem != null) {
            SupplyLab.get(getActivity()).updateSupply(mItem);
        }
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
                updateLastUpdated();
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
                updateLastUpdated();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        mRoomField = (EditText) v.findViewById(R.id.supply_room);
        mRoomField.setText(mItem.getRoom());
        mRoomField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mItem.setRoom(s.toString());
                updateLastUpdated();
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
                updateLastUpdated();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mDateButton = (Button) v.findViewById(R.id.supply_date);
        updateDate();
        mDateButton.setEnabled(false);

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

        mDeleteButton = (Button) v.findViewById(R.id.supply_delete);
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SupplyLab.get(getActivity()).deleteSupply(mItem);
                mItem = null; // Prevent updateSupply in onPause
                getActivity().finish();
            }
        });

        mPhotoButton = (Button) v.findViewById(R.id.supply_camera);
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        PackageManager packageManager = getActivity().getPackageManager();
        boolean canTakePhoto = mPhotoFile != null &&
                captureImage.resolveActivity(packageManager) != null;
        mPhotoButton.setEnabled(canTakePhoto);

        mPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        });

        mPhotoView = (ImageView) v.findViewById(R.id.supply_photo);
        updatePhotoView();

        mReturnButton = (Button) v.findViewById(R.id.supply_return);
        updateReturnButtonVisibility();
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItem.setBorrowed(false);
                mItem.setBorrower(null);
                updateBorrowerDisplay();
                updateReturnButtonVisibility();
                updateLastUpdated();
            }
        });

        mLastUpdatedTextView = (TextView) v.findViewById(R.id.last_updated_status);
        
        return v;
    }

    private void updateDate() {
        mDateButton.setText(mItem.getDate().toString());
        checkOverdue();
    }

    private void checkOverdue() {
        if (mItem.isBorrowed()) {
            long diff = new Date().getTime() - mItem.getDate().getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if (days >= 1) {
                mDateButton.setTextColor(Color.RED);
                Toast.makeText(getActivity(), "Warning: Item overdue by " + days + " day(s)!", Toast.LENGTH_LONG).show();
            } else {
                mDateButton.setTextColor(Color.BLACK);
            }
        } else {
            mDateButton.setTextColor(Color.BLACK);
        }
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

    private void updateReturnButtonVisibility() {
        if (mItem.isBorrowed()) {
            mReturnButton.setVisibility(View.VISIBLE);
        } else {
            mReturnButton.setVisibility(View.GONE);
        }
    }

    private void updateLastUpdated() {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        mLastUpdatedTextView.setText(getString(R.string.last_updated_label, time));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_ID_SCAN && data != null) {
            String scannedId = data.getStringExtra("SCANNED_BARCODE");
            String name = SupplyLab.get(getActivity()).findNameByBarcode(scannedId);
            
            // Mark as borrowed when ID is scanned
            mItem.setBorrowed(true);
            mItem.setDate(new Date()); // Record borrow time

            if (name != null) {
                mItem.setBorrower(name);
                Toast.makeText(getActivity(), "Found user: " + name, Toast.LENGTH_SHORT).show();
            } else {
                mItem.setBorrower("ID: " + scannedId);
                Toast.makeText(getActivity(), "ID scanned: " + scannedId, Toast.LENGTH_SHORT).show();
            }
            updateDate();
            updateBorrowerDisplay();
            updateReturnButtonVisibility();
            updateLastUpdated();
        } else if (requestCode == REQUEST_PHOTO) {
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.example.schoolsupplyinventory.fileprovider",
                    mPhotoFile);

            getActivity().revokeUriPermission(uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            updatePhotoView();
            updateLastUpdated();
        }
    }
}
