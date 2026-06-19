package com.ptithcm.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ptithcm.myapplication.auth.AuthManager;

public class SettingsActivity extends AppCompatActivity {
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        authManager = new AuthManager(this);

        if (!authManager.isLoggedIn()) {
            openLoginScreen();
            return;
        }

        setContentView(R.layout.activity_settings);
        FooterNavigationHelper.bind(this, 0);
        bindThemeOptions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FooterNavigationHelper.bind(this, 0);
    }

    private void bindThemeOptions() {
        RadioGroup themeRadioGroup = findViewById(R.id.themeRadioGroup);
        String savedMode = ThemePreferenceManager.getSavedMode(this);

        if (ThemePreferenceManager.MODE_LIGHT.equals(savedMode)) {
            themeRadioGroup.check(R.id.themeLightRadio);
        } else if (ThemePreferenceManager.MODE_DARK.equals(savedMode)) {
            themeRadioGroup.check(R.id.themeDarkRadio);
        } else {
            themeRadioGroup.check(R.id.themeSystemRadio);
        }

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String nextMode = ThemePreferenceManager.MODE_SYSTEM;
            if (checkedId == R.id.themeLightRadio) {
                nextMode = ThemePreferenceManager.MODE_LIGHT;
            } else if (checkedId == R.id.themeDarkRadio) {
                nextMode = ThemePreferenceManager.MODE_DARK;
            }

            ThemePreferenceManager.setMode(this, nextMode);
            Toast.makeText(this, "Đã cập nhật giao diện.", Toast.LENGTH_SHORT).show();
        });
    }

    private void openLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
