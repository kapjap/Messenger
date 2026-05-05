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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        SwitchCompat switchNotifications = findViewById(R.id.switchNotifications);
        SwitchCompat switchMessageSound = findViewById(R.id.switchMessageSound);
        SwitchCompat switchVibration = findViewById(R.id.switchVibration);
        SwitchCompat switchDarkTheme = findViewById(R.id.switchDarkTheme);

        RadioGroup radioGroupTextSize = findViewById(R.id.radioGroupTextSize);
        RadioButton radioSmall = findViewById(R.id.radioTextSmall);
        RadioButton radioMedium = findViewById(R.id.radioTextMedium);
        RadioButton radioLarge = findViewById(R.id.radioTextLarge);

        MaterialButton buttonClearCache = findViewById(R.id.buttonClearCache);
        MaterialButton buttonAbout = findViewById(R.id.buttonAbout);
        TextView textLogout = findViewById(R.id.textLogout);

        viewModel.getNotificationsEnabled().observe(this, switchNotifications::setChecked);
        viewModel.getMessageSoundEnabled().observe(this, switchMessageSound::setChecked);
        viewModel.getVibrationEnabled().observe(this, switchVibration::setChecked);
        viewModel.getDarkThemeEnabled().observe(this, switchDarkTheme::setChecked);
        viewModel.getTextSize().observe(this, size -> {
            if (SettingsViewModel.TEXT_SMALL.equals(size)) {
                radioSmall.setChecked(true);
            } else if (SettingsViewModel.TEXT_LARGE.equals(size)) {
                radioLarge.setChecked(true);
            } else {
                radioMedium.setChecked(true);
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.setNotificationsEnabled(isChecked));

        switchMessageSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.setMessageSoundEnabled(isChecked));

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.setVibrationEnabled(isChecked));

        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setDarkThemeEnabled(isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        radioGroupTextSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTextSmall) {
                viewModel.setTextSize(SettingsViewModel.TEXT_SMALL);
            } else if (checkedId == R.id.radioTextLarge) {
                viewModel.setTextSize(SettingsViewModel.TEXT_LARGE);
            } else {
                viewModel.setTextSize(SettingsViewModel.TEXT_MEDIUM);
            }
        });

        buttonClearCache.setOnClickListener(v -> {
            viewModel.clearCache();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show();
        });

        buttonAbout.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));

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
