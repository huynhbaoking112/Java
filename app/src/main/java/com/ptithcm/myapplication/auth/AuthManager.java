package com.ptithcm.myapplication.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class AuthManager {
    private static final String PREFS_NAME = "task_manager_auth";
    private static final String KEY_USERS = "users";
    private static final String KEY_LOGGED_IN_USERNAME = "logged_in_username";
    private static final String PASSWORD_SALT = "PTIT_TASK_MANAGER_AUTH_V1";

    private final SharedPreferences preferences;

    public AuthManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        seedDefaultUsersIfNeeded();
        migrateDefaultUserDisplayNames();
    }

    public AuthResult login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty() || password == null || password.isEmpty()) {
            return AuthResult.failure("Vui lòng nhập đầy đủ username và mật khẩu.");
        }

        User user = findUser(normalizedUsername);
        if (user == null || !user.getPasswordHash().equals(hashPassword(password))) {
            return AuthResult.failure("Username hoặc mật khẩu không đúng.");
        }

        preferences.edit()
                .putString(KEY_LOGGED_IN_USERNAME, user.getUsername())
                .apply();
        return AuthResult.success(user);
    }

    public void logout() {
        preferences.edit()
                .remove(KEY_LOGGED_IN_USERNAME)
                .apply();
    }

    public AuthResult changePassword(String oldPassword, String newPassword) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return AuthResult.failure("Phiên đăng nhập đã hết hạn.");
        }
        if (oldPassword == null || oldPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()) {
            return AuthResult.failure("Vui lòng nhập đầy đủ mật khẩu.");
        }
        if (newPassword.length() < 6) {
            return AuthResult.failure("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!currentUser.getPasswordHash().equals(hashPassword(oldPassword))) {
            return AuthResult.failure("Mật khẩu hiện tại không đúng.");
        }
        if (oldPassword.equals(newPassword)) {
            return AuthResult.failure("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        List<User> users = getUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(currentUser.getUsername())) {
                users.set(i, currentUser.withPasswordHash(hashPassword(newPassword)));
                saveUsers(users);
                return AuthResult.success(users.get(i));
            }
        }
        return AuthResult.failure("Không tìm thấy tài khoản hiện tại.");
    }

    public User getCurrentUser() {
        String username = preferences.getString(KEY_LOGGED_IN_USERNAME, null);
        if (username == null) {
            return null;
        }
        return findUser(username);
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        String usersJson = preferences.getString(KEY_USERS, "[]");
        try {
            JSONArray array = new JSONArray(usersJson);
            for (int i = 0; i < array.length(); i++) {
                users.add(User.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
            preferences.edit().remove(KEY_USERS).apply();
            seedDefaultUsersIfNeeded();
            return getUsers();
        }
        return users;
    }

    public AuthResult createUser(String username, String fullName, UserRole role, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedFullName = normalizeFullName(fullName, normalizedUsername);

        if (normalizedUsername.isEmpty()) {
            return AuthResult.failure("Username không được để trống.");
        }
        if (!normalizedUsername.matches("[a-z0-9._-]{3,24}")) {
            return AuthResult.failure("Username chỉ gồm chữ thường, số, dấu chấm, gạch dưới hoặc gạch ngang; dài 3-24 ký tự.");
        }
        if (password == null || password.length() < 6) {
            return AuthResult.failure("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (findUser(normalizedUsername) != null) {
            return AuthResult.failure("Username đã tồn tại.");
        }

        User newUser = new User(
                normalizedUsername,
                normalizedFullName,
                role == null ? UserRole.MEMBER : role,
                hashPassword(password)
        );
        List<User> users = getUsers();
        users.add(newUser);
        saveUsers(users);
        return AuthResult.success(newUser);
    }

    public AuthResult updateUser(String username, String fullName, UserRole role, String newPassword) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedFullName = normalizeFullName(fullName, normalizedUsername);
        List<User> users = getUsers();

        for (int i = 0; i < users.size(); i++) {
            User existingUser = users.get(i);
            if (!existingUser.getUsername().equals(normalizedUsername)) {
                continue;
            }

            User updatedUser = existingUser.withProfile(
                    normalizedFullName,
                    role == null ? existingUser.getRole() : role
            );
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (newPassword.length() < 6) {
                    return AuthResult.failure("Mật khẩu mới phải có ít nhất 6 ký tự.");
                }
                updatedUser = updatedUser.withPasswordHash(hashPassword(newPassword));
            }

            users.set(i, updatedUser);
            saveUsers(users);
            return AuthResult.success(updatedUser);
        }

        return AuthResult.failure("Không tìm thấy tài khoản cần cập nhật.");
    }

    public AuthResult deleteUser(String username) {
        String normalizedUsername = normalizeUsername(username);
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getUsername().equals(normalizedUsername)) {
            return AuthResult.failure("Không thể xóa tài khoản đang đăng nhập.");
        }

        List<User> users = getUsers();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(normalizedUsername)) {
                User deletedUser = users.remove(i);
                saveUsers(users);
                return AuthResult.success(deletedUser);
            }
        }

        return AuthResult.failure("Không tìm thấy tài khoản cần xóa.");
    }

    private User findUser(String username) {
        String normalizedUsername = normalizeUsername(username);
        for (User user : getUsers()) {
            if (user.getUsername().equals(normalizedUsername)) {
                return user;
            }
        }
        return null;
    }

    private void seedDefaultUsersIfNeeded() {
        if (preferences.contains(KEY_USERS)) {
            return;
        }

        List<User> defaultUsers = new ArrayList<>();
        defaultUsers.add(new User("admin", "Quản trị viên", UserRole.ADMIN, hashPassword("admin123")));
        defaultUsers.add(new User("manager", "Quản lý dự án", UserRole.MANAGER, hashPassword("manager123")));
        defaultUsers.add(new User("member", "Thành viên", UserRole.MEMBER, hashPassword("member123")));
        saveUsers(defaultUsers);
    }

    private void migrateDefaultUserDisplayNames() {
        List<User> users = getUsers();
        boolean changed = false;
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getUsername().equals("admin") && user.getFullName().equals("Quan tri vien")) {
                users.set(i, user.withProfile("Quản trị viên", user.getRole()));
                changed = true;
            } else if (user.getUsername().equals("manager") && user.getFullName().equals("Quan ly du an")) {
                users.set(i, user.withProfile("Quản lý dự án", user.getRole()));
                changed = true;
            } else if (user.getUsername().equals("member") && user.getFullName().equals("Thanh vien")) {
                users.set(i, user.withProfile("Thành viên", user.getRole()));
                changed = true;
            }
        }
        if (changed) {
            saveUsers(users);
        }
    }

    private void saveUsers(List<User> users) {
        JSONArray array = new JSONArray();
        try {
            for (User user : users) {
                array.put(user.toJson());
            }
            preferences.edit()
                    .putString(KEY_USERS, array.toString())
                    .apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Cannot save users", exception);
        }
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase();
    }

    private String normalizeFullName(String fullName, String fallbackUsername) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return fallbackUsername;
        }
        return fullName.trim();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((PASSWORD_SALT + password).getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static class AuthResult {
        private final boolean successful;
        private final String message;
        private final User user;

        private AuthResult(boolean successful, String message, User user) {
            this.successful = successful;
            this.message = message;
            this.user = user;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, "Thành công.", user);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }
}

