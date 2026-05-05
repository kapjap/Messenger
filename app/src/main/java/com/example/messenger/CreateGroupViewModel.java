package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGroupViewModel extends ViewModel {

    private final MutableLiveData<Boolean> groupCreated = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");

    public LiveData<Boolean> getGroupCreated() {
        return groupCreated;
    }

    public LiveData<String> getError() {
        return error;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUid() : null;
    }

    public void createGroup(@NonNull String title,
                            String description,
                            @NonNull List<String> selectedMembers,
                            @NonNull OnGroupCreatedListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            String message = "Текущий пользователь не найден";
            error.setValue(message);
            listener.onError(message);
            return;
        }

        String groupId = groupsRef.push().getKey();
        if (groupId == null || groupId.trim().isEmpty()) {
            String message = "Не удалось создать группу";
            error.setValue(message);
            listener.onError(message);
            return;
        }

        Map<String, Boolean> members = new HashMap<>();
        members.put(currentUserId, true);
        for (String userId : selectedMembers) {
            if (userId != null && !userId.trim().isEmpty()) {
                members.put(userId, true);
            }
        }

        Group group = new Group();
        group.setId(groupId);
        group.setTitle(title.trim());
        group.setDescription(description == null ? "" : description.trim());
        group.setAdminId(currentUserId);
        group.setCreatedAt(System.currentTimeMillis());
        group.setMembers(members);

        groupsRef.child(groupId).setValue(group)
                .addOnSuccessListener(unused -> {
                    groupCreated.setValue(true);
                    listener.onSuccess(groupId);
                })
                .addOnFailureListener(e -> {
                    String message = e.getMessage() != null ? e.getMessage() : "Ошибка создания группы";
                    error.setValue(message);
                    listener.onError(message);
                });
    }

    interface OnGroupCreatedListener {
        void onSuccess(String groupId);
        void onError(String message);
    }
}
