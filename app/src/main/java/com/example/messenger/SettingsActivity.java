package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private SettingsViewModel viewModel;
    private boolean bindingSettings = false;
    private boolean pendingThemeChange = false;

    private SwitchCompat switchNotifications;
    private SwitchCompat switchMessageSound;
    private SwitchCompat switchVibration;
    private SwitchCompat switchDarkTheme;
    private RadioGroup radioGroupTextSize;
    private RadioButton radioSmall;
    private RadioButton radioMedium;
    private RadioButton radioLarge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        bindViews();
        observeSettings();
        setupListeners();
    }

    private void bindViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        switchNotifications = findViewById(R.id.switchNotifications);
        switchMessageSound = findViewById(R.id.switchMessageSound);
        switchVibration = findViewById(R.id.switchVibration);
        switchDarkTheme = findViewById(R.id.switchDarkTheme);

        radioGroupTextSize = findViewById(R.id.radioGroupTextSize);
        radioSmall = findViewById(R.id.radioTextSmall);
        radioMedium = findViewById(R.id.radioTextMedium);
        radioLarge = findViewById(R.id.radioTextLarge);
    }

    private void observeSettings() {
        viewModel.getNotificationsEnabled().observe(this, enabled -> {
            bindingSettings = true;
            switchNotifications.setChecked(Boolean.TRUE.equals(enabled));
            bindingSettings = false;
        });
        viewModel.getMessageSoundEnabled().observe(this, enabled -> {
            bindingSettings = true;
            switchMessageSound.setChecked(Boolean.TRUE.equals(enabled));
            bindingSettings = false;
        });
        viewModel.getVibrationEnabled().observe(this, enabled -> {
            bindingSettings = true;
            switchVibration.setChecked(Boolean.TRUE.equals(enabled));
            bindingSettings = false;
        });
        viewModel.getDarkThemeEnabled().observe(this, enabled -> {
            bindingSettings = true;
            switchDarkTheme.setChecked(Boolean.TRUE.equals(enabled));
            bindingSettings = false;
        });
        viewModel.getTextSize().observe(this, size -> {
            bindingSettings = true;
            if (SettingsViewModel.TEXT_SMALL.equals(size)) {
                radioSmall.setChecked(true);
            } else if (SettingsViewModel.TEXT_LARGE.equals(size)) {
                radioLarge.setChecked(true);
            } else {
                radioMedium.setChecked(true);
            }
            bindingSettings = false;
        });
    }

    private void setupListeners() {
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingSettings) viewModel.setNotificationsEnabled(isChecked);
        });

        switchMessageSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingSettings) viewModel.setMessageSoundEnabled(isChecked);
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!bindingSettings) viewModel.setVibrationEnabled(isChecked);
        });

        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSettings) return;
            viewModel.setDarkThemeEnabled(isChecked);
            pendingThemeChange = true;
            Toast.makeText(this, "Тема выбрана. Нажмите \"Сохранить настройки\"", Toast.LENGTH_SHORT).show();
        });

        radioGroupTextSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (bindingSettings) return;
            if (checkedId == R.id.radioTextSmall) {
                viewModel.setTextSize(SettingsViewModel.TEXT_SMALL);
            } else if (checkedId == R.id.radioTextLarge) {
                viewModel.setTextSize(SettingsViewModel.TEXT_LARGE);
            } else {
                viewModel.setTextSize(SettingsViewModel.TEXT_MEDIUM);
            }
        });

        MaterialButton buttonSaveSettings = findViewById(R.id.buttonSaveSettings);
        buttonSaveSettings.setOnClickListener(v -> {
            boolean dark = Boolean.TRUE.equals(viewModel.getDarkThemeEnabled().getValue());
            ThemePreferences.setDarkThemeEnabled(this, dark);
            AppCompatDelegate.setDefaultNightMode(dark
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
            pendingThemeChange = false;
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
        });

        MaterialButton buttonClearCache = findViewById(R.id.buttonClearCache);
        buttonClearCache.setOnClickListener(v -> {
            viewModel.clearCache();
            ThemePreferences.setDarkThemeEnabled(this, false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Настройки сброшены", Toast.LENGTH_SHORT).show();
        });

        MaterialButton buttonAbout = findViewById(R.id.buttonAbout);
        buttonAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

        TextView textLogout = findViewById(R.id.textLogout);
        textLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }
}
