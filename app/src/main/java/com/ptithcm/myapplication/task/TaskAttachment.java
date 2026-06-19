package com.ptithcm.myapplication.task;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskAttachment {
    private final String id;
    private final String taskId;
    private final String name;
    private final String uri;
    private final String mimeType;
    private final String createdAt;

    public TaskAttachment(String id, String taskId, String name, String uri, String mimeType, String createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.name = name;
        this.uri = uri;
        this.mimeType = mimeType;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("taskId", taskId);
        object.put("name", name);
        object.put("uri", uri);
        object.put("mimeType", mimeType);
        object.put("createdAt", createdAt);
        return object;
    }

    public static TaskAttachment fromJson(JSONObject object) {
        return new TaskAttachment(
                object.optString("id"),
                object.optString("taskId"),
                object.optString("name"),
                object.optString("uri"),
                object.optString("mimeType"),
                object.optString("createdAt")
        );
    }
}
