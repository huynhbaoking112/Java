package com.ptithcm.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReportsActivity extends AppCompatActivity {
    private AuthManager authManager;
    private ProjectManager projectManager;
    private TaskManager taskManager;
    private User currentUser;
    private LinearLayout statusReportContainer;
    private LinearLayout projectReportContainer;
    private LinearLayout assigneeReportContainer;

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

        setContentView(R.layout.activity_reports);
        bindViews();
        FooterNavigationHelper.bind(this, R.id.menu_reports);
        renderReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_reports);
        renderReports();
    }

    private void bindViews() {
        statusReportContainer = findViewById(R.id.statusReportContainer);
        projectReportContainer = findViewById(R.id.projectReportContainer);
        assigneeReportContainer = findViewById(R.id.assigneeReportContainer);
    }

    private void renderReports() {
        List<TaskItem> visibleTasks = getVisibleTasks();
        int total = visibleTasks.size();
        int todo = 0;
        int doing = 0;
        int done = 0;
        int overdue = 0;
        int highPriority = 0;

        for (TaskItem task : visibleTasks) {
            if (TaskStatus.TODO.equals(task.getStatus())) {
                todo++;
            }
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

        setText(R.id.reportScopeText, getScopeDescription());
        setText(R.id.reportTotalTasksText, String.valueOf(total));
        setText(R.id.reportTodoTasksText, String.valueOf(todo));
        setText(R.id.reportDoingTasksText, String.valueOf(doing));
        setText(R.id.reportDoneTasksText, String.valueOf(done));
        setText(R.id.reportOverdueTasksText, String.valueOf(overdue));
        setText(R.id.reportHighPriorityTasksText, String.valueOf(highPriority));

        renderStatusReport(todo, doing, done, overdue, highPriority);
        renderProjectReport(visibleTasks);
        renderAssigneeReport(visibleTasks);
    }

    private void renderStatusReport(int todo, int doing, int done, int overdue, int highPriority) {
        statusReportContainer.removeAllViews();
        addReportRow(statusReportContainer, "Todo", todo);
        addReportRow(statusReportContainer, "Đang làm", doing);
        addReportRow(statusReportContainer, "Hoàn thành", done);
        addReportRow(statusReportContainer, "Quá hạn", overdue);
        addReportRow(statusReportContainer, "Ưu tiên cao", highPriority);
    }

    private void renderProjectReport(List<TaskItem> visibleTasks) {
        projectReportContainer.removeAllViews();
        Map<String, Integer> counts = new HashMap<>();
        for (TaskItem task : visibleTasks) {
            String projectName = getProjectName(task.getProjectId());
            counts.put(projectName, counts.getOrDefault(projectName, 0) + 1);
        }
        renderMapReport(projectReportContainer, counts, "Chưa có dữ liệu dự án.");
    }

    private void renderAssigneeReport(List<TaskItem> visibleTasks) {
        assigneeReportContainer.removeAllViews();
        Map<String, Integer> counts = new HashMap<>();
        for (TaskItem task : visibleTasks) {
            String assignee = task.getAssigneeUsername().isEmpty() ? "N/A" : task.getAssigneeUsername();
            counts.put(assignee, counts.getOrDefault(assignee, 0) + 1);
        }
        renderMapReport(assigneeReportContainer, counts, "Chưa có dữ liệu thành viên.");
    }

    private void renderMapReport(LinearLayout container, Map<String, Integer> counts, String emptyText) {
        if (counts.isEmpty()) {
            TextView textView = new TextView(this);
            textView.setText(emptyText);
            textView.setTextColor(getColor(R.color.auth_body));
            textView.setTextSize(14);
            textView.setPadding(0, dp(8), 0, 0);
            container.addView(textView);
            return;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            addReportRow(container, entry.getKey(), entry.getValue());
        }
    }

    private void addReportRow(LinearLayout container, String label, int value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextColor(getColor(R.color.auth_title));
        labelText.setTextSize(15);
        labelText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView valueText = new TextView(this);
        valueText.setText(String.valueOf(value));
        valueText.setTextColor(getColor(R.color.primary_blue));
        valueText.setTextSize(15);
        valueText.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        row.addView(labelText);
        row.addView(valueText);
        container.addView(row);
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

    private String getProjectName(String projectId) {
        for (Project project : projectManager.getProjects()) {
            if (project.getId().equals(projectId)) {
                return project.getName().isEmpty() ? "N/A" : project.getName();
            }
        }
        return "N/A";
    }

    private String getScopeDescription() {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return "Báo cáo toàn bộ công việc trong hệ thống.";
        }
        if (currentUser.getRole() == UserRole.MANAGER) {
            return "Báo cáo các dự án bạn quản lý và công việc được giao.";
        }
        return "Báo cáo các công việc được giao cho bạn.";
    }

    private void setText(int viewId, String value) {
        TextView textView = findViewById(viewId);
        textView.setText(value);
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
