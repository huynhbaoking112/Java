package com.ptithcm.myapplication.task;

import android.content.Context;
import android.content.SharedPreferences;

import com.ptithcm.myapplication.auth.User;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TaskActivityLogManager {
    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_NOTE_ADDED = "NOTE_ADDED";
    public static final String ACTION_ATTACHMENT_ADDED = "ATTACHMENT_ADDED";
    public static final String ACTION_ATTACHMENT_DELETED = "ATTACHMENT_DELETED";
    public static final String ACTION_DELETED = "DELETED";
    public static final String ACTION_RESTORED = "RESTORED";

    private static final String PREFS_NAME = "task_manager_task_activity_logs";
    private static final String KEY_LOGS = "logs";

    private final SharedPreferences preferences;

    public TaskActivityLogManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<TaskActivityLog> getLogs(String taskId) {
        List<TaskActivityLog> logs = new ArrayList<>();
        for (TaskActivityLog log : getAllLogs()) {
            if (log.getTaskId().equals(taskId)) {
                logs.add(log);
            }
        }
        Collections.reverse(logs);
        return logs;
    }

    public TaskActivityLog addLog(String taskId, User actor, String action, String message) {
        TaskActivityLog log = new TaskActivityLog(
                UUID.randomUUID().toString(),
                normalize(taskId),
                actor == null ? "system" : actor.getUsername(),
                actor == null ? "SYSTEM" : actor.getRole().name(),
                normalize(action),
                normalize(message),
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())
        );

        List<TaskActivityLog> logs = getAllLogs();
        logs.add(log);
        saveLogs(logs);
        return log;
    }

    private List<TaskActivityLog> getAllLogs() {
        List<TaskActivityLog> logs = new ArrayList<>();
        String json = preferences.getString(KEY_LOGS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                logs.add(TaskActivityLog.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_LOGS).apply();
            return new ArrayList<>();
        }
        return logs;
    }

    private void saveLogs(List<TaskActivityLog> logs) {
        JSONArray array = new JSONArray();
        try {
            for (TaskActivityLog log : logs) {
                array.put(log.toJson());
            }
            preferences.edit().putString(KEY_LOGS, array.toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save task activity logs", exception);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
