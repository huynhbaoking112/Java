package com.ptithcm.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ptithcm.myapplication.auth.AuthManager;

public class LoginActivity extends Activity {
    private AuthManager authManager;
    private EditText usernameInput;
    private EditText passwordInput;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);

        if (authManager.isLoggedIn()) {
            openMainScreen();
            return;
        }

        setContentView(R.layout.activity_login);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        errorText = findViewById(R.id.loginErrorText);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(view -> handleLogin());
    }

    private void handleLogin() {
        AuthManager.AuthResult result = authManager.login(
                usernameInput.getText().toString(),
                passwordInput.getText().toString()
        );

        if (result.isSuccessful()) {
            Toast.makeText(this, "Đăng nhập thành công.", Toast.LENGTH_SHORT).show();
            openMainScreen();
            return;
        }

        errorText.setText(result.getMessage());
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
