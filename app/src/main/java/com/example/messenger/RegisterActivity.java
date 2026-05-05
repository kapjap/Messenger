package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextName;
    private EditText editTextLastName;
    private EditText editTextAge;
    private Button btnRegister;
    private TextView textViewLogin;
    private RegisrtyViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        initViews();
        viewModel = new ViewModelProvider(this).get(RegisrtyViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        observeViewModel();
        setupClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextName = findViewById(R.id.editTextName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextAge = findViewById(R.id.editTextAge);
        btnRegister = findViewById(R.id.btnRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> submitRegistration());
        textViewLogin.setOnClickListener(v -> {
            startActivity(LoginActivity.newIntent(RegisterActivity.this));
            finish();
        });
    }

    private void submitRegistration() {
        String email = getTrimmedValue(editTextEmail);
        String password = getTrimmedValue(editTextPassword);
        String name = getTrimmedValue(editTextName);
        String lastName = getTrimmedValue(editTextLastName);
        String ageText = getTrimmedValue(editTextAge);

        if (TextUtils.isEmpty(name)) {
            showError("Введите имя");
            return;
        }
        if (TextUtils.isEmpty(lastName)) {
            showError("Введите фамилию");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            showError("Введите электронную почту");
            return;
        }
        if (password.length() < 6) {
            showError("Пароль должен быть не короче 6 символов");
            return;
        }
        if (TextUtils.isEmpty(ageText)) {
            showError("Введите возраст");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showError("Возраст должен быть числом");
            return;
        }

        viewModel.signUp(email, password, name, lastName, age);
    }

    private String getTrimmedValue(EditText editText) {
        return editText.getText().toString().trim();
    }

    private void observeViewModel() {
        viewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                openUsersScreen(firebaseUser);
            }
        });
    }

    private void openUsersScreen(FirebaseUser firebaseUser) {
        Intent intent = UsersActivity.newIntent(RegisterActivity.this, firebaseUser.getUid());
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
