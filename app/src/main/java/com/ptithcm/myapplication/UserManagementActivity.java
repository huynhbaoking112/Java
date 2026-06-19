package com.ptithcm.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.auth.UserRole;

import java.util.List;

public class UserManagementActivity extends Activity {
    private AuthManager authManager;
    private LinearLayout usersContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);

        User currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            openLoginScreen();
            return;
        }
        if (!currentUser.getRole().canManageUsers()) {
            Toast.makeText(this, "Chi Admin duoc quan ly nguoi dung.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_user_management);
        usersContainer = findViewById(R.id.usersContainer);
        FooterNavigationHelper.bind(this, R.id.menu_users);
        bindActions();
        renderUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_users);
    }

    private void bindActions() {
        Button createUserButton = findViewById(R.id.openCreateUserDialogButton);

        createUserButton.setOnClickListener(view -> showUserDialog(null));
    }

    private void showUserDialog(User user) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_user_form);

        TextView titleText = dialog.findViewById(R.id.dialogUserTitleText);
        EditText usernameInput = dialog.findViewById(R.id.dialogUserUsernameInput);
        EditText fullNameInput = dialog.findViewById(R.id.dialogUserFullNameInput);
        EditText passwordInput = dialog.findViewById(R.id.dialogUserPasswordInput);
        Spinner roleSpinner = dialog.findViewById(R.id.dialogUserRoleSpinner);
        TextView errorText = dialog.findViewById(R.id.dialogUserErrorText);
        Button saveButton = dialog.findViewById(R.id.dialogSaveUserButton);
        Button cancelButton = dialog.findViewById(R.id.dialogCancelUserButton);

        setupRoleSpinner(roleSpinner);
        boolean editing = user != null;

        if (editing) {
            titleText.setText("Sua tai khoan");
            usernameInput.setText(user.getUsername());
            usernameInput.setEnabled(false);
            fullNameInput.setText(user.getFullName());
            passwordInput.setHint("Mat khau moi (de trong neu khong doi)");
            setSelectedRole(roleSpinner, user.getRole());
            saveButton.setText("Cap nhat tai khoan");
        } else {
            titleText.setText("Tao tai khoan");
            setSelectedRole(roleSpinner, UserRole.MEMBER);
            saveButton.setText("Tao tai khoan");
        }

        saveButton.setOnClickListener(view -> {
            AuthManager.AuthResult result;
            UserRole selectedRole = getSelectedRole(roleSpinner);

            if (editing) {
                result = authManager.updateUser(
                        user.getUsername(),
                        fullNameInput.getText().toString(),
                        selectedRole,
                        passwordInput.getText().toString()
                );
            } else {
                result = authManager.createUser(
                        usernameInput.getText().toString(),
                        fullNameInput.getText().toString(),
                        selectedRole,
                        passwordInput.getText().toString()
                );
            }

            if (!result.isSuccessful()) {
                errorText.setText(result.getMessage());
                return;
            }

            Toast.makeText(this, editing ? "Da cap nhat tai khoan." : "Da tao tai khoan.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            renderUsers();
        });

        cancelButton.setOnClickListener(view -> dialog.dismiss());

        Window window = dialog.getWindow();
        dialog.setOnShowListener(dialogInterface -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                shownWindow.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
            }
        });
        dialog.show();
        if (window != null) {
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setupRoleSpinner(Spinner roleSpinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{
                        UserRole.ADMIN.getDisplayName(),
                        UserRole.MANAGER.getDisplayName(),
                        UserRole.MEMBER.getDisplayName()
                }
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
    }

    private void deleteUser(User user) {
        AuthManager.AuthResult result = authManager.deleteUser(user.getUsername());
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
        if (result.isSuccessful()) {
            renderUsers();
        }
    }

    private void renderUsers() {
        usersContainer.removeAllViews();
        List<User> users = authManager.getUsers();
        for (User user : users) {
            usersContainer.addView(createUserCard(user));
        }
    }

    private MaterialCardView createUserCard(User user) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(android.R.color.white));
        card.setRadius(dp(8));
        card.setCardElevation(dp(1));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.card_stroke));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView nameText = new TextView(this);
        nameText.setText(user.getFullName() + " (" + user.getUsername() + ")");
        nameText.setTextColor(getColor(R.color.auth_title));
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView roleText = new TextView(this);
        roleText.setText("Vai tro: " + user.getRole().getDisplayName());
        roleText.setTextColor(getColor(R.color.auth_body));
        roleText.setTextSize(14);
        roleText.setPadding(0, dp(4), 0, 0);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);

        Button editButton = new Button(this);
        editButton.setText("Sua");
        editButton.setAllCaps(false);
        editButton.setOnClickListener(view -> showUserDialog(user));

        Button deleteButton = new Button(this);
        deleteButton.setText("Xoa");
        deleteButton.setAllCaps(false);
        deleteButton.setTextColor(getColor(R.color.auth_error));
        deleteButton.setOnClickListener(view -> deleteUser(user));

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        editParams.setMargins(0, 0, dp(8), 0);
        actions.addView(editButton, editParams);
        actions.addView(deleteButton, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        content.addView(nameText);
        content.addView(roleText);
        content.addView(actions);
        card.addView(content);
        return card;
    }

    private UserRole getSelectedRole(Spinner roleSpinner) {
        String selectedRole = (String) roleSpinner.getSelectedItem();
        return UserRole.fromValue(selectedRole);
    }

    private void setSelectedRole(Spinner roleSpinner, UserRole role) {
        for (int i = 0; i < roleSpinner.getCount(); i++) {
            if (roleSpinner.getItemAtPosition(i).toString().equals(role.getDisplayName())) {
                roleSpinner.setSelection(i);
                return;
            }
        }
        roleSpinner.setSelection(2);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
