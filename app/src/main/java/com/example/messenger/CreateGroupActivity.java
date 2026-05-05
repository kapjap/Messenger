package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class CreateGroupActivity extends AppCompatActivity {

    private TextInputEditText editTextGroupName;
    private TextInputEditText editTextGroupDescription;
    private ArrayList<String> selectedMembers = new ArrayList<>();
    private CreateGroupViewModel viewModel;

    private final ActivityResultLauncher<Intent> selectMembersLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedMembers = result.getData().getStringArrayListExtra(SelectGroupMembersActivity.EXTRA_SELECTED_MEMBER_IDS);
                    if (selectedMembers == null) selectedMembers = new ArrayList<>();
                    Toast.makeText(this, "Выбрано участников: " + selectedMembers.size(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        editTextGroupName = findViewById(R.id.editTextGroupName);
        editTextGroupDescription = findViewById(R.id.editTextGroupDescription);
        Button buttonSelectMembers = findViewById(R.id.buttonSelectMembers);
        Button buttonCreateGroup = findViewById(R.id.buttonCreateGroupConfirm);
        ImageButton buttonBack = findViewById(R.id.buttonBackCreateGroup);

        buttonBack.setOnClickListener(v -> onBackPressed());

        viewModel = new ViewModelProvider(this).get(CreateGroupViewModel.class);
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        buttonSelectMembers.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectGroupMembersActivity.class);
            intent.putStringArrayListExtra(SelectGroupMembersActivity.EXTRA_PRESELECTED_IDS, selectedMembers);
            selectMembersLauncher.launch(intent);
        });

        buttonCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String name = editTextGroupName.getText() != null ? editTextGroupName.getText().toString().trim() : "";
        String description = editTextGroupDescription.getText() != null ? editTextGroupDescription.getText().toString().trim() : "";

        if (name.isEmpty()) {
            editTextGroupName.setError("Введите название группы");
            return;
        }

        viewModel.createGroup(name, description, selectedMembers, new CreateGroupViewModel.OnGroupCreatedListener() {
            @Override
            public void onSuccess(String groupId) {
                String currentUserId = viewModel.getCurrentUserId();
                if (currentUserId == null) {
                    finish();
                    return;
                }
                Intent intent = ChatActivity.newIntent(CreateGroupActivity.this, currentUserId, groupId);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CreateGroupActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
