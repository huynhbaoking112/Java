package com.ptithcm.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.auth.UserRole;
import com.ptithcm.myapplication.project.Project;
import com.ptithcm.myapplication.project.ProjectManager;
import com.ptithcm.myapplication.task.TaskItem;
import com.ptithcm.myapplication.task.TaskManager;
import com.ptithcm.myapplication.task.TaskPriority;
import com.ptithcm.myapplication.task.TaskStatus;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private AuthManager authManager;
    private ProjectManager projectManager;
    private TaskManager taskManager;
    private User currentUser;

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

        setContentView(R.layout.activity_main);
        bindUserInfo();
        bindActions();
        FooterNavigationHelper.bind(this, R.id.menu_dashboard);
        renderDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_dashboard);
        renderDashboard();
    }

    private void bindUserInfo() {
        TextView userNameText = findViewById(R.id.currentUserText);
        TextView roleText = findViewById(R.id.currentRoleText);
        TextView scopeText = findViewById(R.id.dashboardScopeText);

        userNameText.setText(currentUser.getFullName() + " (" + currentUser.getUsername() + ")");
        roleText.setText("Vai trò: " + currentUser.getRole().getDisplayName());
        scopeText.setText(getScopeDescription());
    }

    private void renderDashboard() {
        List<TaskItem> visibleTasks = getVisibleTasks();
        int total = visibleTasks.size();
        int doing = 0;
        int done = 0;
        int overdue = 0;
        int highPriority = 0;

        for (TaskItem task : visibleTasks) {
            if (TaskStatus.DOING.equals(task.getStatus())) {
                doing++;
            }
            if (TaskStatus.DONE.equals(task.getStatus())) {
                done++;
            }
            if (TaskStatus.OVERDUE.equals(task.getStatus()) || isPastDue(task)) {
                overdue++;
            }
            if (TaskPriority.HIGH.equals(task.getPriority())) {
                highPriority++;
            }
        }

        setStatText(R.id.totalTasksText, total);
        setStatText(R.id.doingTasksText, doing);
        setStatText(R.id.doneTasksText, done);
        setStatText(R.id.overdueTasksText, overdue);
        setStatText(R.id.highPriorityTasksText, highPriority);
    }

    private void setStatText(int viewId, int value) {
        TextView textView = findViewById(viewId);
        textView.setText(String.valueOf(value));
    }

    private List<TaskItem> getVisibleTasks() {
        List<TaskItem> visibleTasks = new ArrayList<>();
        List<TaskItem> allTasks = taskManager.getTasks(false);
        UserRole role = currentUser.getRole();

        if (role == UserRole.ADMIN) {
            return allTasks;
        }

        Set<String> managedProjectIds = new HashSet<>();
        if (role == UserRole.MANAGER) {
            for (Project project : projectManager.getProjects()) {
                if (project.getManagerUsername().equals(currentUser.getUsername())) {
                    managedProjectIds.add(project.getId());
                }
            }
        }

        for (TaskItem task : allTasks) {
            if (role == UserRole.MANAGER && managedProjectIds.contains(task.getProjectId())) {
                visibleTasks.add(task);
            } else if (currentUser.getUsername().equals(task.getAssigneeUsername())) {
                visibleTasks.add(task);
            }
        }
        return visibleTasks;
    }

    private boolean isPastDue(TaskItem task) {
        if (task.getDueDate() == null || task.getDueDate().trim().isEmpty()) {
            return false;
        }
        if (TaskStatus.DONE.equals(task.getStatus())) {
            return false;
        }
        try {
            LocalDate dueDate = LocalDate.parse(task.getDueDate());
            return dueDate.isBefore(LocalDate.now());
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private String getScopeDescription() {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return "Tổng quan toàn bộ công việc trong hệ thống";
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return "Tổng quan các dự án bạn quản lý và công việc được giao";
        }
        return "Tổng quan các công việc được giao cho bạn";
    }

    private void bindActions() {
        Button settingsButton = findViewById(R.id.settingsButton);
        Button changePasswordButton = findViewById(R.id.changePasswordButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        settingsButton.setOnClickListener(view ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
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
