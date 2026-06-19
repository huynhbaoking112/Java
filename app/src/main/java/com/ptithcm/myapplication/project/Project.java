package com.ptithcm.myapplication.project;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private final String id;
    private final String name;
    private final String description;
    private final String startDate;
    private final String dueDate;
    private final String managerUsername;
    private final List<String> memberUsernames;

    public Project(
            String id,
            String name,
            String description,
            String startDate,
            String dueDate,
            String managerUsername,
            List<String> memberUsernames
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.managerUsername = managerUsername;
        this.memberUsernames = new ArrayList<>(memberUsernames);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getManagerUsername() {
        return managerUsername;
    }

    public List<String> getMemberUsernames() {
        return new ArrayList<>(memberUsernames);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("name", name);
        object.put("description", description);
        object.put("startDate", startDate);
        object.put("dueDate", dueDate);
        object.put("managerUsername", managerUsername);

        JSONArray members = new JSONArray();
        for (String username : memberUsernames) {
            members.put(username);
        }
        object.put("memberUsernames", members);
        return object;
    }

    public static Project fromJson(JSONObject object) throws JSONException {
        JSONArray membersJson = object.optJSONArray("memberUsernames");
        List<String> members = new ArrayList<>();
        if (membersJson != null) {
            for (int i = 0; i < membersJson.length(); i++) {
                members.add(membersJson.getString(i));
            }
        }

        return new Project(
                object.getString("id"),
                object.optString("name"),
                object.optString("description"),
                object.optString("startDate"),
                object.optString("dueDate"),
                object.optString("managerUsername"),
                members
        );
    }
}
