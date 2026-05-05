package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

public class EditProfileActivity extends AppCompatActivity {

    private EditProfileViewModel viewModel;

    private EditText editTextName;
    private EditText editTextLastName;
    private EditText editTextAge;
    private EditText editTextAbout;
    private Button buttonSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        initViews();

        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        observeViewModel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        editTextName = findViewById(R.id.editTextName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextAge = findViewById(R.id.editTextAge);
        editTextAbout = findViewById(R.id.editTextAbout);
        buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(v -> onSaveClicked());
    }

    private void observeViewModel() {
        viewModel.getProfileUser().observe(this, user -> {
            if (user == null) return;
            editTextName.setText(user.getName());
            editTextLastName.setText(user.getLastName());
            if (user.getAge() > 0) {
                editTextAge.setText(String.valueOf(user.getAge()));
            }
            editTextAbout.setText(user.getAbout());
        });

        viewModel.getIsSaving().observe(this, isSaving -> buttonSave.setEnabled(Boolean.FALSE.equals(isSaving)));

        viewModel.getSaveSuccess().observe(this, isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                Toast.makeText(this, "Профиль успешно обновлён", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getSaveError().observe(this, errorMessage -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSaveClicked() {
        String name = editTextName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String ageText = editTextAge.getText().toString().trim();
        String about = editTextAbout.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Введите имя");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            editTextLastName.setError("Введите фамилию");
            isValid = false;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            editTextAge.setError("Введите корректный возраст");
            return;
        }

        if (!viewModel.isValidAge(age)) {
            editTextAge.setError("Возраст должен быть от 1 до 120");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        viewModel.saveProfile(name, lastName, age, about);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, EditProfileActivity.class);
    }
}
