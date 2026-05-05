package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;

    private TextView textViewAvatar;
    private TextView textViewName;
    private TextView textViewStatus;
    private TextView textViewEmail;
    private TextView textViewAge;
    private TextView textViewAbout;
    private TextView textViewCreatedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        initViews();

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        observeViewModel();
        setupClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        textViewAvatar = findViewById(R.id.textViewAvatar);
        textViewName = findViewById(R.id.textViewName);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewEmail = findViewById(R.id.textViewEmailValue);
        textViewAge = findViewById(R.id.textViewAgeValue);
        textViewAbout = findViewById(R.id.textViewAboutValue);
        textViewCreatedAt = findViewById(R.id.textViewCreatedAtValue);
    }

    private void observeViewModel() {
        viewModel.getFirebaseUser().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                Intent intent = LoginActivity.newIntent(ProfileActivity.this);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getProfileUser().observe(this, user -> {
            if (user == null) return;
            bindUser(user);
        });
    }

    private void setupClickListeners() {
        Button buttonEditProfile = findViewById(R.id.buttonEditProfile);
        Button buttonLogout = findViewById(R.id.buttonLogout);

        buttonEditProfile.setOnClickListener(v ->
                startActivity(EditProfileActivity.newIntent(ProfileActivity.this)));

        buttonLogout.setOnClickListener(v -> viewModel.logout());
    }

    private void bindUser(User user) {
        String fullName = (user.getName() + " " + user.getLastName()).trim();
        if (TextUtils.isEmpty(fullName)) {
            fullName = "Без имени";
        }

        textViewName.setText(fullName);
        textViewAvatar.setText(getInitials(user));
        textViewEmail.setText(TextUtils.isEmpty(user.getEmail()) ? "—" : user.getEmail());
        textViewAge.setText(user.getAge() > 0 ? String.valueOf(user.getAge()) : "—");
        textViewAbout.setText(TextUtils.isEmpty(user.getAbout()) ? "—" : user.getAbout());
        textViewCreatedAt.setText(formatDate(user.getCreatedAt()));

        if (user.isOnline()) {
            textViewStatus.setText("online");
            textViewStatus.setTextColor(getColor(R.color.online_green));
        } else {
            textViewStatus.setText("offline");
            textViewStatus.setTextColor(getColor(R.color.offline_red));
        }
    }

    private String formatDate(long createdAt) {
        if (createdAt <= 0) return "—";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(createdAt));
    }

    private String getInitials(User user) {
        String first = user.getName();
        String last = user.getLastName();
        StringBuilder sb = new StringBuilder();

        if (!TextUtils.isEmpty(first)) {
            sb.append(first.substring(0, 1).toUpperCase(Locale.getDefault()));
        }
        if (!TextUtils.isEmpty(last)) {
            sb.append(last.substring(0, 1).toUpperCase(Locale.getDefault()));
        }

        if (sb.length() == 0 && !TextUtils.isEmpty(user.getEmail())) {
            sb.append(user.getEmail().substring(0, 1).toUpperCase(Locale.getDefault()));
        }

        return sb.length() == 0 ? "?" : sb.toString();
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, ProfileActivity.class);
    }
}
