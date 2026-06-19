package com.ptithcm.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
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
    private PieChart statusPieChart;
    private BarChart projectBarChart;
    private LinearLayout statusReportContainer;
    private LinearLayout projectReportContainer;
    private LinearLayout assigneeReportContainer;
    private LinearLayout projectProgressContainer;

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
        statusPieChart = findViewById(R.id.reportStatusPieChart);
        projectBarChart = findViewById(R.id.reportProjectBarChart);
        statusReportContainer = findViewById(R.id.statusReportContainer);
        projectReportContainer = findViewById(R.id.projectReportContainer);
        assigneeReportContainer = findViewById(R.id.assigneeReportContainer);
        projectProgressContainer = findViewById(R.id.projectProgressContainer);
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
        renderStatusChart(todo, doing, done, overdue);
        renderProjectChart(visibleTasks);
        renderProjectProgress(visibleTasks);
    }

    private void renderStatusReport(int todo, int doing, int done, int overdue, int highPriority) {
        statusReportContainer.removeAllViews();
        addReportRow(statusReportContainer, "Todo", todo);
        addReportRow(statusReportContainer, "Đang làm", doing);
        addReportRow(statusReportContainer, "Hoàn thành", done);
        addReportRow(statusReportContainer, "Quá hạn", overdue);
        addReportRow(statusReportContainer, "Ưu tiên cao", highPriority);
    }

    private void renderStatusChart(int todo, int doing, int done, int overdue) {
        List<PieEntry> entries = new ArrayList<>();
        if (todo > 0) {
            entries.add(new PieEntry(todo, "Todo"));
        }
        if (doing > 0) {
            entries.add(new PieEntry(doing, "Đang làm"));
        }
        if (done > 0) {
            entries.add(new PieEntry(done, "Hoàn thành"));
        }
        if (overdue > 0) {
            entries.add(new PieEntry(overdue, "Quá hạn"));
        }

        if (entries.isEmpty()) {
            statusPieChart.clear();
            statusPieChart.setNoDataText("Chưa có dữ liệu biểu đồ.");
            statusPieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(getColor(R.color.primary_blue), getColor(R.color.accent_amber),
                getColor(R.color.success_green), getColor(R.color.auth_error));
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColor(getColor(R.color.surface_white));
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        statusPieChart.setData(data);
        statusPieChart.getDescription().setEnabled(false);
        statusPieChart.setDrawHoleEnabled(true);
        statusPieChart.setHoleRadius(48f);
        statusPieChart.setCenterText("Trạng thái");
        statusPieChart.setCenterTextColor(getColor(R.color.auth_title));
        statusPieChart.setEntryLabelColor(getColor(R.color.auth_title));
        statusPieChart.setEntryLabelTextSize(11f);
        statusPieChart.getLegend().setWordWrapEnabled(true);
        statusPieChart.invalidate();
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

    private void renderProjectChart(List<TaskItem> visibleTasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (TaskItem task : visibleTasks) {
            String projectName = getProjectName(task.getProjectId());
            counts.put(projectName, counts.getOrDefault(projectName, 0) + 1);
        }

        if (counts.isEmpty()) {
            projectBarChart.clear();
            projectBarChart.setNoDataText("Chưa có dữ liệu biểu đồ.");
            projectBarChart.invalidate();
            return;
        }

        List<String> labels = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            labels.add(entry.getKey());
            entries.add(new BarEntry(index, entry.getValue()));
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Task theo dự án");
        dataSet.setColor(getColor(R.color.primary_blue));
        dataSet.setValueTextColor(getColor(R.color.auth_title));
        dataSet.setValueTextSize(11f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.72f);
        projectBarChart.setData(data);
        projectBarChart.getDescription().setEnabled(false);
        projectBarChart.setDrawGridBackground(false);
        projectBarChart.setFitBars(true);
        projectBarChart.getAxisLeft().setAxisMinimum(0f);
        projectBarChart.getAxisRight().setEnabled(false);

        XAxis xAxis = projectBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-25f);

        Legend legend = projectBarChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        projectBarChart.invalidate();
    }

    private void renderProjectProgress(List<TaskItem> visibleTasks) {
        projectProgressContainer.removeAllViews();
        Map<String, List<TaskItem>> tasksByProject = new HashMap<>();
        for (TaskItem task : visibleTasks) {
            if (!tasksByProject.containsKey(task.getProjectId())) {
                tasksByProject.put(task.getProjectId(), new ArrayList<>());
            }
            tasksByProject.get(task.getProjectId()).add(task);
        }

        if (tasksByProject.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có dữ liệu tiến độ dự án.");
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, dp(8), 0, 0);
            projectProgressContainer.addView(emptyText);
            return;
        }

        for (Map.Entry<String, List<TaskItem>> entry : tasksByProject.entrySet()) {
            projectProgressContainer.addView(createProjectProgressCard(entry.getKey(), entry.getValue()));
        }
    }

    private MaterialCardView createProjectProgressCard(String projectId, List<TaskItem> projectTasks) {
        int total = projectTasks.size();
        int done = 0;
        for (TaskItem task : projectTasks) {
            if (TaskStatus.DONE.equals(task.getStatus())) {
                done++;
            }
        }
        int percent = total == 0 ? 0 : Math.round((done * 100f) / total);

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
        cardParams.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView titleText = new TextView(this);
        titleText.setText(getProjectName(projectId));
        titleText.setTextColor(getColor(R.color.auth_title));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);

        TextView progressText = new TextView(this);
        progressText.setText("Hoàn thành: " + done + "/" + total + " task (" + percent + "%)");
        progressText.setTextColor(getColor(R.color.primary_blue));
        progressText.setTextSize(15);
        progressText.setTypeface(null, Typeface.BOLD);
        progressText.setPadding(0, dp(6), 0, 0);

        TextView tasksTitle = createSectionTitle("Danh sách task");
        LinearLayout taskRows = new LinearLayout(this);
        taskRows.setOrientation(LinearLayout.VERTICAL);
        for (TaskItem task : projectTasks) {
            TextView taskText = new TextView(this);
            taskText.setText("- " + task.getTitle()
                    + " | " + task.getStatus()
                    + " | " + task.getAssigneeUsername()
                    + " | " + (task.getDueDate().isEmpty() ? "N/A" : task.getDueDate()));
            taskText.setTextColor(getColor(R.color.auth_body));
            taskText.setTextSize(14);
            taskText.setPadding(0, dp(4), 0, 0);
            taskRows.addView(taskText);
        }

        TextView membersTitle = createSectionTitle("Hiệu suất thành viên");
        LinearLayout memberRows = new LinearLayout(this);
        memberRows.setOrientation(LinearLayout.VERTICAL);
        Map<String, int[]> memberStats = getMemberStats(projectTasks);
        for (Map.Entry<String, int[]> memberEntry : memberStats.entrySet()) {
            int[] stats = memberEntry.getValue();
            int memberTotal = stats[0];
            int memberDone = stats[1];
            int memberPercent = memberTotal == 0 ? 0 : Math.round((memberDone * 100f) / memberTotal);

            TextView memberText = new TextView(this);
            memberText.setText(memberEntry.getKey() + ": " + memberDone + "/" + memberTotal + " hoàn thành (" + memberPercent + "%)");
            memberText.setTextColor(getColor(R.color.auth_body));
            memberText.setTextSize(14);
            memberText.setPadding(0, dp(4), 0, 0);
            memberRows.addView(memberText);
        }

        content.addView(titleText);
        content.addView(progressText);
        content.addView(tasksTitle);
        content.addView(taskRows);
        content.addView(membersTitle);
        content.addView(memberRows);
        card.addView(content);
        return card;
    }

    private Map<String, int[]> getMemberStats(List<TaskItem> projectTasks) {
        Map<String, int[]> stats = new HashMap<>();
        for (TaskItem task : projectTasks) {
            String username = task.getAssigneeUsername().isEmpty() ? "N/A" : task.getAssigneeUsername();
            if (!stats.containsKey(username)) {
                stats.put(username, new int[]{0, 0});
            }
            int[] values = stats.get(username);
            values[0]++;
            if (TaskStatus.DONE.equals(task.getStatus())) {
                values[1]++;
            }
        }
        return stats;
    }

    private TextView createSectionTitle(String value) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextColor(getColor(R.color.auth_title));
        textView.setTextSize(15);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(0, dp(12), 0, 0);
        return textView;
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
