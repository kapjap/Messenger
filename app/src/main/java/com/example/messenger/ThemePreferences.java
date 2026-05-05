package com.example.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class ThemePreferences {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";

    private ThemePreferences() {
    }

    public static boolean isDarkThemeEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_THEME, false);
    }

    public static void setDarkThemeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
    }

    public static void applyTheme(Context context) {
        boolean darkThemeEnabled = isDarkThemeEnabled(context);
        AppCompatDelegate.setDefaultNightMode(
                darkThemeEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
