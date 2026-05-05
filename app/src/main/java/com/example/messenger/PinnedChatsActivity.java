package com.example.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PinnedChatsActivity extends AppCompatActivity {

    private UsersAdapter usersAdapter;
    private UsersVIewModel viewModel;
    private TextInputEditText editTextSearch;
    private TextView textViewEmpty;
    private final List<UsersVIewModel.ChatPreview> pinnedChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinned_chats);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewPinnedChats);
        editTextSearch = findViewById(R.id.editTextSearchPinned);
        textViewEmpty = findViewById(R.id.textViewEmptyPinned);
        ImageButton buttonBack = findViewById(R.id.buttonBackPinned);

        buttonBack.setOnClickListener(v -> onBackPressed());

        usersAdapter = new UsersAdapter();
        recyclerView.setAdapter(usersAdapter);

        viewModel = new ViewModelProvider(this).get(UsersVIewModel.class);
        viewModel.getChatPreviews().observe(this, previews -> {
            pinnedChats.clear();
            if (previews != null) {
                for (UsersVIewModel.ChatPreview chat : previews) {
                    if (chat.isPinned()) pinnedChats.add(chat);
                }
            }
            applyFilter(editTextSearch.getText());
        });

        usersAdapter.setOnUserClickListener(user -> {
            String currentUserId = viewModel.getCurrentUserId();
            if (currentUserId == null || user == null || user.getId() == null) return;
            startActivity(ChatActivity.newIntent(this, currentUserId, user.getId()));
        });

        usersAdapter.setOnUserLongClickListener(chatPreview -> {
            User user = chatPreview.getUser();
            if (user != null && user.getId() != null) viewModel.togglePinned(user.getId());
        });

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(s); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void applyFilter(CharSequence query) {
        String q = query == null ? "" : query.toString().toLowerCase(Locale.getDefault()).trim();
        if (q.isEmpty()) {
            usersAdapter.setChats(new ArrayList<>(pinnedChats));
            textViewEmpty.setVisibility(pinnedChats.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        List<UsersVIewModel.ChatPreview> filtered = new ArrayList<>();
        for (UsersVIewModel.ChatPreview chat : pinnedChats) {
            User user = chat.getUser();
            String name = (user.getName() + " " + user.getLastName()).toLowerCase(Locale.getDefault());
            if (name.contains(q)) filtered.add(chat);
        }
        usersAdapter.setChats(filtered);
        textViewEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PinnedChatsActivity.class);
    }
}
