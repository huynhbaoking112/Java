package com.ptithcm.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.auth.UserRole;

public final class FooterNavigationHelper {
    private FooterNavigationHelper() {
    }

    public static void bind(Activity activity, int selectedItemId) {
        BottomNavigationView footerMenu = activity.findViewById(R.id.dashboardFooterMenu);
        if (footerMenu == null) {
            return;
        }

        AuthManager authManager = new AuthManager(activity);
        User currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        UserRole role = currentUser.getRole();
        Menu menu = footerMenu.getMenu();
        menu.findItem(R.id.menu_users).setVisible(role.canManageUsers());
        menu.findItem(R.id.menu_projects).setVisible(role.canManageProjects());
        menu.findItem(R.id.menu_reports).setVisible(role.canViewReports());
        menu.findItem(R.id.menu_tasks).setVisible(role.canUpdateAssignedTasks());

        footerMenu.setOnItemSelectedListener(null);
        if (selectedItemId != 0) {
            footerMenu.setSelectedItemId(selectedItemId);
        }
        footerMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (selectedItemId != 0 && itemId == selectedItemId) {
                return true;
            }
            if (itemId == R.id.menu_dashboard) {
                open(activity, MainActivity.class);
                return true;
            }
            if (itemId == R.id.menu_users) {
                open(activity, UserManagementActivity.class);
                return true;
            }
            if (itemId == R.id.menu_projects) {
                open(activity, ProjectManagementActivity.class);
                return true;
            }
            if (itemId == R.id.menu_tasks) {
                open(activity, TaskManagementActivity.class);
                return true;
            }
            if (itemId == R.id.menu_reports) {
                Toast.makeText(activity, "Chức năng báo cáo thống kê sẽ làm ở task tiếp theo.", Toast.LENGTH_SHORT).show();
                return false;
            }
            return false;
        });
    }

    private static void open(Activity activity, Class<?> targetActivity) {
        if (activity.getClass().equals(targetActivity)) {
            return;
        }
        activity.startActivity(new Intent(activity, targetActivity));
    }
}
