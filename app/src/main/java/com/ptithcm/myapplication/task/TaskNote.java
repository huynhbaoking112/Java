package com.ptithcm.myapplication.task;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskNote {
    private final String id;
    private final String taskId;
    private final String authorUsername;
    private final String content;
    private final String createdAt;

    public TaskNote(String id, String taskId, String authorUsername, String content, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.authorUsername = authorUsername;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("taskId", taskId);
        object.put("authorUsername", authorUsername);
        object.put("content", content);
        object.put("createdAt", createdAt);
        return object;
    }

    public static TaskNote fromJson(JSONObject object) {
        return new TaskNote(
                object.optString("id"),
                object.optString("taskId"),
                object.optString("authorUsername"),
                object.optString("content"),
                object.optString("createdAt")
        );
    }
}
