package com.example.schoolsupplyinventory;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class InventoryActivity extends AppCompatActivity {

    private Button mAvailableButton;
    private Button mBorrowedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This line connects the Java code to the XML layout
        setContentView(R.layout.activity_main);

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
}
