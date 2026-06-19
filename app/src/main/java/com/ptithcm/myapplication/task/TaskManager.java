package com.ptithcm.myapplication.task;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskManager {
    private static final String PREFS_NAME = "task_manager_tasks";
    private static final String KEY_TASKS = "tasks";

    private final SharedPreferences preferences;

    public TaskManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<TaskItem> getTasks(boolean includeDeleted) {
        List<TaskItem> tasks = new ArrayList<>();
        String tasksJson = preferences.getString(KEY_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(tasksJson);
            for (int i = 0; i < array.length(); i++) {
                TaskItem task = TaskItem.fromJson(array.getJSONObject(i));
                if (includeDeleted || !task.isDeleted()) {
                    tasks.add(task);
                }
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_TASKS).apply();
            return new ArrayList<>();
        }
        return tasks;
    }

    public TaskItem findTaskById(String taskId) {
        for (TaskItem task : getTasks(true)) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public TaskResult updateStatus(String taskId, String status) {
        TaskItem task = findTaskById(taskId);
        if (task == null) {
            return TaskResult.failure("Không tìm thấy công việc.");
        }
        return updateTask(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssigneeUsername(),
                status,
                task.getPriority(),
                task.getStartDate(),
                task.getDueDate(),
                task.getTags()
        );
    }

    public TaskResult createTask(
            String projectId,
            String title,
            String description,
            String assigneeUsername,
            String status,
            String priority,
            String startDate,
            String dueDate,
            String tags
    ) {
        String normalizedTitle = normalize(title);
        if (projectId == null || projectId.trim().isEmpty()) {
            return TaskResult.failure("Vui lòng chọn dự án.");
        }
        if (normalizedTitle.isEmpty()) {
            return TaskResult.failure("Tiêu đề công việc không được để trống.");
        }
        if (assigneeUsername == null || assigneeUsername.trim().isEmpty()) {
            return TaskResult.failure("Vui lòng chọn người thực hiện.");
        }

        TaskItem task = new TaskItem(
                UUID.randomUUID().toString(),
                projectId,
                normalizedTitle,
                normalize(description),
                assigneeUsername.trim().toLowerCase(),
                normalizeChoice(status, TaskStatus.TODO),
                normalizeChoice(priority, TaskPriority.MEDIUM),
                normalize(startDate),
                normalize(dueDate),
                normalize(tags),
                false
        );
        List<TaskItem> tasks = getTasks(true);
        tasks.add(task);
        saveTasks(tasks);
        return TaskResult.success(task);
    }

    public TaskResult updateTask(
            String taskId,
            String projectId,
            String title,
            String description,
            String assigneeUsername,
            String status,
            String priority,
            String startDate,
            String dueDate,
            String tags
    ) {
        String normalizedTitle = normalize(title);
        if (normalizedTitle.isEmpty()) {
            return TaskResult.failure("Tiêu đề công việc không được để trống.");
        }
        if (projectId == null || projectId.trim().isEmpty()) {
            return TaskResult.failure("Vui lòng chọn dự án.");
        }
        if (assigneeUsername == null || assigneeUsername.trim().isEmpty()) {
            return TaskResult.failure("Vui lòng chọn người thực hiện.");
        }

        List<TaskItem> tasks = getTasks(true);
        for (int i = 0; i < tasks.size(); i++) {
            TaskItem existingTask = tasks.get(i);
            if (!existingTask.getId().equals(taskId)) {
                continue;
            }

            TaskItem updatedTask = new TaskItem(
                    existingTask.getId(),
                    projectId,
                    normalizedTitle,
                    normalize(description),
                    assigneeUsername.trim().toLowerCase(),
                    normalizeChoice(status, TaskStatus.TODO),
                    normalizeChoice(priority, TaskPriority.MEDIUM),
                    normalize(startDate),
                    normalize(dueDate),
                    normalize(tags),
                    existingTask.isDeleted()
            );
            tasks.set(i, updatedTask);
            saveTasks(tasks);
            return TaskResult.success(updatedTask);
        }
        return TaskResult.failure("Không tìm thấy công việc cần cập nhật.");
    }

    public TaskResult setDeleted(String taskId, boolean deleted) {
        List<TaskItem> tasks = getTasks(true);
        for (int i = 0; i < tasks.size(); i++) {
            TaskItem task = tasks.get(i);
            if (task.getId().equals(taskId)) {
                TaskItem updatedTask = task.withDeleted(deleted);
                tasks.set(i, updatedTask);
                saveTasks(tasks);
                return TaskResult.success(updatedTask);
            }
        }
        return TaskResult.failure("Không tìm thấy công việc.");
    }

    private void saveTasks(List<TaskItem> tasks) {
        JSONArray array = new JSONArray();
        try {
            for (TaskItem task : tasks) {
                array.put(task.toJson());
            }
            preferences.edit().putString(KEY_TASKS, array.toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save tasks", exception);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeChoice(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    public static class TaskResult {
        private final boolean successful;
        private final String message;
        private final TaskItem task;

        private TaskResult(boolean successful, String message, TaskItem task) {
            this.successful = successful;
            this.message = message;
            this.task = task;
        }

        public static TaskResult success(TaskItem task) {
            return new TaskResult(true, "Thành công.", task);
        }

        public static TaskResult failure(String message) {
            return new TaskResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public TaskItem getTask() {
            return task;
        }
    }
}

