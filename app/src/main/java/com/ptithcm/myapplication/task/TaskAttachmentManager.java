package com.ptithcm.myapplication.task;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TaskAttachmentManager {
    private static final String PREFS_NAME = "task_manager_task_attachments";
    private static final String KEY_ATTACHMENTS = "attachments";

    private final SharedPreferences preferences;

    public TaskAttachmentManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<TaskAttachment> getAttachments(String taskId) {
        List<TaskAttachment> attachments = new ArrayList<>();
        for (TaskAttachment attachment : getAllAttachments()) {
            if (attachment.getTaskId().equals(taskId)) {
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    public AttachmentResult addAttachment(String taskId, String name, String uri, String mimeType) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return AttachmentResult.failure("Không tìm thấy công việc.");
        }
        if (uri == null || uri.trim().isEmpty()) {
            return AttachmentResult.failure("Không tìm thấy file đính kèm.");
        }

        TaskAttachment attachment = new TaskAttachment(
                UUID.randomUUID().toString(),
                taskId,
                normalizeName(name),
                uri.trim(),
                mimeType == null ? "" : mimeType,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())
        );

        List<TaskAttachment> attachments = getAllAttachments();
        attachments.add(attachment);
        saveAttachments(attachments);
        return AttachmentResult.success(attachment);
    }

    public AttachmentResult deleteAttachment(String attachmentId) {
        List<TaskAttachment> attachments = getAllAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).getId().equals(attachmentId)) {
                TaskAttachment removed = attachments.remove(i);
                saveAttachments(attachments);
                return AttachmentResult.success(removed);
            }
        }
        return AttachmentResult.failure("Không tìm thấy file đính kèm.");
    }

    private List<TaskAttachment> getAllAttachments() {
        List<TaskAttachment> attachments = new ArrayList<>();
        String json = preferences.getString(KEY_ATTACHMENTS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                attachments.add(TaskAttachment.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_ATTACHMENTS).apply();
            return new ArrayList<>();
        }
        return attachments;
    }

    private void saveAttachments(List<TaskAttachment> attachments) {
        JSONArray array = new JSONArray();
        try {
            for (TaskAttachment attachment : attachments) {
                array.put(attachment.toJson());
            }
            preferences.edit().putString(KEY_ATTACHMENTS, array.toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save task attachments", exception);
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "File đính kèm";
        }
        return name.trim();
    }

    public static class AttachmentResult {
        private final boolean successful;
        private final String message;
        private final TaskAttachment attachment;

        private AttachmentResult(boolean successful, String message, TaskAttachment attachment) {
            this.successful = successful;
            this.message = message;
            this.attachment = attachment;
        }

        public static AttachmentResult success(TaskAttachment attachment) {
            return new AttachmentResult(true, "Thành công.", attachment);
        }

        public static AttachmentResult failure(String message) {
            return new AttachmentResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public TaskAttachment getAttachment() {
            return attachment;
        }
    }
}
