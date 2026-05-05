package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        SwitchCompat switchDarkTheme = findViewById(R.id.switchDarkTheme);
        switchDarkTheme.setChecked(ThemePreferences.isDarkThemeEnabled(this));

        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ThemePreferences.setDarkThemeEnabled(this, isChecked);
            ThemePreferences.applyTheme(this);
        });
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }
}
