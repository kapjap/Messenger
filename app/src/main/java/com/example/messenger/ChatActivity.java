package com.example.messenger;

import static java.lang.Math.max;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String EXTRA_CURRENT_USER_ID = "current_id";
    private static final String EXTRA_OTHER_USER_ID = "other_id";

    private TextView textViewTitle;
    private View onlineStatus;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageView imageViewSendMessage;

    private MessagesAdapter messagesAdapter;

    private String currentUserId;
    private String otherUserId;

    private ChatViewModel viewModel;
    private ChatViewModelFactory viewModelFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    max(systemBars.bottom, ime.bottom)
            );
            return insets;
        });

        initViews();

        currentUserId = getIntent().getStringExtra(EXTRA_CURRENT_USER_ID);
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);

        if (currentUserId == null || currentUserId.trim().isEmpty()
                || otherUserId == null || otherUserId.trim().isEmpty()) {
            Toast.makeText(this, "Ошибка открытия чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesAdapter = new MessagesAdapter(currentUserId);
        recyclerViewMessages.setAdapter(messagesAdapter);

        viewModelFactory = new ChatViewModelFactory(currentUserId, otherUserId);
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ChatViewModel.class);

        observeViewModel();

        imageViewSendMessage.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();

            if (text.isEmpty()) {
                Toast.makeText(ChatActivity.this, "Введите сообщение", Toast.LENGTH_SHORT).show();
                return;
            }

            Message message = new Message(text, currentUserId, otherUserId);
            viewModel.sendMessage(message);
        });
    }

    private void initViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        onlineStatus = findViewById(R.id.onlineStatus);
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

    private void observeViewModel() {
        viewModel.getMessages().observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messages) {
                messagesAdapter.setMessages(messages);

                if (messages != null && !messages.isEmpty()) {
                    recyclerViewMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        viewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                    Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getMessageSent().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean sent) {
                if (sent != null && sent) {
                    editTextMessage.setText("");
                }
            }
        });

        viewModel.getOtherUser().observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user == null) {
                    textViewTitle.setText("Пользователь");
                    onlineStatus.setBackground(
                            ContextCompat.getDrawable(ChatActivity.this, R.drawable.circle_red)
                    );
                    return;
                }

                String name = user.getName() != null ? user.getName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";

                String userInfo = (name + " " + lastName).trim();

                if (userInfo.isEmpty()) {
                    userInfo = "Пользователь";
                }

                textViewTitle.setText(userInfo);

                int bkResId = user.isOnline()
                        ? R.drawable.circle_green
                        : R.drawable.circle_red;

                Drawable background = ContextCompat.getDrawable(ChatActivity.this, bkResId);
                onlineStatus.setBackground(background);
            }
        });
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