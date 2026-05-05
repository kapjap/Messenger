package com.example.messenger;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GroupInfoActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID = "group_id";

    private GroupInfoViewModel viewModel;
    private GroupMembersAdapter membersAdapter;

    private TextView textViewAdmin;
    private TextView textViewMembersCount;
    private TextView textViewCreatedAt;
    private EditText editTextGroupTitle;
    private EditText editTextGroupDescription;
    private Button buttonSave;
    private Button buttonLeaveGroup;

    private String groupId;
    private boolean isCurrentUserAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);

        ImageButton buttonBack = findViewById(R.id.buttonBackGroupInfo);
        editTextGroupTitle = findViewById(R.id.editTextGroupInfoTitle);
        editTextGroupDescription = findViewById(R.id.editTextGroupInfoDescription);
        textViewAdmin = findViewById(R.id.textViewGroupInfoAdmin);
        textViewMembersCount = findViewById(R.id.textViewGroupInfoMembersCount);
        textViewCreatedAt = findViewById(R.id.textViewGroupInfoCreatedAt);
        buttonSave = findViewById(R.id.buttonSaveGroupInfo);
        buttonLeaveGroup = findViewById(R.id.buttonLeaveGroup);
        RecyclerView recyclerViewMembers = findViewById(R.id.recyclerViewGroupMembers);

        membersAdapter = new GroupMembersAdapter();
        recyclerViewMembers.setAdapter(membersAdapter);

        viewModel = new ViewModelProvider(this).get(GroupInfoViewModel.class);

        buttonBack.setOnClickListener(v -> onBackPressed());
        buttonSave.setOnClickListener(v -> saveGroupInfo());
        buttonLeaveGroup.setOnClickListener(v -> onLeaveGroupClicked());

        viewModel.getGroupInfo().observe(this, state -> {
            if (state == null || state.group == null) return;
            editTextGroupTitle.setText(state.group.getTitle());
            editTextGroupDescription.setText(state.group.getDescription());
            textViewAdmin.setText(state.adminName);
            textViewMembersCount.setText(String.valueOf(state.membersCount));
            textViewCreatedAt.setText(formatDate(state.group.getCreatedAt()));
            membersAdapter.setItems(state.members);

            isCurrentUserAdmin = state.isCurrentUserAdmin;
            setEditableState(isCurrentUserAdmin);
        });

        viewModel.getError().observe(this, error -> {
            if (!TextUtils.isEmpty(error)) Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getLeaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Вы вышли из группы", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.loadGroupInfo(groupId);
    }

    private void setEditableState(boolean editable) {
        editTextGroupTitle.setEnabled(editable);
        editTextGroupDescription.setEnabled(editable);
        buttonSave.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void saveGroupInfo() {
        if (!isCurrentUserAdmin) return;
        String title = editTextGroupTitle.getText().toString().trim();
        String description = editTextGroupDescription.getText().toString().trim();
        viewModel.updateGroup(groupId, title, description);
    }

    private void onLeaveGroupClicked() {
        if (isCurrentUserAdmin) {
            new AlertDialog.Builder(this)
                    .setTitle("Внимание")
                    .setMessage("Вы администратор группы. При выходе вы потеряете права администратора.")
                    .setPositiveButton("Выйти", (dialog, which) -> viewModel.leaveGroup(groupId))
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            viewModel.leaveGroup(groupId);
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "-";
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
