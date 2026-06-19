package com.ptithcm.myapplication;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.project.Project;
import com.ptithcm.myapplication.project.ProjectManager;
import com.ptithcm.myapplication.task.TaskItem;
import com.ptithcm.myapplication.task.TaskManager;
import com.ptithcm.myapplication.task.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_AVATAR = 1201;

    private AuthManager authManager;
    private ProjectManager projectManager;
    private TaskManager taskManager;
    private User currentUser;
    private ImageView avatarImage;
    private EditText fullNameInput;
    private TextView usernameText;
    private TextView roleText;
    private TextView assignedCountText;
    private TextView doneCountText;
    private TextView projectCountText;
    private TextView statusText;
    private LinearLayout assignedTasksContainer;
    private LinearLayout doneTasksContainer;
    private LinearLayout projectsContainer;
    private String selectedAvatarUri = "";
    private boolean hasPendingAvatarChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);
        projectManager = new ProjectManager(this);
        taskManager = new TaskManager(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_profile);
        bindViews();
        bindActions();
        FooterNavigationHelper.bind(this, 0);
        renderProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, 0);
        currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            if (!hasPendingAvatarChange) {
                renderProfile();
            }
        }
    }

    private void bindViews() {
        avatarImage = findViewById(R.id.profileAvatarImage);
        fullNameInput = findViewById(R.id.profileFullNameInput);
        usernameText = findViewById(R.id.profileUsernameText);
        roleText = findViewById(R.id.profileRoleText);
        assignedCountText = findViewById(R.id.profileAssignedCountText);
        doneCountText = findViewById(R.id.profileDoneCountText);
        projectCountText = findViewById(R.id.profileProjectCountText);
        statusText = findViewById(R.id.profileStatusText);
        assignedTasksContainer = findViewById(R.id.profileAssignedTasksContainer);
        doneTasksContainer = findViewById(R.id.profileDoneTasksContainer);
        projectsContainer = findViewById(R.id.profileProjectsContainer);
    }

    private void bindActions() {
        Button pickAvatarButton = findViewById(R.id.pickAvatarButton);
        Button saveProfileButton = findViewById(R.id.saveProfileButton);
        Button changePasswordButton = findViewById(R.id.profileChangePasswordButton);

        pickAvatarButton.setOnClickListener(view -> openAvatarPicker());
        saveProfileButton.setOnClickListener(view -> saveProfile());
        changePasswordButton.setOnClickListener(view ->
                startActivity(new Intent(this, ChangePasswordActivity.class))
        );
    }

    private void renderProfile() {
        if (!hasPendingAvatarChange) {
            selectedAvatarUri = currentUser.getAvatarUri();
        }
        fullNameInput.setText(currentUser.getFullName());
        usernameText.setText("Username: " + currentUser.getUsername());
        roleText.setText("Vai trò: " + currentUser.getRole().getDisplayName());
        renderAvatar();

        List<TaskItem> assignedTasks = getAssignedTasks();
        List<TaskItem> doneTasks = getDoneTasks(assignedTasks);
        List<Project> joinedProjects = getJoinedProjects();

        assignedCountText.setText(String.valueOf(assignedTasks.size()));
        doneCountText.setText(String.valueOf(doneTasks.size()));
        projectCountText.setText(String.valueOf(joinedProjects.size()));

        renderTaskList(assignedTasksContainer, assignedTasks, "Chưa có công việc được giao.");
        renderTaskList(doneTasksContainer, doneTasks, "Chưa có công việc hoàn thành.");
        renderProjectList(projectsContainer, joinedProjects);
    }

    private void renderAvatar() {
        if (selectedAvatarUri == null || selectedAvatarUri.trim().isEmpty()) {
            avatarImage.setImageResource(android.R.drawable.ic_menu_myplaces);
            return;
        }
        avatarImage.setImageURI(Uri.parse(selectedAvatarUri));
    }

    private void openAvatarPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn ảnh đại diện"), REQUEST_PICK_AVATAR);
        } catch (ActivityNotFoundException exception) {
            Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fallbackIntent.setType("image/*");
            fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(Intent.createChooser(fallbackIntent, "Chọn ảnh đại diện"), REQUEST_PICK_AVATAR);
            } catch (ActivityNotFoundException ignored) {
                showProfileMessage("Không tìm thấy ứng dụng chọn ảnh.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_AVATAR || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        try {
            int flags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            if (flags != 0) {
                getContentResolver().takePersistableUriPermission(uri, flags);
            }
        } catch (RuntimeException ignored) {
        }
        selectedAvatarUri = uri.toString();
        hasPendingAvatarChange = true;
        renderAvatar();
        showProfileMessage("Đã chọn ảnh đại diện. Bấm Lưu hồ sơ để lưu.");
    }

    private void saveProfile() {
        AuthManager.AuthResult result = authManager.updateCurrentUserProfile(
                fullNameInput.getText().toString(),
                selectedAvatarUri
        );
        if (result.isSuccessful()) {
            hasPendingAvatarChange = false;
            currentUser = result.getUser();
            renderProfile();
            showProfileMessage("Đã lưu hồ sơ.");
            return;
        }
        showProfileMessage(result.getMessage());
    }

    private void showProfileMessage(String message) {
        if (statusText != null) {
            statusText.setText(message);
            statusText.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private List<TaskItem> getAssignedTasks() {
        List<TaskItem> assignedTasks = new ArrayList<>();
        for (TaskItem task : taskManager.getTasks(false)) {
            if (currentUser.getUsername().equals(task.getAssigneeUsername())) {
                assignedTasks.add(task);
            }
        }
        return assignedTasks;
    }

    private List<TaskItem> getDoneTasks(List<TaskItem> assignedTasks) {
        List<TaskItem> doneTasks = new ArrayList<>();
        for (TaskItem task : assignedTasks) {
            if (TaskStatus.DONE.equals(task.getStatus())) {
                doneTasks.add(task);
            }
        }
        return doneTasks;
    }

    private List<Project> getJoinedProjects() {
        List<Project> joinedProjects = new ArrayList<>();
        for (Project project : projectManager.getProjects()) {
            if (project.getManagerUsername().equals(currentUser.getUsername())
                    || project.getMemberUsernames().contains(currentUser.getUsername())) {
                joinedProjects.add(project);
            }
        }
        return joinedProjects;
    }

    private void renderTaskList(LinearLayout container, List<TaskItem> tasks, String emptyText) {
        container.removeAllViews();
        if (tasks.isEmpty()) {
            addEmptyText(container, emptyText);
            return;
        }
        for (TaskItem task : tasks) {
            container.addView(createInfoCard(
                    task.getTitle(),
                    "Dự án: " + getProjectName(task.getProjectId())
                            + "\nTrạng thái: " + task.getStatus()
                            + "\nƯu tiên: " + task.getPriority()
                            + "\nHạn: " + (task.getDueDate().isEmpty() ? "N/A" : task.getDueDate())
            ));
        }
    }

    private void renderProjectList(LinearLayout container, List<Project> projects) {
        container.removeAllViews();
        if (projects.isEmpty()) {
            addEmptyText(container, "Chưa tham gia dự án nào.");
            return;
        }
        for (Project project : projects) {
            container.addView(createInfoCard(
                    project.getName().isEmpty() ? "N/A" : project.getName(),
                    "Manager: " + project.getManagerUsername()
                            + "\nThành viên: " + project.getMemberUsernames().size()
                            + "\nHạn: " + (project.getDueDate().isEmpty() ? "N/A" : project.getDueDate())
            ));
        }
    }

    private MaterialCardView createInfoCard(String title, String body) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface_white));
        card.setRadius(dp(10));
        card.setCardElevation(dp(1));
        card.setStrokeColor(getColor(R.color.card_stroke));
        card.setStrokeWidth(dp(1));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(8), 0, 0);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(getColor(R.color.auth_title));
        titleText.setTextSize(15);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView bodyText = new TextView(this);
        bodyText.setText(body);
        bodyText.setTextColor(getColor(R.color.auth_body));
        bodyText.setTextSize(14);
        bodyText.setPadding(0, dp(6), 0, 0);

        content.addView(titleText);
        content.addView(bodyText);
        card.addView(content);
        return card;
    }

    private void addEmptyText(LinearLayout container, String value) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(getColor(R.color.auth_body));
        textView.setTextSize(14);
        textView.setPadding(0, dp(8), 0, 0);
        container.addView(textView);
    }

    private String getProjectName(String projectId) {
        for (Project project : projectManager.getProjects()) {
            if (project.getId().equals(projectId)) {
                return project.getName().isEmpty() ? "N/A" : project.getName();
            }
        }
        return "N/A";
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
