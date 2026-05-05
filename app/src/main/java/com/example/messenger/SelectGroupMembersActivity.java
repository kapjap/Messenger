package com.example.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectGroupMembersActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_MEMBER_IDS = "selected_member_ids";
    public static final String EXTRA_PRESELECTED_IDS = "preselected_member_ids";

    private SelectMembersAdapter adapter;
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewSelectMembers);
        ImageButton buttonBack = findViewById(R.id.buttonBackSelectMembers);
        Button buttonCreate = findViewById(R.id.buttonCreateGroupFromMembers);

        adapter = new SelectMembersAdapter();
        recyclerView.setAdapter(adapter);

        buttonBack.setOnClickListener(v -> onBackPressed());
        buttonCreate.setOnClickListener(v -> returnResult());

        loadUsers();
    }

    private void loadUsers() {
        String currentUserId = currentUser != null ? currentUser.getUid() : null;
        ArrayList<String> preselected = getIntent().getStringArrayListExtra(EXTRA_PRESELECTED_IDS);
        Set<String> preselectedSet = preselected == null ? new HashSet<>() : new HashSet<>(preselected);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user == null) continue;
                    if (user.getId() == null || user.getId().trim().isEmpty()) user.setId(child.getKey());
                    if (user.getId() == null || user.getId().equals(currentUserId)) continue;
                    users.add(user);
                }
                adapter.setUsers(users);
                adapter.setPreselectedIds(preselectedSet);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SelectGroupMembersActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void returnResult() {
        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_SELECTED_MEMBER_IDS, new ArrayList<>(adapter.getSelectedIds()));
        setResult(RESULT_OK, result);
        finish();
    }
}
