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
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>();
    private final MutableLiveData<User> otherUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSent = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final DatabaseReference referenceUsers;
    private final DatabaseReference referenceMessages;

    private final String currentUserId;
    private final String otherUserId;
    private String chatId;

    public ChatViewModel(String currentUserId, String otherUserId) {
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;

        referenceUsers = FirebaseDatabase.getInstance().getReference("Users");
        referenceMessages = FirebaseDatabase.getInstance().getReference("Messages");

        if (currentUserId == null || currentUserId.trim().isEmpty()
                || otherUserId == null || otherUserId.trim().isEmpty()) {
            error.setValue("Ошибка: id пользователя пустой");
            return;
        }

        chatId = createChatId(currentUserId, otherUserId);

        loadOtherUser();
        loadMessages();
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

    private void loadMessages() {
        referenceMessages.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messageList = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Message message = dataSnapshot.getValue(Message.class);
                    if (message != null) {
                        if (message.getId() == null || message.getId().trim().isEmpty()) {
                            message.setId(dataSnapshot.getKey());
                        }
                        messageList.add(message);
                    }
                }

                messages.setValue(messageList);
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

        newMessageRef
                .setValue(message)
                .addOnSuccessListener(unused -> messageSent.setValue(true))
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
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

    public LiveData<String> getError() {
        return error;
    }

    public void setUserOnline(boolean isOnline) {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return;
        }

        referenceUsers.child(currentUserId).child("online").setValue(isOnline);
    }
}
