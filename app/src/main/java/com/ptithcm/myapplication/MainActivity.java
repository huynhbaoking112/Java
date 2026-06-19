package com.ptithcm.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;

public class MainActivity extends Activity {
    private AuthManager authManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_main);
        bindUserInfo();
        FooterNavigationHelper.bind(this, R.id.menu_dashboard);
        bindActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_dashboard);
    }

    private void bindUserInfo() {
        TextView userNameText = findViewById(R.id.currentUserText);
        TextView roleText = findViewById(R.id.currentRoleText);

        userNameText.setText(currentUser.getFullName() + " (" + currentUser.getUsername() + ")");
        roleText.setText("Vai trò: " + currentUser.getRole().getDisplayName());
    }

    private void bindActions() {
        Button changePasswordButton = findViewById(R.id.changePasswordButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        changePasswordButton.setOnClickListener(view ->
                startActivity(new Intent(this, ChangePasswordActivity.class))
        );
        logoutButton.setOnClickListener(view -> {
            authManager.logout();
            Toast.makeText(this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
            openLoginScreen();
        });
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
