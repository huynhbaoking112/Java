package com.ptithcm.myapplication;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.ptithcm.myapplication.auth.AuthManager;
import com.ptithcm.myapplication.auth.User;
import com.ptithcm.myapplication.project.Project;
import com.ptithcm.myapplication.project.ProjectManager;
import com.ptithcm.myapplication.task.TaskItem;
import com.ptithcm.myapplication.task.TaskManager;
import com.ptithcm.myapplication.task.TaskNote;
import com.ptithcm.myapplication.task.TaskNoteManager;
import com.ptithcm.myapplication.task.TaskPriority;
import com.ptithcm.myapplication.task.TaskStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TASK_ID = "task_id";

    private AuthManager authManager;
    private ProjectManager projectManager;
    private TaskManager taskManager;
    private TaskNoteManager noteManager;
    private User currentUser;
    private TaskItem currentTask;
    private String taskId;

    private TextView titleText;
    private TextView metaText;
    private TextView descriptionText;
    private TextView tagsText;
    private Spinner statusSpinner;
    private EditText noteInput;
    private LinearLayout notesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);
        projectManager = new ProjectManager(this);
        taskManager = new TaskManager(this);
        noteManager = new TaskNoteManager(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null || taskId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy công việc.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_task_detail);
        bindViews();
        setupStatusSpinner();
        FooterNavigationHelper.bind(this, R.id.menu_tasks);
        bindActions();
        renderTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_tasks);
        renderTask();
    }

    private void bindViews() {
        titleText = findViewById(R.id.taskDetailTitleText);
        metaText = findViewById(R.id.taskDetailMetaText);
        descriptionText = findViewById(R.id.taskDetailDescriptionText);
        tagsText = findViewById(R.id.taskDetailTagsText);
        statusSpinner = findViewById(R.id.taskDetailStatusSpinner);
        noteInput = findViewById(R.id.taskNoteInput);
        notesContainer = findViewById(R.id.taskNotesContainer);
    }

    private void setupStatusSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TaskStatus.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
    }

    private void bindActions() {
        Button editTaskButton = findViewById(R.id.editTaskDetailButton);
        Button saveStatusButton = findViewById(R.id.saveTaskStatusButton);
        Button addNoteButton = findViewById(R.id.addTaskNoteButton);

        editTaskButton.setVisibility(currentUser.getRole().canAssignTasks()
                ? View.VISIBLE
                : View.GONE);
        editTaskButton.setOnClickListener(view -> showEditTaskDialog());
        saveStatusButton.setOnClickListener(view -> saveStatus());
        addNoteButton.setOnClickListener(view -> addNote());
    }

    private void showEditTaskDialog() {
        if (currentTask == null) {
            return;
        }
        List<Project> projects = projectManager.getProjects();
        if (projects.isEmpty()) {
            Toast.makeText(this, "Vui lòng tạo dự án trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task_form);

        TextView dialogTitleText = dialog.findViewById(R.id.dialogTaskTitleText);
        EditText taskTitleInput = dialog.findViewById(R.id.dialogTaskNameInput);
        EditText descriptionInput = dialog.findViewById(R.id.dialogTaskDescriptionInput);
        EditText startDateInput = dialog.findViewById(R.id.dialogTaskStartDateInput);
        EditText dueDateInput = dialog.findViewById(R.id.dialogTaskDueDateInput);
        EditText tagsInput = dialog.findViewById(R.id.dialogTaskTagsInput);
        Spinner projectSpinner = dialog.findViewById(R.id.dialogTaskProjectSpinner);
        Spinner assigneeSpinner = dialog.findViewById(R.id.dialogTaskAssigneeSpinner);
        Spinner statusSpinner = dialog.findViewById(R.id.dialogTaskStatusSpinner);
        Spinner prioritySpinner = dialog.findViewById(R.id.dialogTaskPrioritySpinner);
        TextView errorText = dialog.findViewById(R.id.dialogTaskErrorText);
        Button saveButton = dialog.findViewById(R.id.dialogSaveTaskButton);
        Button cancelButton = dialog.findViewById(R.id.dialogCancelTaskButton);

        dialogTitleText.setText("Sửa công việc");
        saveButton.setText("Cập nhật công việc");
        setupDatePicker(startDateInput);
        setupDatePicker(dueDateInput);
        setupProjectSpinner(projectSpinner, projects);
        setupChoiceSpinner(statusSpinner, TaskStatus.values());
        setupChoiceSpinner(prioritySpinner, TaskPriority.values());

        taskTitleInput.setText(currentTask.getTitle());
        descriptionInput.setText(currentTask.getDescription());
        startDateInput.setText(currentTask.getStartDate());
        dueDateInput.setText(currentTask.getDueDate());
        tagsInput.setText(currentTask.getTags());
        setSelectedProject(projectSpinner, projects, currentTask.getProjectId());
        setSelectedChoice(statusSpinner, currentTask.getStatus());
        setSelectedChoice(prioritySpinner, currentTask.getPriority());
        setupAssigneeSpinner(assigneeSpinner, getSelectedProject(projectSpinner, projects));
        setSelectedAssignee(assigneeSpinner, currentTask.getAssigneeUsername());

        projectSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                setupAssigneeSpinner(assigneeSpinner, projects.get(position));
                if (projects.get(position).getId().equals(currentTask.getProjectId())) {
                    setSelectedAssignee(assigneeSpinner, currentTask.getAssigneeUsername());
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        saveButton.setOnClickListener(view -> {
            Project selectedProject = getSelectedProject(projectSpinner, projects);
            TaskManager.TaskResult result = taskManager.updateTask(
                    currentTask.getId(),
                    selectedProject == null ? "" : selectedProject.getId(),
                    taskTitleInput.getText().toString(),
                    descriptionInput.getText().toString(),
                    getSelectedAssigneeUsername(assigneeSpinner),
                    (String) statusSpinner.getSelectedItem(),
                    (String) prioritySpinner.getSelectedItem(),
                    startDateInput.getText().toString(),
                    dueDateInput.getText().toString(),
                    tagsInput.getText().toString()
            );

            if (!result.isSuccessful()) {
                errorText.setText(result.getMessage());
                return;
            }

            Toast.makeText(this, "Đã cập nhật công việc.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            renderTask();
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
            calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> dateInput.setText(String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void setupProjectSpinner(Spinner spinner, List<Project> projects) {
        List<String> labels = new ArrayList<>();
        for (Project project : projects) {
            labels.add(project.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupChoiceSpinner(Spinner spinner, String[] choices) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupAssigneeSpinner(Spinner spinner, Project project) {
        List<String> usernames = project == null ? new ArrayList<>() : project.getMemberUsernames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, usernames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private Project getSelectedProject(Spinner spinner, List<Project> projects) {
        int selectedIndex = spinner.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= projects.size()) {
            return null;
        }
        return projects.get(selectedIndex);
    }

    private void setSelectedProject(Spinner spinner, List<Project> projects, String projectId) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(projectId)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setSelectedChoice(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setSelectedAssignee(Spinner spinner, String username) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(username)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String getSelectedAssigneeUsername(Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void renderTask() {
        currentTask = taskManager.findTaskById(taskId);
        if (currentTask == null) {
            Toast.makeText(this, "Không tìm thấy công việc.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        titleText.setText(currentTask.getTitle());
        metaText.setText("Dự án: " + getProjectName(currentTask.getProjectId())
                + "\nNgười thực hiện: " + currentTask.getAssigneeUsername()
                + "\nTrạng thái: " + currentTask.getStatus()
                + "\nƯu tiên: " + currentTask.getPriority()
                + "\nNgày bắt đầu: " + displayValue(currentTask.getStartDate())
                + "\nHạn hoàn thành: " + displayValue(currentTask.getDueDate()));
        descriptionText.setText(currentTask.getDescription().isEmpty()
                ? "Không có mô tả."
                : currentTask.getDescription());
        tagsText.setText(currentTask.getTags().isEmpty()
                ? "Tags: N/A"
                : "Tags: " + currentTask.getTags());
        setSelectedStatus(currentTask.getStatus());
        renderNotes();
    }

    private void saveStatus() {
        String selectedStatus = (String) statusSpinner.getSelectedItem();
        TaskManager.TaskResult result = taskManager.updateStatus(taskId, selectedStatus);
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
        if (result.isSuccessful()) {
            renderTask();
        }
    }

    private void addNote() {
        TaskNoteManager.NoteResult result = noteManager.addNote(
                taskId,
                currentUser.getUsername(),
                noteInput.getText().toString()
        );
        if (!result.isSuccessful()) {
            Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        noteInput.setText("");
        renderNotes();
    }

    private void renderNotes() {
        notesContainer.removeAllViews();
        List<TaskNote> notes = noteManager.getNotes(taskId);
        if (notes.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có ghi chú nào.");
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(15);
            emptyText.setPadding(0, dp(12), 0, 0);
            notesContainer.addView(emptyText);
            return;
        }

        for (TaskNote note : notes) {
            notesContainer.addView(createNoteCard(note));
        }
    }

    private MaterialCardView createNoteCard(TaskNote note) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface_white));
        card.setRadius(dp(12));
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
        content.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView authorText = new TextView(this);
        authorText.setText(note.getAuthorUsername() + " - " + note.getCreatedAt());
        authorText.setTextColor(getColor(R.color.auth_title));
        authorText.setTextSize(14);
        authorText.setTypeface(null, Typeface.BOLD);

        TextView contentText = new TextView(this);
        contentText.setText(note.getContent());
        contentText.setTextColor(getColor(R.color.auth_body));
        contentText.setTextSize(15);
        contentText.setPadding(0, dp(6), 0, 0);

        content.addView(authorText);
        content.addView(contentText);
        card.addView(content);
        return card;
    }

    private void setSelectedStatus(String status) {
        for (int i = 0; i < statusSpinner.getCount(); i++) {
            if (statusSpinner.getItemAtPosition(i).toString().equals(status)) {
                statusSpinner.setSelection(i);
                return;
            }
        }
    }

    private String getProjectName(String projectId) {
        for (Project project : projectManager.getProjects()) {
            if (project.getId().equals(projectId)) {
                return project.getName();
            }
        }
        return "N/A";
    }

    private String displayValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value;
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
