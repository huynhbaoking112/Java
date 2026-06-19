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

public class TaskNoteManager {
    private static final String PREFS_NAME = "task_manager_task_notes";
    private static final String KEY_NOTES = "notes";

    private final SharedPreferences preferences;

    public TaskNoteManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<TaskNote> getNotes(String taskId) {
        List<TaskNote> notes = new ArrayList<>();
        String notesJson = preferences.getString(KEY_NOTES, "[]");
        try {
            JSONArray array = new JSONArray(notesJson);
            for (int i = 0; i < array.length(); i++) {
                TaskNote note = TaskNote.fromJson(array.getJSONObject(i));
                if (note.getTaskId().equals(taskId)) {
                    notes.add(note);
                }
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_NOTES).apply();
            return new ArrayList<>();
        }
        return notes;
    }

    public NoteResult addNote(String taskId, String authorUsername, String content) {
        String normalizedContent = normalize(content);
        if (normalizedContent.isEmpty()) {
            return NoteResult.failure("Nội dung ghi chú không được để trống.");
        }

        TaskNote note = new TaskNote(
                UUID.randomUUID().toString(),
                taskId,
                authorUsername,
                normalizedContent,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date())
        );

        List<TaskNote> notes = getAllNotes();
        notes.add(note);
        saveNotes(notes);
        return NoteResult.success(note);
    }

    private List<TaskNote> getAllNotes() {
        List<TaskNote> notes = new ArrayList<>();
        String notesJson = preferences.getString(KEY_NOTES, "[]");
        try {
            JSONArray array = new JSONArray(notesJson);
            for (int i = 0; i < array.length(); i++) {
                notes.add(TaskNote.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_NOTES).apply();
            return new ArrayList<>();
        }
        return notes;
    }

    private void saveNotes(List<TaskNote> notes) {
        JSONArray array = new JSONArray();
        try {
            for (TaskNote note : notes) {
                array.put(note.toJson());
            }
            preferences.edit().putString(KEY_NOTES, array.toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save task notes", exception);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static class NoteResult {
        private final boolean successful;
        private final String message;
        private final TaskNote note;

        private NoteResult(boolean successful, String message, TaskNote note) {
            this.successful = successful;
            this.message = message;
            this.note = note;
        }

        public static NoteResult success(TaskNote note) {
            return new NoteResult(true, "Thành công.", note);
        }

        public static NoteResult failure(String message) {
            return new NoteResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public TaskNote getNote() {
            return note;
        }
    }
}
