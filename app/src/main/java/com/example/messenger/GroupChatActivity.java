package com.example.messenger;

import static java.lang.Math.max;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GroupChatActivity extends AppCompatActivity {

    private static final String EXTRA_GROUP_ID = "extra_group_id";
    private static final String EXTRA_CURRENT_USER_ID = "extra_current_user_id";
    private static final String EXTRA_CURRENT_USER_NAME = "extra_current_user_name";

    private GroupChatViewModel viewModel;
    private GroupMessagesAdapter adapter;

    private TextView textViewGroupTitle;
    private TextView textViewMembersCount;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;

    private String groupId;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_group_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, max(systemBars.bottom, ime.bottom));
            return insets;
        });

        bindViews();

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        currentUserId = getIntent().getStringExtra(EXTRA_CURRENT_USER_ID);
        currentUserName = getIntent().getStringExtra(EXTRA_CURRENT_USER_NAME);

        if (groupId == null || groupId.trim().isEmpty() || currentUserId == null || currentUserId.trim().isEmpty()) {
            Toast.makeText(this, "Ошибка открытия группового чата", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new GroupMessagesAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(GroupChatViewModel.class);
        viewModel.init(groupId, currentUserId);
        observeViewModel();

        findViewById(R.id.buttonBack).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.buttonGroupInfo).setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra(GroupInfoActivity.EXTRA_GROUP_ID, groupId);
            startActivity(intent);
        });

        ImageView imageViewSendMessage = findViewById(R.id.imageViewSendMessage);
        imageViewSendMessage.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString();
            viewModel.sendMessage(currentUserId, currentUserName, text);
        });
    }

    private void bindViews() {
        textViewGroupTitle = findViewById(R.id.textViewGroupTitle);
        textViewMembersCount = findViewById(R.id.textViewMembersCount);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
    }

    private void observeViewModel() {
        viewModel.getGroup().observe(this, group -> {
            if (group == null) {
                textViewGroupTitle.setText("Группа");
                textViewMembersCount.setText("0 участников");
                return;
            }

            String title = group.getTitle() != null && !group.getTitle().trim().isEmpty()
                    ? group.getTitle()
                    : "Группа";
            int membersCount = group.getMembers() != null ? group.getMembers().size() : 0;

            textViewGroupTitle.setText(title);
            textViewMembersCount.setText(membersCount + " участников");
        });

        viewModel.getMessages().observe(this, messages -> {
            adapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getMessageSent().observe(this, sent -> {
            if (Boolean.TRUE.equals(sent)) {
                editTextMessage.setText("");
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static Intent newIntent(Context context, String groupId, String currentUserId, String currentUserName) {
        Intent intent = new Intent(context, GroupChatActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_CURRENT_USER_ID, currentUserId);
        intent.putExtra(EXTRA_CURRENT_USER_NAME, currentUserName);
        return intent;
    }
}
