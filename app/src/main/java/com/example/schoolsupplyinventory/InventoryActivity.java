package com.example.schoolsupplyinventory;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class InventoryActivity extends AppCompatActivity {

    private Button mAvailableButton;
    private Button mBorrowedButton;
    private static final String TAG = "InventoryActivity";
    private static final String KEY_INDEX = "index";
    private int mCurrentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line connects the Java code to the XML layout
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
        }

        // Wiring the 'Available' Button
        mAvailableButton = (Button) findViewById(R.id.available_button);
        mAvailableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast
                Toast.makeText(InventoryActivity.this,
                        R.string.mark_as_available, Toast.LENGTH_SHORT).show();
            }
        });

        // Wiring the 'Borrowed' Button
        mBorrowedButton = (Button) findViewById(R.id.borrowed_button);
        mBorrowedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(InventoryActivity.this,
                        R.string.mark_as_borrowed, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
        //Save which item index the user was looking at
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
        // Logic: Save temporary data here if needed
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
