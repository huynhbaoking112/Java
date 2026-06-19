package com.ptithcm.myapplication;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
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
import com.ptithcm.myapplication.task.TaskAttachment;
import com.ptithcm.myapplication.task.TaskAttachmentManager;
import com.ptithcm.myapplication.task.TaskItem;
import com.ptithcm.myapplication.task.TaskManager;
import com.ptithcm.myapplication.task.TaskNote;
import com.ptithcm.myapplication.task.TaskNoteManager;
import com.ptithcm.myapplication.task.TaskPriority;
import com.ptithcm.myapplication.task.TaskStatus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskManagementActivity extends AppCompatActivity {
    private static final int REQUEST_PICK_TASK_ATTACHMENT = 2201;

    private AuthManager authManager;
    private ProjectManager projectManager;
    private TaskManager taskManager;
    private TaskNoteManager noteManager;
    private TaskAttachmentManager attachmentManager;
    private User currentUser;
    private LinearLayout tasksContainer;
    private EditText searchInput;
    private Spinner statusFilterSpinner;
    private Spinner priorityFilterSpinner;
    private Spinner projectFilterSpinner;
    private List<Project> filterProjects = new ArrayList<>();
    private boolean showDeleted;
    private String pendingAttachmentTaskId;
    private LinearLayout activeAttachmentsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);
        projectManager = new ProjectManager(this);
        taskManager = new TaskManager(this);
        noteManager = new TaskNoteManager(this);
        attachmentManager = new TaskAttachmentManager(this);
        currentUser = authManager.getCurrentUser();

        if (currentUser == null) {
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_task_management);
        tasksContainer = findViewById(R.id.tasksContainer);
        searchInput = findViewById(R.id.taskSearchInput);
        statusFilterSpinner = findViewById(R.id.taskStatusFilterSpinner);
        priorityFilterSpinner = findViewById(R.id.taskPriorityFilterSpinner);
        projectFilterSpinner = findViewById(R.id.taskProjectFilterSpinner);
        FooterNavigationHelper.bind(this, R.id.menu_tasks);
        setupFilters();
        bindActions();
        renderTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, R.id.menu_tasks);
        setupProjectFilter();
        renderTasks();
    }

    private void bindActions() {
        Button createTaskButton = findViewById(R.id.openCreateTaskDialogButton);
        Button clearFiltersButton = findViewById(R.id.clearTaskFiltersButton);
        Switch deletedSwitch = findViewById(R.id.showDeletedTasksSwitch);

        createTaskButton.setVisibility(currentUser.getRole().canAssignTasks() ? View.VISIBLE : View.GONE);
        createTaskButton.setOnClickListener(view -> showTaskDialog(null));
        deletedSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            showDeleted = isChecked;
            renderTasks();
        });
        clearFiltersButton.setOnClickListener(view -> clearFilters());
    }

    private void setupFilters() {
        setupTextSearch();
        setupStatusFilter();
        setupPriorityFilter();
        setupProjectFilter();
    }

    private void setupTextSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                renderTasks();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void setupStatusFilter() {
        List<String> choices = new ArrayList<>();
        choices.add("Tất cả trạng thái");
        for (String status : TaskStatus.values()) {
            choices.add(status);
        }
        setFilterAdapter(statusFilterSpinner, choices);
        statusFilterSpinner.setOnItemSelectedListener(new FilterChangeListener());
    }

    private void setupPriorityFilter() {
        List<String> choices = new ArrayList<>();
        choices.add("Tất cả ưu tiên");
        for (String priority : TaskPriority.values()) {
            choices.add(priority);
        }
        setFilterAdapter(priorityFilterSpinner, choices);
        priorityFilterSpinner.setOnItemSelectedListener(new FilterChangeListener());
    }

    private void setupProjectFilter() {
        String selectedProjectId = getSelectedFilterProjectId();
        filterProjects = projectManager.getProjects();

        List<String> labels = new ArrayList<>();
        labels.add("Tất cả dự án");
        int selectedIndex = 0;
        for (int i = 0; i < filterProjects.size(); i++) {
            Project project = filterProjects.get(i);
            labels.add(project.getName().isEmpty() ? "N/A" : project.getName());
            if (project.getId().equals(selectedProjectId)) {
                selectedIndex = i + 1;
            }
        }

        setFilterAdapter(projectFilterSpinner, labels);
        projectFilterSpinner.setSelection(selectedIndex);
        projectFilterSpinner.setOnItemSelectedListener(new FilterChangeListener());
    }

    private void setFilterAdapter(Spinner spinner, List<String> choices) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void clearFilters() {
        searchInput.setText("");
        statusFilterSpinner.setSelection(0);
        priorityFilterSpinner.setSelection(0);
        projectFilterSpinner.setSelection(0);
        renderTasks();
    }

    private void showTaskDialog(TaskItem task) {
        List<Project> projects = projectManager.getProjects();
        if (projects.isEmpty()) {
            Toast.makeText(this, "Vui lòng tạo dự án trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task_form);

        TextView titleText = dialog.findViewById(R.id.dialogTaskTitleText);
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

        setupDatePicker(startDateInput);
        setupDatePicker(dueDateInput);
        setupProjectSpinner(projectSpinner, projects);
        setupChoiceSpinner(statusSpinner, TaskStatus.values());
        setupChoiceSpinner(prioritySpinner, TaskPriority.values());

        boolean editing = task != null;
        if (editing) {
            titleText.setText("Sửa công việc");
            taskTitleInput.setText(task.getTitle());
            descriptionInput.setText(task.getDescription());
            startDateInput.setText(task.getStartDate());
            dueDateInput.setText(task.getDueDate());
            tagsInput.setText(task.getTags());
            setSelectedProject(projectSpinner, projects, task.getProjectId());
            setSelectedChoice(statusSpinner, task.getStatus());
            setSelectedChoice(prioritySpinner, task.getPriority());
            saveButton.setText("Cập nhật công việc");
        } else {
            titleText.setText("Tạo công việc");
            setSelectedChoice(statusSpinner, TaskStatus.TODO);
            setSelectedChoice(prioritySpinner, TaskPriority.MEDIUM);
            saveButton.setText("Tạo công việc");
        }

        setupAssigneeSpinner(assigneeSpinner, getSelectedProject(projectSpinner, projects));
        if (editing) {
            setSelectedAssignee(assigneeSpinner, task.getAssigneeUsername());
        }
        projectSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                setupAssigneeSpinner(assigneeSpinner, projects.get(position));
                if (editing && projects.get(position).getId().equals(task.getProjectId())) {
                    setSelectedAssignee(assigneeSpinner, task.getAssigneeUsername());
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        boolean canEditAll = currentUser.getRole().canAssignTasks();
        if (!canEditAll) {
            taskTitleInput.setEnabled(false);
            descriptionInput.setEnabled(false);
            projectSpinner.setEnabled(false);
            assigneeSpinner.setEnabled(false);
            prioritySpinner.setEnabled(false);
            startDateInput.setEnabled(false);
            dueDateInput.setEnabled(false);
            tagsInput.setEnabled(false);
        }

        saveButton.setOnClickListener(view -> {
            Project selectedProject = getSelectedProject(projectSpinner, projects);
            String assigneeUsername = getSelectedAssigneeUsername(assigneeSpinner);
            TaskManager.TaskResult result;

            if (editing) {
                result = taskManager.updateTask(
                        task.getId(),
                        selectedProject == null ? "" : selectedProject.getId(),
                        taskTitleInput.getText().toString(),
                        descriptionInput.getText().toString(),
                        assigneeUsername,
                        (String) statusSpinner.getSelectedItem(),
                        (String) prioritySpinner.getSelectedItem(),
                        startDateInput.getText().toString(),
                        dueDateInput.getText().toString(),
                        tagsInput.getText().toString()
                );
            } else {
                result = taskManager.createTask(
                        selectedProject == null ? "" : selectedProject.getId(),
                        taskTitleInput.getText().toString(),
                        descriptionInput.getText().toString(),
                        assigneeUsername,
                        (String) statusSpinner.getSelectedItem(),
                        (String) prioritySpinner.getSelectedItem(),
                        startDateInput.getText().toString(),
                        dueDateInput.getText().toString(),
                        tagsInput.getText().toString()
                );
            }

            if (!result.isSuccessful()) {
                errorText.setText(result.getMessage());
                return;
            }

            Toast.makeText(this, editing ? "Đã cập nhật công việc." : "Đã tạo công việc.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            renderTasks();
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

    private void renderTasks() {
        tasksContainer.removeAllViews();
        List<TaskItem> tasks = taskManager.getTasks(true);
        boolean hasVisibleTask = false;
        for (TaskItem task : tasks) {
            if (task.isDeleted() != showDeleted) {
                continue;
            }
            if (!canSeeTask(task)) {
                continue;
            }
            if (!matchesFilters(task)) {
                continue;
            }
            tasksContainer.addView(createTaskCard(task));
            hasVisibleTask = true;
        }
        if (!hasVisibleTask) {
            TextView emptyText = new TextView(this);
            emptyText.setText(hasActiveFilters() ? "Không tìm thấy công việc phù hợp." :
                    (showDeleted ? "Thùng rác đang trống." : "Chưa có công việc nào."));
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(15);
            emptyText.setPadding(0, dp(16), 0, 0);
            tasksContainer.addView(emptyText);
        }
    }

    private boolean matchesFilters(TaskItem task) {
        String keyword = searchInput.getText().toString().trim().toLowerCase();
        if (!keyword.isEmpty() && !getSearchText(task).contains(keyword)) {
            return false;
        }

        String selectedStatus = getSelectedFilterValue(statusFilterSpinner);
        if (!selectedStatus.isEmpty() && !selectedStatus.equals(task.getStatus())) {
            return false;
        }

        String selectedPriority = getSelectedFilterValue(priorityFilterSpinner);
        if (!selectedPriority.isEmpty() && !selectedPriority.equals(task.getPriority())) {
            return false;
        }

        String selectedProjectId = getSelectedFilterProjectId();
        return selectedProjectId.isEmpty() || selectedProjectId.equals(task.getProjectId());
    }

    private String getSearchText(TaskItem task) {
        return (task.getTitle() + " "
                + task.getDescription() + " "
                + task.getTags() + " "
                + task.getAssigneeUsername() + " "
                + task.getStatus() + " "
                + task.getPriority() + " "
                + getProjectName(task.getProjectId())).toLowerCase();
    }

    private String getSelectedFilterValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItemPosition() <= 0 || spinner.getSelectedItem() == null) {
            return "";
        }
        return spinner.getSelectedItem().toString();
    }

    private String getSelectedFilterProjectId() {
        if (projectFilterSpinner == null) {
            return "";
        }
        int selectedIndex = projectFilterSpinner.getSelectedItemPosition();
        if (selectedIndex <= 0 || selectedIndex - 1 >= filterProjects.size()) {
            return "";
        }
        return filterProjects.get(selectedIndex - 1).getId();
    }

    private boolean hasActiveFilters() {
        return !searchInput.getText().toString().trim().isEmpty()
                || statusFilterSpinner.getSelectedItemPosition() > 0
                || priorityFilterSpinner.getSelectedItemPosition() > 0
                || projectFilterSpinner.getSelectedItemPosition() > 0;
    }

    private boolean canSeeTask(TaskItem task) {
        if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER) {
            return true;
        }
        return currentUser.getUsername().equals(task.getAssigneeUsername());
    }

    private MaterialCardView createTaskCard(TaskItem task) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface_white));
        card.setRadius(dp(12));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(R.color.card_stroke));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(view -> showTaskDetailDialog(task));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView titleText = new TextView(this);
        titleText.setText(task.getTitle());
        titleText.setTextColor(getColor(R.color.auth_title));
        titleText.setTextSize(17);
        titleText.setTypeface(null, Typeface.BOLD);

        TextView metaText = new TextView(this);
        metaText.setText("Project: " + getProjectName(task.getProjectId())
                + "\nAssignee: " + task.getAssigneeUsername()
                + "\nStatus: " + task.getStatus() + " | Priority: " + task.getPriority()
                + "\nDeadline: " + (task.getDueDate().isEmpty() ? "N/A" : task.getDueDate()));
        metaText.setTextColor(getColor(R.color.auth_body));
        metaText.setTextSize(14);
        metaText.setPadding(0, dp(8), 0, 0);

        TextView descText = new TextView(this);
        descText.setText(task.getDescription().isEmpty() ? "Không có mô tả." : task.getDescription());
        descText.setTextColor(getColor(R.color.auth_body));
        descText.setTextSize(14);
        descText.setPadding(0, dp(8), 0, 0);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);

        MaterialButton editButton = new MaterialButton(this);
        editButton.setText("Chi tiết");
        editButton.setAllCaps(false);
        editButton.setOnClickListener(view -> showTaskDetailDialog(task));

        MaterialButton deleteButton = new MaterialButton(this);
        deleteButton.setText(task.isDeleted() ? "Khôi phục" : "Xóa mềm");
        deleteButton.setAllCaps(false);
        int actionColor = task.isDeleted() ? getColor(R.color.success_green) : getColor(R.color.auth_error);
        deleteButton.setTextColor(actionColor);
        deleteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface_white)));
        deleteButton.setStrokeColor(ColorStateList.valueOf(actionColor));
        deleteButton.setStrokeWidth(dp(1));
        deleteButton.setVisibility(currentUser.getRole().canAssignTasks() ? View.VISIBLE : View.GONE);
        deleteButton.setOnClickListener(view -> setTaskDeleted(task, !task.isDeleted()));

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        editParams.setMargins(0, 0, dp(8), 0);
        actions.addView(editButton, editParams);
        actions.addView(deleteButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        content.addView(titleText);
        content.addView(metaText);
        content.addView(descText);
        content.addView(actions);
        card.addView(content);
        return card;
    }

    private String getProjectName(String projectId) {
        for (Project project : projectManager.getProjects()) {
            if (project.getId().equals(projectId)) {
                return project.getName();
            }
        }
        return "N/A";
    }

    private void setTaskDeleted(TaskItem task, boolean deleted) {
        TaskManager.TaskResult result = taskManager.setDeleted(task.getId(), deleted);
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
        if (result.isSuccessful()) {
            renderTasks();
        }
    }

    private void showTaskDetailDialog(TaskItem task) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task_detail);

        TextView titleText = dialog.findViewById(R.id.dialogTaskDetailTitleText);
        TextView metaText = dialog.findViewById(R.id.dialogTaskDetailMetaText);
        TextView descriptionText = dialog.findViewById(R.id.dialogTaskDetailDescriptionText);
        TextView tagsText = dialog.findViewById(R.id.dialogTaskDetailTagsText);
        EditText noteInput = dialog.findViewById(R.id.dialogTaskNoteInput);
        LinearLayout notesContainer = dialog.findViewById(R.id.dialogTaskNotesContainer);
        LinearLayout attachmentsContainer = dialog.findViewById(R.id.dialogTaskAttachmentsContainer);
        Button addNoteButton = dialog.findViewById(R.id.dialogAddTaskNoteButton);
        Button addAttachmentButton = dialog.findViewById(R.id.dialogAddTaskAttachmentButton);
        Button editFullButton = dialog.findViewById(R.id.dialogEditFullTaskButton);
        Button closeButton = dialog.findViewById(R.id.dialogCloseTaskDetailButton);

        renderTaskDetailContent(task, titleText, metaText, descriptionText, tagsText);
        renderTaskNotes(task.getId(), notesContainer);
        renderTaskAttachments(task.getId(), attachmentsContainer);

        addNoteButton.setOnClickListener(view -> {
            TaskNoteManager.NoteResult result = noteManager.addNote(
                    task.getId(),
                    currentUser.getUsername(),
                    noteInput.getText().toString()
            );
            if (!result.isSuccessful()) {
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            noteInput.setText("");
            renderTaskNotes(task.getId(), notesContainer);
        });
        addAttachmentButton.setOnClickListener(view -> openAttachmentPicker(task.getId(), attachmentsContainer));
        editFullButton.setVisibility(currentUser.getRole().canAssignTasks() ? View.VISIBLE : View.GONE);
        editFullButton.setOnClickListener(view -> {
            dialog.dismiss();
            showTaskDialog(taskManager.findTaskById(task.getId()));
        });
        closeButton.setOnClickListener(view -> dialog.dismiss());

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

    private void renderTaskDetailContent(
            TaskItem task,
            TextView titleText,
            TextView metaText,
            TextView descriptionText,
            TextView tagsText
    ) {
        titleText.setText(task.getTitle());
        metaText.setText("Dự án: " + getProjectName(task.getProjectId())
                + "\nNgười thực hiện: " + task.getAssigneeUsername()
                + "\nTrạng thái: " + task.getStatus()
                + "\nƯu tiên: " + task.getPriority()
                + "\nNgày bắt đầu: " + (task.getStartDate().isEmpty() ? "N/A" : task.getStartDate())
                + "\nHạn hoàn thành: " + (task.getDueDate().isEmpty() ? "N/A" : task.getDueDate()));
        descriptionText.setText(task.getDescription().isEmpty() ? "Không có mô tả." : task.getDescription());
        tagsText.setText(task.getTags().isEmpty() ? "Tags: N/A" : "Tags: " + task.getTags());
    }

    private void renderTaskNotes(String taskId, LinearLayout notesContainer) {
        notesContainer.removeAllViews();
        List<TaskNote> notes = noteManager.getNotes(taskId);
        if (notes.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có ghi chú nào.");
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(15);
            emptyText.setPadding(0, dp(10), 0, 0);
            notesContainer.addView(emptyText);
            return;
        }

        for (TaskNote note : notes) {
            MaterialCardView card = new MaterialCardView(this);
            card.setCardBackgroundColor(getColor(R.color.surface_white));
            card.setRadius(dp(10));
            card.setCardElevation(dp(1));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(getColor(R.color.card_stroke));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dp(8), 0, 0);
            card.setLayoutParams(cardParams);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(12), dp(10), dp(12), dp(10));

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
            notesContainer.addView(card);
        }
    }

    private void openAttachmentPicker(String taskId, LinearLayout attachmentsContainer) {
        pendingAttachmentTaskId = taskId;
        activeAttachmentsContainer = attachmentsContainer;

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn file đính kèm"), REQUEST_PICK_TASK_ATTACHMENT);
        } catch (ActivityNotFoundException exception) {
            Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fallbackIntent.setType("*/*");
            fallbackIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(Intent.createChooser(fallbackIntent, "Chọn file đính kèm"), REQUEST_PICK_TASK_ATTACHMENT);
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "Không tìm thấy ứng dụng chọn file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_TASK_ATTACHMENT || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        if (pendingAttachmentTaskId == null || pendingAttachmentTaskId.trim().isEmpty()) {
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

        String name = getAttachmentName(uri);
        String mimeType = getContentResolver().getType(uri);
        TaskAttachmentManager.AttachmentResult result = attachmentManager.addAttachment(
                pendingAttachmentTaskId,
                name,
                uri.toString(),
                mimeType
        );
        Toast.makeText(this, result.isSuccessful() ? "Đã thêm file đính kèm." : result.getMessage(), Toast.LENGTH_SHORT).show();
        if (result.isSuccessful() && activeAttachmentsContainer != null) {
            renderTaskAttachments(pendingAttachmentTaskId, activeAttachmentsContainer);
        }
    }

    private void renderTaskAttachments(String taskId, LinearLayout attachmentsContainer) {
        attachmentsContainer.removeAllViews();
        List<TaskAttachment> attachments = attachmentManager.getAttachments(taskId);
        if (attachments.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có file đính kèm.");
            emptyText.setTextColor(getColor(R.color.auth_body));
            emptyText.setTextSize(15);
            emptyText.setPadding(0, dp(10), 0, 0);
            attachmentsContainer.addView(emptyText);
            return;
        }

        for (TaskAttachment attachment : attachments) {
            MaterialCardView card = new MaterialCardView(this);
            card.setCardBackgroundColor(getColor(R.color.surface_white));
            card.setRadius(dp(10));
            card.setCardElevation(dp(1));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(getColor(R.color.card_stroke));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dp(8), 0, 0);
            card.setLayoutParams(cardParams);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView nameText = new TextView(this);
            nameText.setText(attachment.getName());
            nameText.setTextColor(getColor(R.color.auth_title));
            nameText.setTextSize(15);
            nameText.setTypeface(null, Typeface.BOLD);

            TextView metaText = new TextView(this);
            metaText.setText((attachment.getMimeType().isEmpty() ? "File" : attachment.getMimeType())
                    + " - " + attachment.getCreatedAt());
            metaText.setTextColor(getColor(R.color.auth_body));
            metaText.setTextSize(13);
            metaText.setPadding(0, dp(4), 0, 0);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(8), 0, 0);

            MaterialButton openButton = new MaterialButton(this);
            openButton.setText("Mở");
            openButton.setAllCaps(false);
            openButton.setOnClickListener(view -> openAttachment(attachment));

            MaterialButton deleteButton = new MaterialButton(this);
            deleteButton.setText("Xóa");
            deleteButton.setAllCaps(false);
            deleteButton.setTextColor(getColor(R.color.auth_error));
            deleteButton.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.surface_white)));
            deleteButton.setStrokeColor(ColorStateList.valueOf(getColor(R.color.auth_error)));
            deleteButton.setStrokeWidth(dp(1));
            deleteButton.setOnClickListener(view -> {
                TaskAttachmentManager.AttachmentResult result = attachmentManager.deleteAttachment(attachment.getId());
                Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                if (result.isSuccessful()) {
                    renderTaskAttachments(taskId, attachmentsContainer);
                }
            });

            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            openParams.setMargins(0, 0, dp(8), 0);
            actions.addView(openButton, openParams);
            actions.addView(deleteButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            content.addView(nameText);
            content.addView(metaText);
            content.addView(actions);
            card.addView(content);
            attachmentsContainer.addView(card);
        }
    }

    private void openAttachment(TaskAttachment attachment) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(attachment.getUri()), attachment.getMimeType().isEmpty() ? "*/*" : attachment.getMimeType());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Không có ứng dụng mở file này.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getAttachmentName(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || lastPathSegment.trim().isEmpty()) {
            return "File đính kèm";
        }
        int separatorIndex = Math.max(lastPathSegment.lastIndexOf('/'), lastPathSegment.lastIndexOf(':'));
        if (separatorIndex >= 0 && separatorIndex < lastPathSegment.length() - 1) {
            return lastPathSegment.substring(separatorIndex + 1);
        }
        return lastPathSegment;
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

    private class FilterChangeListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            renderTasks();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }
}
