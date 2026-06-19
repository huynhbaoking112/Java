package com.ptithcm.myapplication.task;

public final class TaskStatus {
    public static final String TODO = "Todo";
    public static final String DOING = "Doing";
    public static final String DONE = "Done";
    public static final String OVERDUE = "Overdue";

    private TaskStatus() {
    }

    public static String[] values() {
        return new String[]{TODO, DOING, DONE, OVERDUE};
    }
}
