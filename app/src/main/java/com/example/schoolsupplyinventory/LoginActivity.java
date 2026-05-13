package com.example.schoolsupplyinventory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText mEmailField;
    private TextInputEditText mPasswordField;
    private MaterialButton mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmailField = findViewById(R.id.login_email);
        mPasswordField = findViewById(R.id.login_password);
        mLoginButton = findViewById(R.id.btn_login);

        mLoginButton.setOnClickListener(v -> {
            String email = mEmailField.getText().toString();
            String password = mPasswordField.getText().toString();

            // Mock authentication
            if (email.equals("admin@supplyflow.com") && password.equals("password")) {
                SupplyLab.get(this).setCurrentUser(email);
                Intent intent = new Intent(LoginActivity.this, InventoryActivity.class);
                startActivity(intent);
                finish();
            } else if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_signup).setOnClickListener(v -> {
            Toast.makeText(this, "Registration coming soon", Toast.LENGTH_SHORT).show();
        });
    }
}
