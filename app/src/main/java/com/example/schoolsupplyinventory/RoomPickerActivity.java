package com.example.schoolsupplyinventory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class RoomPickerActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ORDINAL = "com.example.schoolsupplyinventory.room_ordinal";

    public static Intent newIntent(Context packageContext) {
        return new Intent(packageContext, RoomPickerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_picker);

        ListView listView = findViewById(R.id.room_list_view);
        ArrayAdapter<Room> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Room.values());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Room selectedRoom = Room.values()[position];
            sendResult(selectedRoom.ordinal());
        });
    }

    private void sendResult(int roomOrdinal) {
        Intent data = new Intent();
        data.putExtra(EXTRA_ROOM_ORDINAL, roomOrdinal);
        setResult(Activity.RESULT_OK, data);
        finish();
    }
}
