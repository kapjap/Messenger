package com.example.messenger;

import static java.lang.Math.max;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String EXTRA_CURRENT_USER_ID = "current_id";
    private static final String EXTRA_OTHER_USER_ID = "other_id";

    private TextView textViewTitle;
    private TextView textViewStatus;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageView imageViewSendMessage;

    private MessagesAdapter messagesAdapter;

    private String currentUserId;
    private String otherUserId;

    private ChatViewModel viewModel;
    private TextWatcher messageWatcher;
    private DatabaseReference favoritesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, max(systemBars.bottom, ime.bottom));
            return insets;
        });

        initViews();

        favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites");

        currentUserId = getIntent().getStringExtra(EXTRA_CURRENT_USER_ID);
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);

        if (currentUserId == null || currentUserId.trim().isEmpty()
                || otherUserId == null || otherUserId.trim().isEmpty()) {
            Toast.makeText(this, "Ошибка открытия чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesAdapter = new MessagesAdapter(currentUserId);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter.setOnMessageLongClickListener(this::showMessageActions);
        recyclerViewMessages.setAdapter(messagesAdapter);

        ChatViewModelFactory viewModelFactory = new ChatViewModelFactory(currentUserId, otherUserId);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ChatViewModel.class);

        observeViewModel();
        initTypingListener();

        imageViewSendMessage.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (text.isEmpty()) {
                return;
            }

            Message message = new Message(
                    null,
                    text,
                    currentUserId,
                    otherUserId,
                    System.currentTimeMillis(),
                    false,
                    false
            );
            viewModel.sendMessage(message);
            viewModel.setTyping(false);
        });
    }


    private void showMessageActions(Message message) {
        if (message == null || message.getId() == null || message.getId().trim().isEmpty()) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(this, recyclerViewMessages);
        String actionTitle = message.isFavorite() ? "Убрать из избранного" : "Добавить в избранное";
        popupMenu.getMenu().add(actionTitle);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (message.isFavorite()) {
                removeFromFavorites(message);
            } else {
                addToFavorites(message);
            }
            return true;
        });
        popupMenu.show();
    }

    private void addToFavorites(Message message) {
        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("messageId", message.getId());
        favoriteData.put("text", message.getText());
        favoriteData.put("timestamp", message.getTimestamp());
        favoriteData.put("companionId", otherUserId);
        favoriteData.put("companionName", textViewTitle.getText() != null ? textViewTitle.getText().toString() : "");
        favoriteData.put("favorite", true);

        favoritesRef.child(currentUserId).child(message.getId()).setValue(favoriteData)
                .addOnSuccessListener(unused -> {
                    message.setFavorite(true);
                    messagesAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeFromFavorites(Message message) {
        favoritesRef.child(currentUserId).child(message.getId()).removeValue()
                .addOnSuccessListener(unused -> {
                    message.setFavorite(false);
                    messagesAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void initViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewStatus = findViewById(R.id.textViewStatus);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        imageViewSendMessage = findViewById(R.id.imageViewSendMessage);
    }

    public static Intent newIntent(Context context, String currentUserId, String otherUserId) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_CURRENT_USER_ID, currentUserId);
        intent.putExtra(EXTRA_OTHER_USER_ID, otherUserId);
        return intent;
    }

    private void initTypingListener() {
        messageWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (viewModel == null) {
                    return;
                }
                viewModel.setTyping(s != null && s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        editTextMessage.addTextChangedListener(messageWatcher);
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(this, messages -> {
            messagesAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getMessageSent().observe(this, sent -> {
            if (sent != null && sent) {
                editTextMessage.setText("");
            }
        });

        viewModel.getOtherUser().observe(this, user -> {
            if (user == null) {
                textViewTitle.setText("Пользователь");
                return;
            }

            String name = user.getName() != null ? user.getName() : "";
            String lastName = user.getLastName() != null ? user.getLastName() : "";
            String userInfo = (name + " " + lastName).trim();

            textViewTitle.setText(userInfo.isEmpty() ? "Пользователь" : userInfo);
            updateStatusText(user.isOnline(), viewModel.getOtherUserTyping().getValue());
        });

        viewModel.getOtherUserTyping().observe(this, isTyping -> {
            User user = viewModel.getOtherUser().getValue();
            boolean isOnline = user != null && user.isOnline();
            updateStatusText(isOnline, isTyping);
        });
    }

    private void updateStatusText(boolean isOnline, Boolean isTyping) {
        if (Boolean.TRUE.equals(isTyping)) {
            textViewStatus.setText("печатает...");
        } else {
            textViewStatus.setText(isOnline ? "online" : "offline");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.setUserOnline(true);
            String text = editTextMessage != null ? editTextMessage.getText().toString().trim() : "";
            viewModel.setTyping(!text.isEmpty());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.setTyping(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (editTextMessage != null && messageWatcher != null) {
            editTextMessage.removeTextChangedListener(messageWatcher);
        }
        super.onDestroy();
    }
}
