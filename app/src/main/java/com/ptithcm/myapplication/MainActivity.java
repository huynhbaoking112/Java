package com.ptithcm.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.auth.UserRole;

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
        bindRoleFeatures();
        bindActions();
    }

    private void bindUserInfo() {
        TextView userNameText = findViewById(R.id.currentUserText);
        TextView roleText = findViewById(R.id.currentRoleText);

        userNameText.setText(currentUser.getFullName() + " (" + currentUser.getUsername() + ")");
        roleText.setText("Vai trò: " + currentUser.getRole().getDisplayName());
    }

    private void bindRoleFeatures() {
        UserRole role = currentUser.getRole();

        configureFeatureButton(
                R.id.manageUsersButton,
                role.canManageUsers(),
                "Admin được phép quản lý tài khoản người dùng."
        );
        configureFeatureButton(
                R.id.manageProjectsButton,
                role.canManageProjects(),
                "Admin/Manager được phép quản lý dự án."
        );
        configureFeatureButton(
                R.id.assignTasksButton,
                role.canAssignTasks(),
                "Admin/Manager được phép phân công công việc."
        );
        configureFeatureButton(
                R.id.viewReportsButton,
                role.canViewReports(),
                "Admin/Manager được phép xem báo cáo thống kê."
        );
        configureFeatureButton(
                R.id.updateTasksButton,
                role.canUpdateAssignedTasks(),
                "Người dùng được phép cập nhật công việc được giao."
        );
    }

    private void configureFeatureButton(int buttonId, boolean allowed, String message) {
        Button button = findViewById(buttonId);
        button.setVisibility(allowed ? View.VISIBLE : View.GONE);
        button.setOnClickListener(view -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
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
