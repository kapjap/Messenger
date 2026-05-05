package com.example.messenger;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";
    private static final String KEY_MESSAGE_SOUND = "message_sound_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_DARK_THEME = "dark_theme_enabled";
    private static final String KEY_TEXT_SIZE = "text_size";

    public static final String TEXT_SMALL = "small";
    public static final String TEXT_MEDIUM = "medium";
    public static final String TEXT_LARGE = "large";

    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSoundEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> darkThemeEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> textSize = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSettings();
    }

    private void loadSettings() {
        notificationsEnabled.setValue(sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true));
        messageSoundEnabled.setValue(sharedPreferences.getBoolean(KEY_MESSAGE_SOUND, true));
        vibrationEnabled.setValue(sharedPreferences.getBoolean(KEY_VIBRATION, true));
        darkThemeEnabled.setValue(sharedPreferences.getBoolean(KEY_DARK_THEME, false));
        textSize.setValue(sharedPreferences.getString(KEY_TEXT_SIZE, TEXT_MEDIUM));
    }

    public LiveData<Boolean> getNotificationsEnabled() { return notificationsEnabled; }
    public LiveData<Boolean> getMessageSoundEnabled() { return messageSoundEnabled; }
    public LiveData<Boolean> getVibrationEnabled() { return vibrationEnabled; }
    public LiveData<Boolean> getDarkThemeEnabled() { return darkThemeEnabled; }
    public LiveData<String> getTextSize() { return textSize; }

    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
        notificationsEnabled.setValue(enabled);
    }

    public void setMessageSoundEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_MESSAGE_SOUND, enabled).apply();
        messageSoundEnabled.setValue(enabled);
    }

    public void setVibrationEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_VIBRATION, enabled).apply();
        vibrationEnabled.setValue(enabled);
    }

    public void setDarkThemeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
        darkThemeEnabled.setValue(enabled);
    }

    public void setTextSize(@NonNull String size) {
        sharedPreferences.edit().putString(KEY_TEXT_SIZE, size).apply();
        textSize.setValue(size);
    }

    public void clearCache() {
        sharedPreferences.edit().remove(KEY_NOTIFICATIONS)
                .remove(KEY_MESSAGE_SOUND)
                .remove(KEY_VIBRATION)
                .remove(KEY_DARK_THEME)
                .remove(KEY_TEXT_SIZE)
                .apply();
        loadSettings();
    }
}
