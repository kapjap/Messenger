package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersActivity extends AppCompatActivity {
    private RecyclerView recyclingView;
    private UsersAdapter usersAdapter;
    private UsersVIewModel vIewModel;
    private TextInputEditText editTextSearch;
    private TextView textViewEmpty;

    private final List<UsersVIewModel.ChatPreview> allChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        initViews();

        vIewModel = new ViewModelProvider(this).get(UsersVIewModel.class);
        observeViewModel();

        usersAdapter.setOnUserClickListener(user -> {
            String currentUserId = vIewModel.getCurrentUserId();
            if (currentUserId == null || user == null || user.getId() == null) return;
            Intent intent = ChatActivity.newIntent(UsersActivity.this, currentUserId, user.getId());
            startActivity(intent);
        });

        usersAdapter.setOnUserLongClickListener(this::showPinPopup);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(s); }
            @Override public void afterTextChanged(Editable s) { }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showPinPopup(UsersVIewModel.ChatPreview preview) {
        PopupMenu popupMenu = new PopupMenu(this, recyclingView);
        String title = preview.isPinned() ? "Открепить чат" : "Закрепить чат";
        popupMenu.getMenu().add(0, 1, 0, title);
        popupMenu.setOnMenuItemClickListener(item -> {
            User user = preview.getUser();
            if (item.getItemId() == 1 && user != null && user.getId() != null) {
                vIewModel.togglePinned(user.getId());
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void initViews() {
        recyclingView = findViewById(R.id.recyclerViewUsers);
        editTextSearch = findViewById(R.id.editTextSearch);
        usersAdapter = new UsersAdapter();
        recyclingView.setAdapter(usersAdapter);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());
    }

    private void observeViewModel() {
        vIewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                Intent intent = LoginActivity.newIntent(UsersActivity.this);
                startActivity(intent);
                finish();
            }
        });

        vIewModel.getChatPreviews().observe(this, previews -> {
            allChats.clear();
            if (previews != null) {
                allChats.addAll(previews);
            }
            applyFilter(editTextSearch.getText());
        });
    }

    private void applyFilter(CharSequence query) {
        String q = query == null ? "" : query.toString().toLowerCase(Locale.getDefault()).trim();
        if (q.isEmpty()) {
            usersAdapter.setChats(new ArrayList<>(allChats));
            updateEmptyState(allChats.isEmpty());
            return;
        }

        List<UsersVIewModel.ChatPreview> filtered = new ArrayList<>();
        for (UsersVIewModel.ChatPreview chat : allChats) {
            User user = chat.getUser();
            String name = (user.getName() + " " + user.getLastName()).toLowerCase(Locale.getDefault());
            if (name.contains(q)) {
                filtered.add(chat);
            }
        }
        usersAdapter.setChats(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        textViewEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    public static Intent newIntent(Context context, String currentUserId) {
        return new Intent(context, UsersActivity.class);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_profile) {
            startActivity(ProfileActivity.newIntent(this));
            return true;
        }
        if (item.getItemId() == R.id.item_pinned_chats) {
            startActivity(PinnedChatsActivity.newIntent(this));
            return true;
        }
        if (item.getItemId() == R.id.item_logout) {
            vIewModel.logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        vIewModel.setUserOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        vIewModel.setUserOnline(false);
    }
}
