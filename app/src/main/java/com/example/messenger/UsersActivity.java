package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsers;
    private UsersAdapter usersAdapter;
    private UsersVIewModel viewModel;
    private TextInputEditText editTextSearch;
    private TextView textViewEmpty;
    private TextView textViewEmptyHint;
    private LinearLayout emptyStateLayout;
    private MaterialButtonToggleGroup toggleGroupFilters;
    private boolean unreadOnly = false;

    private final List<UsersVIewModel.ChatPreview> allChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        initViews();

        viewModel = new ViewModelProvider(this).get(UsersVIewModel.class);
        observeViewModel();
        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        editTextSearch = findViewById(R.id.editTextSearch);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        textViewEmptyHint = findViewById(R.id.textViewEmptyHint);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        toggleGroupFilters = findViewById(R.id.toggleGroupFilters);

        usersAdapter = new UsersAdapter();
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(usersAdapter);
    }

    private void setupListeners() {
        usersAdapter.setOnUserClickListener(user -> {
            String currentUserId = viewModel.getCurrentUserId();
            if (currentUserId == null || user == null || user.getId() == null) {
                Toast.makeText(this, "Не удалось открыть чат", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = ChatActivity.newIntent(UsersActivity.this, currentUserId, user.getId());
            startActivity(intent);
        });

        usersAdapter.setOnUserLongClickListener(this::showPinPopup);

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(s); }
            @Override public void afterTextChanged(Editable s) { }
        });

        toggleGroupFilters.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            unreadOnly = checkedId == R.id.buttonUnread;
            applyFilter(editTextSearch.getText());
        });

        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(v -> showMainMenu(buttonMenu));

        findViewById(R.id.navChats).setOnClickListener(v -> {
            unreadOnly = false;
            toggleGroupFilters.check(R.id.buttonAllChats);
            applyFilter(editTextSearch.getText());
        });
        findViewById(R.id.navGroups).setOnClickListener(v -> startActivity(new Intent(this, GroupChatsActivity.class)));
        findViewById(R.id.navFavorites).setOnClickListener(v -> startActivity(new Intent(this, FavoriteMessagesActivity.class)));
        findViewById(R.id.navActivity).setOnClickListener(v -> startActivity(new Intent(this, ActivityFeedActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(ProfileActivity.newIntent(this)));
    }

    private void observeViewModel() {
        viewModel.getUser().observe(this, firebaseUser -> {
            if (firebaseUser == null) {
                Intent intent = LoginActivity.newIntent(UsersActivity.this);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getChatPreviews().observe(this, previews -> {
            allChats.clear();
            if (previews != null) {
                allChats.addAll(previews);
            }
            applyFilter(editTextSearch.getText());
        });
    }

    private void showMainMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, R.id.item_profile, 0, "Профиль");
        popupMenu.getMenu().add(0, R.id.item_pinned_chats, 1, "Закреплённые");
        popupMenu.getMenu().add(0, R.id.item_settings, 2, "Настройки");
        popupMenu.getMenu().add(0, R.id.item_logout, 3, "Выйти");
        popupMenu.setOnMenuItemClickListener(this::handleMenuItem);
        popupMenu.show();
    }

    private boolean handleMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.item_profile) {
            startActivity(ProfileActivity.newIntent(this));
            return true;
        }
        if (item.getItemId() == R.id.item_pinned_chats) {
            startActivity(PinnedChatsActivity.newIntent(this));
            return true;
        }
        if (item.getItemId() == R.id.item_settings) {
            startActivity(SettingsActivity.newIntent(this));
            return true;
        }
        if (item.getItemId() == R.id.item_logout) {
            viewModel.logout();
            return true;
        }
        return false;
    }

    private void showPinPopup(UsersVIewModel.ChatPreview preview) {
        PopupMenu popupMenu = new PopupMenu(this, recyclerViewUsers);
        String title = preview.isPinned() ? "Открепить чат" : "Закрепить чат";
        popupMenu.getMenu().add(0, 1, 0, title);
        popupMenu.setOnMenuItemClickListener(item -> {
            User user = preview.getUser();
            if (item.getItemId() == 1 && user != null && user.getId() != null) {
                viewModel.togglePinned(user.getId());
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void applyFilter(CharSequence query) {
        String q = query == null ? "" : query.toString().toLowerCase(Locale.getDefault()).trim();

        List<UsersVIewModel.ChatPreview> filtered = new ArrayList<>();
        for (UsersVIewModel.ChatPreview chat : allChats) {
            if (unreadOnly && chat.getUnreadCount() <= 0) {
                continue;
            }
            User user = chat.getUser();
            if (user == null) continue;

            String firstName = user.getName() == null ? "" : user.getName();
            String lastName = user.getLastName() == null ? "" : user.getLastName();
            String fullName = (firstName + " " + lastName).toLowerCase(Locale.getDefault()).trim();
            String lastMessage = chat.getLastMessage() == null ? "" : chat.getLastMessage().toLowerCase(Locale.getDefault());

            if (q.isEmpty() || fullName.contains(q) || lastMessage.contains(q)) {
                filtered.add(chat);
            }
        }

        usersAdapter.setChats(filtered);
        updateEmptyState(filtered.isEmpty(), q);
    }

    private void updateEmptyState(boolean isEmpty, String query) {
        emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewUsers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (!isEmpty) return;

        if (unreadOnly) {
            textViewEmpty.setText("Нет непрочитанных сообщений");
            textViewEmptyHint.setText("Когда вам напишут новые сообщения, они появятся в этом разделе.");
        } else if (query != null && !query.trim().isEmpty()) {
            textViewEmpty.setText("Ничего не найдено");
            textViewEmptyHint.setText("Попробуйте изменить запрос или проверить имя пользователя в Firebase.");
        } else {
            textViewEmpty.setText("Пока нет собеседников");
            textViewEmptyHint.setText("Для проверки мессенджера создайте второй аккаунт. Текущий пользователь в списке не отображается.");
        }
    }

    public static Intent newIntent(Context context, String currentUserId) {
        return new Intent(context, UsersActivity.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.setUserOnline(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.setUserOnline(false);
        }
    }
}
