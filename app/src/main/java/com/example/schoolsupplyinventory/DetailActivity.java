package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    private Button mMarkBorrowedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Retrieve the Data
        String itemName = getIntent().getStringExtra(InventoryActivity.EXTRA_ITEM_NAME);
        TextView textView = findViewById(R.id.detail_text_view);
        if (itemName != null) {
            textView.setText(itemName);
        }

        mMarkBorrowedButton = findViewById(R.id.mark_borrowed_button);
        mMarkBorrowedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the Result
                Intent data = new Intent();
                data.putExtra(InventoryActivity.EXTRA_IS_BORROWED, true);
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }
}
