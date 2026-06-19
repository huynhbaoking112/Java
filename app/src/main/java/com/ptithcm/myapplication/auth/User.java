package com.ptithcm.myapplication.auth;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private final String username;
    private final String fullName;
    private final UserRole role;
    private final String passwordHash;
    private final String avatarUri;

    public User(String username, String fullName, UserRole role, String passwordHash) {
        this(username, fullName, role, passwordHash, "");
    }

    public User(String username, String fullName, UserRole role, String passwordHash, String avatarUri) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.avatarUri = avatarUri == null ? "" : avatarUri;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public User withPasswordHash(String newPasswordHash) {
        return new User(username, fullName, role, newPasswordHash, avatarUri);
    }

    public User withProfile(String newFullName, UserRole newRole) {
        return new User(username, newFullName, newRole, passwordHash, avatarUri);
    }

    public User withPersonalProfile(String newFullName, String newAvatarUri) {
        return new User(username, newFullName, role, passwordHash, newAvatarUri);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("username", username);
        object.put("fullName", fullName);
        object.put("role", role.name());
        object.put("passwordHash", passwordHash);
        object.put("avatarUri", avatarUri);
        return object;
    }

    public static User fromJson(JSONObject object) throws JSONException {
        return new User(
                object.getString("username"),
                object.optString("fullName", object.getString("username")),
                UserRole.fromValue(object.optString("role")),
                object.getString("passwordHash"),
                object.optString("avatarUri")
        );
    }
}
