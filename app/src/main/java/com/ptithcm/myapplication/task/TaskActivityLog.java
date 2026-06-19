package com.ptithcm.myapplication.task;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskActivityLog {
    private final String id;
    private final String taskId;
    private final String actorUsername;
    private final String actorRole;
    private final String action;
    private final String message;
    private final String createdAt;

    public TaskActivityLog(
            String id,
            String taskId,
            String actorUsername,
            String actorRole,
            String action,
            String message,
            String createdAt
    ) {
        this.id = id;
        this.taskId = taskId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("taskId", taskId);
        object.put("actorUsername", actorUsername);
        object.put("actorRole", actorRole);
        object.put("action", action);
        object.put("message", message);
        object.put("createdAt", createdAt);
        return object;
    }

    public static TaskActivityLog fromJson(JSONObject object) {
        return new TaskActivityLog(
                object.optString("id"),
                object.optString("taskId"),
                object.optString("actorUsername"),
                object.optString("actorRole"),
                object.optString("action"),
                object.optString("message"),
                object.optString("createdAt")
        );
    }
}
