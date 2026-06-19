package com.ptithcm.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ptithcm.myapplication.auth.AuthManager;

public class ChangePasswordActivity extends Activity {
    private AuthManager authManager;
    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText confirmPasswordInput;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);

        if (!authManager.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_change_password);
        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        errorText = findViewById(R.id.changePasswordErrorText);
        Button saveButton = findViewById(R.id.savePasswordButton);
        Button cancelButton = findViewById(R.id.cancelPasswordButton);

        saveButton.setOnClickListener(view -> handleChangePassword());
        cancelButton.setOnClickListener(view -> finish());
    }

    private void handleChangePassword() {
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!newPassword.equals(confirmPassword)) {
            errorText.setText("Xác nhận mật khẩu mới không khớp.");
            return;
        }

        AuthManager.AuthResult result = authManager.changePassword(
                currentPasswordInput.getText().toString(),
                newPassword
        );

        if (result.isSuccessful()) {
            Toast.makeText(this, "Đổi mật khẩu thành công.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        errorText.setText(result.getMessage());
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
