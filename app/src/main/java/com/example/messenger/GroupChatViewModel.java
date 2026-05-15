package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatViewModel extends ViewModel {

    private final MutableLiveData<List<GroupMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Group> group = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSent = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("GroupMessages");

    private final Map<String, String> senderNames = new HashMap<>();
    private List<GroupMessage> cachedMessages = new ArrayList<>();

    private String groupId;
    private String currentUserId;
    private String currentUserName = "";

    public void init(String groupId, String currentUserId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            error.setValue("Ошибка: groupId пустой");
            return;
        }
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            error.setValue("Ошибка: пользователь не найден");
            return;
        }
        if (groupId.equals(this.groupId) && currentUserId.equals(this.currentUserId)) {
            return;
        }

        this.groupId = groupId;
        this.currentUserId = currentUserId;
        observeCurrentUserName();
        observeUsers();
        observeGroup();
        observeMessages();
    }

    private void observeCurrentUserName() {
        usersRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserName = buildUserDisplayName(snapshot, currentUserId);
                senderNames.put(currentUserId, currentUserName);
                publishMessages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private void observeUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderNames.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String uid = userSnapshot.getKey();
                    if (uid == null || uid.trim().isEmpty()) continue;
                    senderNames.put(uid, buildUserDisplayName(userSnapshot, uid));
                }
                publishMessages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private String buildUserDisplayName(DataSnapshot snapshot, String fallbackUid) {
        String name = snapshot.child("name").getValue(String.class);
        String lastName = snapshot.child("lastName").getValue(String.class);
        String email = snapshot.child("email").getValue(String.class);

        String fullName = ((name == null ? "" : name) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isEmpty()) return fullName;
        if (email != null && !email.trim().isEmpty()) return email.trim();
        return fallbackUid == null ? "Пользователь" : fallbackUid;
    }

    private void observeGroup() {
        groupsRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Group groupValue = snapshot.getValue(Group.class);
                if (groupValue != null) {
                    if (groupValue.getId() == null || groupValue.getId().trim().isEmpty()) {
                        groupValue.setId(snapshot.getKey());
                    }
                    group.setValue(groupValue);
                } else {
                    error.setValue("Группа не найдена");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private void observeMessages() {
        groupMessagesRef.child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GroupMessage> newMessages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    GroupMessage message = child.getValue(GroupMessage.class);
                    if (message != null) {
                        if (message.getId() == null || message.getId().trim().isEmpty()) {
                            message.setId(child.getKey());
                        }
                        markIncomingMessageAsRead(message);
                        newMessages.add(message);
                    }
                }
                cachedMessages = newMessages;
                publishMessages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private void publishMessages() {
        List<GroupMessage> prepared = new ArrayList<>();
        for (GroupMessage message : cachedMessages) {
            String senderId = message.getSenderId();
            if (senderId != null && senderNames.containsKey(senderId)) {
                message.setSenderName(senderNames.get(senderId));
            } else if (message.getSenderName() == null || message.getSenderName().trim().isEmpty()) {
                message.setSenderName("Пользователь");
            }
            prepared.add(message);
        }
        messages.setValue(prepared);
    }

    private void markIncomingMessageAsRead(GroupMessage message) {
        if (message == null || currentUserId == null || message.getId() == null) return;
        if (currentUserId.equals(message.getSenderId())) return;
        if (message.isReadBy(currentUserId)) return;

        groupMessagesRef
                .child(groupId)
                .child(message.getId())
                .child("readBy")
                .child(currentUserId)
                .setValue(true);
    }

    public void sendMessage(String senderId, String senderName, String text) {
        if (groupId == null || groupId.trim().isEmpty()) {
            error.setValue("Ошибка группы");
            return;
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            error.setValue("Ошибка пользователя");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            error.setValue("Введите сообщение");
            return;
        }

        DatabaseReference newMessageRef = groupMessagesRef.child(groupId).push();
        String messageId = newMessageRef.getKey();

        Map<String, Boolean> readBy = new HashMap<>();
        readBy.put(senderId, true);

        String resolvedSenderName = senderNames.containsKey(senderId)
                ? senderNames.get(senderId)
                : currentUserName;
        if (resolvedSenderName == null || resolvedSenderName.trim().isEmpty()) {
            resolvedSenderName = senderName != null ? senderName : "";
        }

        GroupMessage message = new GroupMessage(
                messageId,
                text.trim(),
                senderId,
                resolvedSenderName,
                System.currentTimeMillis(),
                readBy
        );

        newMessageRef.setValue(message)
                .addOnSuccessListener(unused -> messageSent.setValue(true))
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    public LiveData<List<GroupMessage>> getMessages() {
        return messages;
    }

    public LiveData<Group> getGroup() {
        return group;
    }

    public LiveData<Boolean> getMessageSent() {
        return messageSent;
    }

    public LiveData<String> getError() {
        return error;
    }
}
