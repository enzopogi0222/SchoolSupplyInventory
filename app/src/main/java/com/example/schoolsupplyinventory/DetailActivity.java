package com.example.schoolsupplyinventory;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        String itemName = getIntent().getStringExtra("ItemName");
        TextView textView = findViewById(R.id.detail_text_view);
        if (itemName != null) {
            textView.setText(itemName);
        }
    }
}
