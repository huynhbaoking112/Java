package com.ptithcm.myapplication.task;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskItem {
    private final String id;
    private final String projectId;
    private final String title;
    private final String description;
    private final String assigneeUsername;
    private final String status;
    private final String priority;
    private final String startDate;
    private final String dueDate;
    private final String tags;
    private final boolean deleted;

    public TaskItem(
            String id,
            String projectId,
            String title,
            String description,
            String assigneeUsername,
            String status,
            String priority,
            String startDate,
            String dueDate,
            String tags,
            boolean deleted
    ) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.assigneeUsername = assigneeUsername;
        this.status = status;
        this.priority = priority;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.tags = tags;
        this.deleted = deleted;
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getTags() {
        return tags;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public TaskItem withDeleted(boolean newDeleted) {
        return new TaskItem(
                id,
                projectId,
                title,
                description,
                assigneeUsername,
                status,
                priority,
                startDate,
                dueDate,
                tags,
                newDeleted
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("projectId", projectId);
        object.put("title", title);
        object.put("description", description);
        object.put("assigneeUsername", assigneeUsername);
        object.put("status", status);
        object.put("priority", priority);
        object.put("startDate", startDate);
        object.put("dueDate", dueDate);
        object.put("tags", tags);
        object.put("deleted", deleted);
        return object;
    }

    public static TaskItem fromJson(JSONObject object) {
        return new TaskItem(
                object.optString("id"),
                object.optString("projectId"),
                object.optString("title"),
                object.optString("description"),
                object.optString("assigneeUsername"),
                object.optString("status", TaskStatus.TODO),
                object.optString("priority", TaskPriority.MEDIUM),
                object.optString("startDate"),
                object.optString("dueDate"),
                object.optString("tags"),
                object.optBoolean("deleted", false)
        );
    }
}
