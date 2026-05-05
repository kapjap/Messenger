package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GroupInfoViewModel extends ViewModel {

    public static class GroupInfoState {
        public Group group;
        public String adminName;
        public int membersCount;
        public List<GroupMembersAdapter.MemberItem> members;
        public boolean isCurrentUserAdmin;
    }

    private final MutableLiveData<GroupInfoState> groupInfo = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> leaveSuccess = new MutableLiveData<>();

    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    public LiveData<GroupInfoState> getGroupInfo() { return groupInfo; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getLeaveSuccess() { return leaveSuccess; }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUid() : null;
    }

    public void loadGroupInfo(String groupId) {
        String uid = getCurrentUserId();
        if (uid == null || groupId == null || groupId.trim().isEmpty()) {
            error.setValue("Не удалось загрузить группу");
            return;
        }

        groupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null) {
                    error.setValue("Группа не найдена");
                    return;
                }
                if (group.getId() == null || group.getId().trim().isEmpty()) {
                    group.setId(snapshot.getKey());
                }
                Map<String, Boolean> membersMap = group.getMembers();
                List<String> memberIds = new ArrayList<>();
                if (membersMap != null) {
                    for (Map.Entry<String, Boolean> entry : membersMap.entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) memberIds.add(entry.getKey());
                    }
                }
                loadMembers(group, memberIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue("Ошибка загрузки группы");
            }
        });
    }

    private void loadMembers(Group group, List<String> memberIds) {
        if (memberIds.isEmpty()) {
            GroupInfoState state = new GroupInfoState();
            state.group = group;
            state.adminName = "Неизвестно";
            state.membersCount = 0;
            state.members = Collections.emptyList();
            state.isCurrentUserAdmin = group.getAdminId() != null && group.getAdminId().equals(getCurrentUserId());
            groupInfo.setValue(state);
            return;
        }

        List<GroupMembersAdapter.MemberItem> members = new ArrayList<>();
        final int[] loaded = {0};
        final String[] adminName = {"Неизвестно"};

        for (String memberId : memberIds) {
            usersRef.child(memberId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    String name = "Пользователь";
                    String email = "";
                    if (user != null) {
                        String fullName = (user.getName() + " " + user.getLastName()).trim();
                        name = fullName.isEmpty() ? "Пользователь" : fullName;
                        email = user.getEmail();
                    }
                    boolean isAdmin = memberId.equals(group.getAdminId());
                    if (isAdmin) adminName[0] = name;
                    members.add(new GroupMembersAdapter.MemberItem(memberId, name, email, isAdmin));
                    loaded[0]++;
                    if (loaded[0] == memberIds.size()) {
                        publishState(group, members, adminName[0]);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loaded[0]++;
                    if (loaded[0] == memberIds.size()) {
                        publishState(group, members, adminName[0]);
                    }
                }
            });
        }
    }

    private void publishState(Group group, List<GroupMembersAdapter.MemberItem> members, String adminName) {
        GroupInfoState state = new GroupInfoState();
        state.group = group;
        state.adminName = adminName;
        state.membersCount = members.size();
        state.members = members;
        state.isCurrentUserAdmin = group.getAdminId() != null && group.getAdminId().equals(getCurrentUserId());
        groupInfo.setValue(state);
    }

    public void updateGroup(String groupId, String newTitle, String newDescription) {
        String uid = getCurrentUserId();
        if (uid == null || groupId == null) return;

        groupsRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group group = snapshot.getValue(Group.class);
                if (group == null || !uid.equals(group.getAdminId())) {
                    error.setValue("Только администратор может редактировать группу");
                    return;
                }
                groupsRef.child(groupId).child("title").setValue(newTitle);
                groupsRef.child(groupId).child("description").setValue(newDescription);
                loadGroupInfo(groupId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                thisError("Ошибка обновления группы");
            }

            private void thisError(String message) {
                GroupInfoViewModel.this.error.setValue(message);
            }
        });
    }

    public void leaveGroup(String groupId) {
        String uid = getCurrentUserId();
        if (uid == null || groupId == null) {
            error.setValue("Не удалось выйти из группы");
            return;
        }

        groupsRef.child(groupId).child("members").child(uid).removeValue((databaseError, databaseReference) -> {
            if (databaseError != null) {
                error.setValue("Ошибка выхода из группы");
            } else {
                leaveSuccess.setValue(true);
            }
        });
    }
}
