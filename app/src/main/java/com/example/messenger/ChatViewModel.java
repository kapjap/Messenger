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

public class ChatViewModel extends ViewModel {

    private static final long ONLINE_TIMEOUT_MS = 2 * 60 * 1000;

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<User> otherUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> otherUserTyping = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final DatabaseReference referenceUsers;
    private final DatabaseReference referenceMessages;
    private final DatabaseReference referenceTyping;

    private final String currentUserId;
    private final String otherUserId;
    private String chatId;

    public ChatViewModel(String currentUserId, String otherUserId) {
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;

        referenceUsers = FirebaseDatabase.getInstance().getReference("Users");
        referenceMessages = FirebaseDatabase.getInstance().getReference("Messages");
        referenceTyping = FirebaseDatabase.getInstance().getReference("Typing");

        if (currentUserId == null || currentUserId.trim().isEmpty()
                || otherUserId == null || otherUserId.trim().isEmpty()) {
            error.setValue("Ошибка: id пользователя пустой");
            return;
        }

        chatId = createChatId(currentUserId, otherUserId);

        setupPresence();
        loadOtherUser();
        loadMessages();
        observeOtherUserTyping();
    }

    private String createChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void loadOtherUser() {
        referenceUsers.child(otherUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);

                if (user != null) {
                    user.setId(snapshot.getKey());
                    user.setOnline(isUserReallyOnline(user));
                    otherUser.setValue(user);
                } else {
                    error.setValue("Пользователь не найден");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private boolean isUserReallyOnline(User user) {
        if (user == null || !user.isOnline()) return false;
        long lastSeen = user.getLastSeen();
        if (lastSeen <= 0) return true;
        return System.currentTimeMillis() - lastSeen < ONLINE_TIMEOUT_MS;
    }

    private void loadMessages() {
        referenceMessages.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messageList = new ArrayList<>();
                List<String> unreadIncomingIds = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        if (message.getId() == null || message.getId().trim().isEmpty()) {
                            message.setId(dataSnapshot.getKey());
                        }
                        messageList.add(message);

                        if (currentUserId.equals(message.getReceiverId()) && !message.isRead() && dataSnapshot.getKey() != null) {
                            unreadIncomingIds.add(dataSnapshot.getKey());
                        }
                    }
                }

                messages.setValue(messageList);
                markIncomingAsRead(unreadIncomingIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    private void markIncomingAsRead(List<String> unreadIncomingIds) {
        if (unreadIncomingIds == null || unreadIncomingIds.isEmpty()) {
            return;
        }
        for (String messageId : unreadIncomingIds) {
            referenceMessages.child(chatId).child(messageId).child("read").setValue(true);
        }
    }

    private void observeOtherUserTyping() {
        if (!canUseTypingNode()) {
            otherUserTyping.setValue(false);
            return;
        }

        referenceTyping
                .child(chatId)
                .child(otherUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isTyping = snapshot.getValue(Boolean.class);
                        otherUserTyping.setValue(Boolean.TRUE.equals(isTyping));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        error.setValue(databaseError.getMessage());
                    }
                });
    }

    public void sendMessage(Message message) {
        if (message == null) {
            error.setValue("Сообщение пустое");
            return;
        }

        if (chatId == null || chatId.trim().isEmpty()) {
            error.setValue("Ошибка чата");
            return;
        }

        if (message.getText() == null || message.getText().trim().isEmpty()) {
            error.setValue("Введите сообщение");
            return;
        }

        if (message.getSenderId() == null || message.getReceiverId() == null) {
            error.setValue("Ошибка отправителя или получателя");
            return;
        }

        DatabaseReference newMessageRef = referenceMessages.child(chatId).push();
        message.setId(newMessageRef.getKey());

        if (message.getTimestamp() <= 0) {
            message.setTimestamp(System.currentTimeMillis());
        }
        message.setRead(false);

        newMessageRef
                .setValue(message)
                .addOnSuccessListener(unused -> messageSent.setValue(true))
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    public void setTyping(boolean isTyping) {
        if (!canUseTypingNode()) {
            return;
        }

        DatabaseReference typingRef = referenceTyping.child(chatId).child(currentUserId);
        typingRef.onDisconnect().setValue(false);
        typingRef.setValue(isTyping);
    }

    private boolean canUseTypingNode() {
        return chatId != null && !chatId.trim().isEmpty()
                && currentUserId != null && !currentUserId.trim().isEmpty()
                && otherUserId != null && !otherUserId.trim().isEmpty();
    }

    private void setupPresence() {
        if (currentUserId == null || currentUserId.trim().isEmpty()) return;
        DatabaseReference userRef = referenceUsers.child(currentUserId);
        userRef.child("online").onDisconnect().setValue(false);
        userRef.child("lastSeen").onDisconnect().setValue(System.currentTimeMillis());
        setUserOnline(true);
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<User> getOtherUser() {
        return otherUser;
    }

    public LiveData<Boolean> getMessageSent() {
        return messageSent;
    }

    public LiveData<Boolean> getOtherUserTyping() {
        return otherUserTyping;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setUserOnline(boolean isOnline) {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", isOnline);
        updates.put("lastSeen", System.currentTimeMillis());
        referenceUsers.child(currentUserId).updateChildren(updates);
    }
}
