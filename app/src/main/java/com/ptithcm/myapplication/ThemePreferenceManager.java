package com.ptithcm.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferenceManager {
    public static final String MODE_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";

    private static final String PREFS_NAME = "task_manager_theme";
    private static final String KEY_THEME_MODE = "theme_mode";

    private ThemePreferenceManager() {
    }

    public static void applySavedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(getSavedMode(context)));
    }

    public static void setMode(Context context, String mode) {
        String normalizedMode = normalizeMode(mode);
        getPrefs(context)
                .edit()
                .putString(KEY_THEME_MODE, normalizedMode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(toDelegateMode(normalizedMode));
    }

    public static String getSavedMode(Context context) {
        return normalizeMode(getPrefs(context).getString(KEY_THEME_MODE, MODE_SYSTEM));
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String normalizeMode(String mode) {
        if (MODE_LIGHT.equals(mode) || MODE_DARK.equals(mode)) {
            return mode;
        }
        return MODE_SYSTEM;
    }

    private static int toDelegateMode(String mode) {
        if (MODE_LIGHT.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (MODE_DARK.equals(mode)) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
