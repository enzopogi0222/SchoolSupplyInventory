package com.example.schoolsupplyinventory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class InventoryActivity extends AppCompatActivity {

    private Button mAvailableButton;
    private Button mBorrowedButton;
    private Button mDetailButton;
    private TextView mItemTextView;

    private static final String TAG = "InventoryActivity";
    private static final String KEY_INDEX = "index";
    public static final String EXTRA_ITEM_NAME = "com.schoolapp.item_name";
    public static final String EXTRA_IS_BORROWED = "com.schoolapp.is_borrowed";
    private static final int REQUEST_CODE_DETAIL = 0;

    private int mCurrentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mItemTextView = findViewById(R.id.item_text_view);

        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
        }

        mAvailableButton = findViewById(R.id.available_button);
        mAvailableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(InventoryActivity.this,
                        R.string.mark_as_available, Toast.LENGTH_SHORT).show();
            }
        });

        mBorrowedButton = findViewById(R.id.borrowed_button);
        mBorrowedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(InventoryActivity.this,
                        R.string.mark_as_borrowed, Toast.LENGTH_SHORT).show();
            }
        });

        mDetailButton = findViewById(R.id.detail_button);
        mDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Explicit Intent and Extras
                String itemName = mItemTextView.getText().toString();
                Intent intent = new Intent(InventoryActivity.this, DetailActivity.class);
                intent.putExtra(EXTRA_ITEM_NAME, itemName);
                // Start for Result
                startActivityForResult(intent, REQUEST_CODE_DETAIL);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_DETAIL) {
            if (data == null) {
                return;
            }
            boolean isBorrowed = data.getBooleanExtra(EXTRA_IS_BORROWED, false);
            if (isBorrowed) {
                Toast.makeText(this, "Item marked as borrowed in details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        outState.putInt(KEY_INDEX, mCurrentIndex);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
