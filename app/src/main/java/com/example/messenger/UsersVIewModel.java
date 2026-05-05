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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersVIewModel extends ViewModel {

    public static class ChatPreview {
        private final User user;
        private final String lastMessage;
        private final long lastMessageTime;
        private final int unreadCount;

        public ChatPreview(User user, String lastMessage, long lastMessageTime, int unreadCount) {
            this.user = user;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
        }

        public User getUser() { return user; }
        public String getLastMessage() { return lastMessage; }
        public long getLastMessageTime() { return lastMessageTime; }
        public int getUnreadCount() { return unreadCount; }
    }

    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;
    private final DatabaseReference messagesReference;

    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<List<ChatPreview>> chatPreviews = new MutableLiveData<>(new ArrayList<>());

    public UsersVIewModel() {
        auth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference("Users");
        messagesReference = FirebaseDatabase.getInstance().getReference("Messages");

        auth.addAuthStateListener(firebaseAuth -> {
            user.setValue(firebaseAuth.getCurrentUser());
            loadChats();
        });

        loadChats();
    }

    private void loadChats() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            chatPreviews.setValue(new ArrayList<>());
            return;
        }

        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                Map<String, User> usersMap = new HashMap<>();
                for (DataSnapshot dataSnapshot : usersSnapshot.getChildren()) {
                    User u = dataSnapshot.getValue(User.class);
                    if (u == null) continue;
                    String uid = dataSnapshot.getKey();
                    if (uid == null || uid.equals(currentUser.getUid())) continue;
                    u.setId(uid);
                    usersMap.put(uid, u);
                }

                messagesReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot messagesSnapshot) {
                        List<ChatPreview> previews = new ArrayList<>();

                        for (Map.Entry<String, User> entry : usersMap.entrySet()) {
                            String otherUserId = entry.getKey();
                            User otherUser = entry.getValue();
                            String chatId = createChatId(currentUser.getUid(), otherUserId);

                            DataSnapshot chatSnapshot = messagesSnapshot.child(chatId);
                            String lastMessageText = "";
                            long lastMessageTime = 0L;
                            int unread = 0;

                            if (chatSnapshot.exists()) {
                                for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                                    String text = messageSnapshot.child("text").getValue(String.class);
                                    if (text != null) {
                                        lastMessageText = text;
                                    }

                                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                                    if (timestamp == null) {
                                        timestamp = messageSnapshot.child("time").getValue(Long.class);
                                    }
                                    if (timestamp == null) {
                                        timestamp = messageSnapshot.child("createdAt").getValue(Long.class);
                                    }
                                    if (timestamp != null && timestamp > 0) {
                                        lastMessageTime = timestamp;
                                    }

                                    String receiverId = messageSnapshot.child("receiverId").getValue(String.class);
                                    Boolean isRead = messageSnapshot.child("isRead").getValue(Boolean.class);
                                    if (currentUser.getUid().equals(receiverId) && Boolean.FALSE.equals(isRead)) {
                                        unread++;
                                    }
                                }
                            }

                            previews.add(new ChatPreview(otherUser, lastMessageText, lastMessageTime, unread));
                        }

                        Collections.sort(previews, (a, b) -> Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
                        chatPreviews.setValue(previews);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private String createChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public LiveData<List<ChatPreview>> getChatPreviews() {
        return chatPreviews;
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
    }

    public String getCurrentUserId() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        return firebaseUser != null ? firebaseUser.getUid() : null;
    }

    public void logout() {
        auth.signOut();
    }

    public void setUserOnline(boolean isOnline) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        usersReference.child(firebaseUser.getUid())
                .child("online")
                .setValue(isOnline);

        if (!isOnline) {
            usersReference.child(firebaseUser.getUid())
                    .child("lastSeen")
                    .setValue(System.currentTimeMillis());
        }
    }
}
