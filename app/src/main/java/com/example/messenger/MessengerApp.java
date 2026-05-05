package com.example.messenger;

import android.app.Application;

public class MessengerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applyTheme(this);
    }
}
