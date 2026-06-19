package com.ptithcm.myapplication.project;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProjectManager {
    private static final String PREFS_NAME = "task_manager_projects";
    private static final String KEY_PROJECTS = "projects";

    private final SharedPreferences preferences;

    public ProjectManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Project> getProjects() {
        List<Project> projects = new ArrayList<>();
        String projectsJson = preferences.getString(KEY_PROJECTS, "[]");
        try {
            JSONArray array = new JSONArray(projectsJson);
            for (int i = 0; i < array.length(); i++) {
                projects.add(Project.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_PROJECTS).apply();
            return new ArrayList<>();
        }
        return projects;
    }

    public ProjectResult createProject(
            String name,
            String description,
            String startDate,
            String dueDate,
            String managerUsername,
            List<String> memberUsernames
    ) {
        String normalizedName = normalize(name);
        if (normalizedName.isEmpty()) {
            return ProjectResult.failure("Tên dự án không được để trống.");
        }
        if (managerUsername == null || managerUsername.trim().isEmpty()) {
            return ProjectResult.failure("Vui lòng chọn manager cho dự án.");
        }

        Project project = new Project(
                UUID.randomUUID().toString(),
                normalizedName,
                normalize(description),
                normalize(startDate),
                normalize(dueDate),
                managerUsername.trim().toLowerCase(),
                uniqueMembers(memberUsernames, managerUsername)
        );

        List<Project> projects = getProjects();
        projects.add(project);
        saveProjects(projects);
        return ProjectResult.success(project);
    }

    public ProjectResult updateProject(
            String projectId,
            String name,
            String description,
            String startDate,
            String dueDate,
            String managerUsername,
            List<String> memberUsernames
    ) {
        String normalizedName = normalize(name);
        if (normalizedName.isEmpty()) {
            return ProjectResult.failure("Tên dự án không được để trống.");
        }
        if (managerUsername == null || managerUsername.trim().isEmpty()) {
            return ProjectResult.failure("Vui lòng chọn manager cho dự án.");
        }

        List<Project> projects = getProjects();
        for (int i = 0; i < projects.size(); i++) {
            Project project = projects.get(i);
            if (!project.getId().equals(projectId)) {
                continue;
            }

            Project updatedProject = new Project(
                    projectId,
                    normalizedName,
                    normalize(description),
                    normalize(startDate),
                    normalize(dueDate),
                    managerUsername.trim().toLowerCase(),
                    uniqueMembers(memberUsernames, managerUsername)
            );
            projects.set(i, updatedProject);
            saveProjects(projects);
            return ProjectResult.success(updatedProject);
        }

        return ProjectResult.failure("Không tìm thấy dự án cần cập nhật.");
    }

    public ProjectResult deleteProject(String projectId) {
        List<Project> projects = getProjects();
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(projectId)) {
                Project deletedProject = projects.remove(i);
                saveProjects(projects);
                return ProjectResult.success(deletedProject);
            }
        }
        return ProjectResult.failure("Không tìm thấy dự án cần xóa.");
    }

    private void saveProjects(List<Project> projects) {
        JSONArray array = new JSONArray();
        try {
            for (Project project : projects) {
                array.put(project.toJson());
            }
            preferences.edit().putString(KEY_PROJECTS, array.toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save projects", exception);
        }
    }

    private List<String> uniqueMembers(List<String> memberUsernames, String managerUsername) {
        Set<String> usernames = new LinkedHashSet<>();
        if (managerUsername != null && !managerUsername.trim().isEmpty()) {
            usernames.add(managerUsername.trim().toLowerCase());
        }
        if (memberUsernames != null) {
            for (String username : memberUsernames) {
                if (username != null && !username.trim().isEmpty()) {
                    usernames.add(username.trim().toLowerCase());
                }
            }
        }
        return new ArrayList<>(usernames);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static class ProjectResult {
        private final boolean successful;
        private final String message;
        private final Project project;

        private ProjectResult(boolean successful, String message, Project project) {
            this.successful = successful;
            this.message = message;
            this.project = project;
        }

        public static ProjectResult success(Project project) {
            return new ProjectResult(true, "Thành công.", project);
        }

        public static ProjectResult failure(String message) {
            return new ProjectResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public Project getProject() {
            return project;
        }
    }
}

