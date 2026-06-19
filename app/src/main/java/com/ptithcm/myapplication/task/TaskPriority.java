package com.ptithcm.myapplication.task;

public final class TaskPriority {
    public static final String LOW = "Low";
    public static final String MEDIUM = "Medium";
    public static final String HIGH = "High";

    private TaskPriority() {
    }

    public static String[] values() {
        return new String[]{LOW, MEDIUM, HIGH};
    }
}
