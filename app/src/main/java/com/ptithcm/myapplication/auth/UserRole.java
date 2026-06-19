package com.ptithcm.myapplication.auth;

public enum UserRole {
    ADMIN("Admin"),
    MANAGER("Manager"),
    MEMBER("Member");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }

    public boolean canManageProjects() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canAssignTasks() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canViewReports() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean canUpdateAssignedTasks() {
        return true;
    }

    public static UserRole fromValue(String value) {
        if (value == null) {
            return MEMBER;
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value) || role.displayName.equalsIgnoreCase(value)) {
                return role;
            }
        }
        return MEMBER;
    }
}
