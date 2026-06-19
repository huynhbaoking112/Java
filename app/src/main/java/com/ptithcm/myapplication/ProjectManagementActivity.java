package com.ptithcm.myapplication;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.auth.UserRole;
import com.ptithcm.myapplication.project.Project;
import com.ptithcm.myapplication.project.ProjectManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectManagementActivity extends AppCompatActivity {
    private AuthManager authManager;
    private ProjectManager projectManager;
    private User currentUser;
    private LinearLayout projectsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);
        projectManager = new ProjectManager(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }
        if (!currentUser.getRole().canManageProjects()) {
            Toast.makeText(this, "Chỉ Admin/Manager được quản lý dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_project_management);
        projectsContainer = findViewById(R.id.projectsContainer);
        FooterNavigationHelper.bind(this, R.id.menu_projects);
        bindActions();
        renderProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_projects);
    }

    private void bindActions() {
        Button createProjectButton = findViewById(R.id.openCreateProjectDialogButton);
        createProjectButton.setOnClickListener(view -> showProjectDialog(null));
    }

    private void showProjectDialog(Project project) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_project_form);

        TextView titleText = dialog.findViewById(R.id.dialogProjectTitleText);
        EditText nameInput = dialog.findViewById(R.id.dialogProjectNameInput);
        EditText descriptionInput = dialog.findViewById(R.id.dialogProjectDescriptionInput);
        EditText startDateInput = dialog.findViewById(R.id.dialogProjectStartDateInput);
        EditText dueDateInput = dialog.findViewById(R.id.dialogProjectDueDateInput);
        Spinner managerSpinner = dialog.findViewById(R.id.dialogProjectManagerSpinner);
        LinearLayout membersContainer = dialog.findViewById(R.id.dialogProjectMembersContainer);
        TextView errorText = dialog.findViewById(R.id.dialogProjectErrorText);
        Button saveButton = dialog.findViewById(R.id.dialogSaveProjectButton);
        Button cancelButton = dialog.findViewById(R.id.dialogCancelProjectButton);

        setupDatePicker(startDateInput);
        setupDatePicker(dueDateInput);

        List<User> users = authManager.getUsers();
        List<User> managers = getManagers(users);
        setupManagerSpinner(managerSpinner, managers);

        boolean editing = project != null;
        Set<String> selectedMembers = new HashSet<>();
        if (editing) {
            titleText.setText("Sửa dự án");
            nameInput.setText(project.getName());
            descriptionInput.setText(project.getDescription());
            startDateInput.setText(project.getStartDate());
            dueDateInput.setText(project.getDueDate());
            selectedMembers.addAll(project.getMemberUsernames());
            setSelectedManager(managerSpinner, managers, project.getManagerUsername());
            saveButton.setText("Cập nhật dự án");
        } else {
            titleText.setText("Tạo dự án");
            if (currentUser.getRole() == UserRole.MANAGER) {
                setSelectedManager(managerSpinner, managers, currentUser.getUsername());
            }
            saveButton.setText("Tạo dự án");
        }
        renderMemberCheckboxes(membersContainer, users, selectedMembers);

        saveButton.setOnClickListener(view -> {
            String managerUsername = getSelectedManagerUsername(managerSpinner, managers);
            ProjectManager.ProjectResult result;

            if (editing) {
                result = projectManager.updateProject(
                        project.getId(),
                        nameInput.getText().toString(),
                        descriptionInput.getText().toString(),
                        startDateInput.getText().toString(),
                        dueDateInput.getText().toString(),
                        managerUsername,
                        collectSelectedMembers(membersContainer)
                );
            } else {
                result = projectManager.createProject(
                        nameInput.getText().toString(),
                        descriptionInput.getText().toString(),
                        startDateInput.getText().toString(),
                        dueDateInput.getText().toString(),
                        managerUsername,
                        collectSelectedMembers(membersContainer)
                );
            }

            if (!result.isSuccessful()) {
                errorText.setText(result.getMessage());
                return;
            }

            Toast.makeText(this, editing ? "Đã cập nhật dự án." : "Đã tạo dự án.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            renderProjects();
        });
        cancelButton.setOnClickListener(view -> dialog.dismiss());

        dialog.setOnShowListener(dialogInterface -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                        (int) (getResources().getDisplayMetrics().heightPixels * 0.86f)
                );
            }
        });
        dialog.show();
    }

    private void setupDatePicker(EditText dateInput) {
        dateInput.setFocusable(false);
        dateInput.setCursorVisible(false);
        dateInput.setOnClickListener(view -> showDatePicker(dateInput));
    }

    private void showDatePicker(EditText dateInput) {
        Calendar calendar = Calendar.getInstance();
        String currentValue = dateInput.getText().toString().trim();
        if (currentValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = currentValue.split("-");
            calendar.set(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2])
            );
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> dateInput.setText(formatDate(year, month, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private String formatDate(int year, int month, int dayOfMonth) {
        return String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
    }

    private List<User> getManagers(List<User> users) {
        List<User> managers = new ArrayList<>();
        for (User user : users) {
            if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER) {
                managers.add(user);
            }
        }
        return managers;
    }

    private void setupManagerSpinner(Spinner managerSpinner, List<User> managers) {
        List<String> labels = new ArrayList<>();
        for (User user : managers) {
            labels.add(user.getFullName() + " (" + user.getUsername() + ")");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        managerSpinner.setAdapter(adapter);
    }

    private void setSelectedManager(Spinner managerSpinner, List<User> managers, String username) {
        for (int i = 0; i < managers.size(); i++) {
            if (managers.get(i).getUsername().equals(username)) {
                managerSpinner.setSelection(i);
                return;
            }
        }
    }

    private String getSelectedManagerUsername(Spinner managerSpinner, List<User> managers) {
        int selectedIndex = managerSpinner.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= managers.size()) {
            return "";
        }
        return managers.get(selectedIndex).getUsername();
    }

    private void renderMemberCheckboxes(LinearLayout membersContainer, List<User> users, Set<String> selectedMembers) {
        membersContainer.removeAllViews();
        for (User user : users) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(user.getFullName() + " (" + user.getUsername() + ")");
            checkBox.setTextColor(getColor(R.color.auth_body));
            checkBox.setTag(user.getUsername());
            checkBox.setChecked(selectedMembers.contains(user.getUsername()));
            membersContainer.addView(checkBox);
        }
    }

    private List<String> collectSelectedMembers(LinearLayout membersContainer) {
        List<String> members = new ArrayList<>();
        for (int i = 0; i < membersContainer.getChildCount(); i++) {
            if (!(membersContainer.getChildAt(i) instanceof CheckBox)) {
                continue;
            }
            CheckBox checkBox = (CheckBox) membersContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                members.add((String) checkBox.getTag());
            }
        }
        return members;
    }

    private void renderProjects() {
        projectsContainer.removeAllViews();
        List<Project> projects = projectManager.getProjects();
        if (projects.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có dự án nào.");
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(15);
            emptyText.setPadding(0, dp(16), 0, 0);
            projectsContainer.addView(emptyText);
            return;
        }

        for (Project project : projects) {
            projectsContainer.addView(createProjectCard(project));
        }
    }

    private MaterialCardView createProjectCard(Project project) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface_white));
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
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
        nameText.setText(project.getName());
        nameText.setTextColor(getColor(R.color.auth_title));
        nameText.setTextSize(17);
        nameText.setTypeface(null, Typeface.BOLD);

        TextView descriptionText = new TextView(this);
        descriptionText.setText(project.getDescription().isEmpty() ? "Không có mô tả." : project.getDescription());
        descriptionText.setTextColor(getColor(R.color.auth_body));
        descriptionText.setTextSize(14);
        descriptionText.setPadding(0, dp(6), 0, 0);

        TextView metaText = new TextView(this);
        metaText.setText("Manager: " + project.getManagerUsername()
                + "\nThành viên: " + project.getMemberUsernames().size()
                + "\nThời gian: " + displayDateRange(project));
        metaText.setTextColor(getColor(R.color.auth_body));
        metaText.setTextSize(14);
        metaText.setPadding(0, dp(8), 0, 0);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);

        MaterialButton editButton = new MaterialButton(this);
        editButton.setText("Sửa");
        editButton.setAllCaps(false);
        editButton.setOnClickListener(view -> showProjectDialog(project));

        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setText("Xóa");
        deleteButton.setAllCaps(false);
        deleteButton.setTextColor(getColor(R.color.auth_error));
        deleteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface_white)));
        deleteButton.setStrokeColor(ColorStateList.valueOf(getColor(R.color.auth_error)));
        deleteButton.setStrokeWidth(dp(1));
        deleteButton.setOnClickListener(view -> deleteProject(project));

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
        content.addView(descriptionText);
        content.addView(metaText);
        content.addView(actions);
        card.addView(content);
        return card;
    }

    private String displayDateRange(Project project) {
        String startDate = project.getStartDate().isEmpty() ? "N/A" : project.getStartDate();
        String dueDate = project.getDueDate().isEmpty() ? "N/A" : project.getDueDate();
        return startDate + " - " + dueDate;
    }

    private void deleteProject(Project project) {
        ProjectManager.ProjectResult result = projectManager.deleteProject(project.getId());
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
        if (result.isSuccessful()) {
            renderProjects();
        }
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
