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

    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("GroupMessages");

    private String groupId;
    private String currentUserId;

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
        observeGroup();
        observeMessages();
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
                messages.setValue(newMessages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
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

        GroupMessage message = new GroupMessage(
                messageId,
                text.trim(),
                senderId,
                senderName != null ? senderName : "",
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
