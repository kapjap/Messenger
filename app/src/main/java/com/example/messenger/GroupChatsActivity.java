package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GroupChatsActivity extends AppCompatActivity {

    private GroupChatsViewModel viewModel;
    private GroupsAdapter groupsAdapter;
    private TextView textViewEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chats);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewGroups);
        textViewEmpty = findViewById(R.id.textViewEmptyGroups);
        ImageButton buttonBack = findViewById(R.id.buttonBackGroups);
        FloatingActionButton buttonCreateGroup = findViewById(R.id.buttonCreateGroup);

        buttonBack.setOnClickListener(v -> onBackPressed());
        buttonCreateGroup.setOnClickListener(v -> Toast.makeText(this, "Создание группы — TODO", Toast.LENGTH_SHORT).show());

        groupsAdapter = new GroupsAdapter();
        recyclerView.setAdapter(groupsAdapter);

        groupsAdapter.setOnGroupClickListener(group -> {
            String currentUserId = viewModel.getCurrentUserId();
            if (currentUserId == null || group == null || group.getId() == null) return;
            Intent intent = ChatActivity.newIntent(this, currentUserId, group.getId());
            startActivity(intent);
        });

        viewModel = new ViewModelProvider(this).get(GroupChatsViewModel.class);
        viewModel.getGroupChats().observe(this, items -> {
            groupsAdapter.setItems(items);
            textViewEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : TextView.GONE);
        });
        viewModel.loadGroups();
    }
}
