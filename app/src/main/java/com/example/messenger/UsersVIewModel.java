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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UsersVIewModel extends ViewModel {

    private static final long ONLINE_TIMEOUT_MS = 90_000L;

    public static class ChatPreview {
        private final User user;
        private final String lastMessage;
        private final long lastMessageTime;
        private final int unreadCount;
        private final boolean pinned;

        public ChatPreview(User user, String lastMessage, long lastMessageTime, int unreadCount, boolean pinned) {
            this.user = user;
            this.lastMessage = lastMessage;
            this.lastMessageTime = lastMessageTime;
            this.unreadCount = unreadCount;
            this.pinned = pinned;
        }

        public User getUser() { return user; }
        public String getLastMessage() { return lastMessage; }
        public long getLastMessageTime() { return lastMessageTime; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isPinned() { return pinned; }
    }

    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;
    private final DatabaseReference messagesReference;
    private final DatabaseReference pinnedChatsReference;

    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private final MutableLiveData<List<ChatPreview>> chatPreviews = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final Set<String> pinnedChatIds = new HashSet<>();
    private final Map<String, User> cachedUsers = new HashMap<>();
    private DataSnapshot cachedMessagesSnapshot;
    private String activeUserId;
    private boolean usersListenerAttached = false;
    private boolean messagesListenerAttached = false;
    private boolean pinnedListenerAttached = false;

    public UsersVIewModel() {
        auth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference("Users");
        messagesReference = FirebaseDatabase.getInstance().getReference("Messages");
        pinnedChatsReference = FirebaseDatabase.getInstance().getReference("PinnedChats");

        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser current = firebaseAuth.getCurrentUser();
            user.setValue(current);
            if (current == null) {
                activeUserId = null;
                cachedUsers.clear();
                pinnedChatIds.clear();
                cachedMessagesSnapshot = null;
                chatPreviews.setValue(new ArrayList<>());
                return;
            }
            activeUserId = current.getUid();
            setUserOnline(true);
            attachUsersListener();
            attachMessagesListener();
            attachPinnedChatsListener(activeUserId);
        });

        FirebaseUser current = auth.getCurrentUser();
        user.setValue(current);
        if (current != null) {
            activeUserId = current.getUid();
            setUserOnline(true);
            attachUsersListener();
            attachMessagesListener();
            attachPinnedChatsListener(activeUserId);
        }
    }

    private void attachUsersListener() {
        if (usersListenerAttached) return;
        usersListenerAttached = true;

        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                rebuildUsers(usersSnapshot);
                publishChatPreviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
                cachedUsers.clear();
                publishChatPreviews();
            }
        });
    }

    private void attachMessagesListener() {
        if (messagesListenerAttached) return;
        messagesListenerAttached = true;

        messagesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot messagesSnapshot) {
                cachedMessagesSnapshot = messagesSnapshot;
                publishChatPreviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
                cachedMessagesSnapshot = null;
                publishChatPreviews();
            }
        });
    }

    private void attachPinnedChatsListener(String uid) {
        if (pinnedListenerAttached) return;
        pinnedListenerAttached = true;

        pinnedChatsReference.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pinnedChatIds.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean isPinned = child.getValue(Boolean.class);
                    if (Boolean.TRUE.equals(isPinned) && child.getKey() != null) {
                        pinnedChatIds.add(child.getKey());
                    }
                }
                publishChatPreviews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
                publishChatPreviews();
            }
        });
    }

    private void rebuildUsers(DataSnapshot usersSnapshot) {
        cachedUsers.clear();
        String currentId = getCurrentUserId();
        if (currentId == null) return;

        for (DataSnapshot dataSnapshot : usersSnapshot.getChildren()) {
            User u = dataSnapshot.getValue(User.class);
            if (u == null) continue;

            String uid = dataSnapshot.getKey();
            if (uid == null || uid.equals(currentId)) continue;

            u.setId(uid);
            u.setOnline(isUserReallyOnline(u));
            cachedUsers.put(uid, u);
        }
    }

    private boolean isUserReallyOnline(User u) {
        if (u == null || !u.isOnline()) return false;
        long lastSeen = u.getLastSeen();
        if (lastSeen <= 0) return false;
        return System.currentTimeMillis() - lastSeen < ONLINE_TIMEOUT_MS;
    }

    private void publishChatPreviews() {
        String currentId = getCurrentUserId();
        if (currentId == null) {
            chatPreviews.setValue(new ArrayList<>());
            return;
        }

        List<ChatPreview> previews = new ArrayList<>();
        for (Map.Entry<String, User> entry : cachedUsers.entrySet()) {
            String otherUserId = entry.getKey();
            User otherUser = entry.getValue();
            String chatId = createChatId(currentId, otherUserId);

            String lastMessageText = "";
            long lastMessageTime = 0L;
            int unread = 0;

            DataSnapshot chatSnapshot = cachedMessagesSnapshot == null ? null : cachedMessagesSnapshot.child(chatId);
            if (chatSnapshot != null && chatSnapshot.exists()) {
                for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                    String text = messageSnapshot.child("text").getValue(String.class);
                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                    if (timestamp == null) timestamp = messageSnapshot.child("time").getValue(Long.class);
                    if (timestamp == null) timestamp = messageSnapshot.child("createdAt").getValue(Long.class);
                    if (timestamp == null) timestamp = 0L;

                    if (timestamp >= lastMessageTime) {
                        lastMessageTime = timestamp;
                        lastMessageText = text == null ? "" : text;
                    }

                    String senderId = messageSnapshot.child("senderId").getValue(String.class);
                    String receiverId = messageSnapshot.child("receiverId").getValue(String.class);
                    Boolean isRead = messageSnapshot.child("read").getValue(Boolean.class);
                    if (isRead == null) isRead = messageSnapshot.child("isRead").getValue(Boolean.class);
                    if (isRead == null) isRead = false;

                    boolean incoming = !currentId.equals(senderId) && currentId.equals(receiverId);
                    if (incoming && !isRead) {
                        unread++;
                    }
                }
            }

            previews.add(new ChatPreview(otherUser, lastMessageText, lastMessageTime, unread, pinnedChatIds.contains(chatId)));
        }

        Collections.sort(previews, (a, b) -> {
            if (a.isPinned() != b.isPinned()) {
                return a.isPinned() ? -1 : 1;
            }
            return Long.compare(b.getLastMessageTime(), a.getLastMessageTime());
        });
        chatPreviews.setValue(previews);
    }

    public void togglePinned(String otherUserId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || otherUserId == null) return;

        String chatId = createChatId(currentUser.getUid(), otherUserId);
        DatabaseReference ref = pinnedChatsReference.child(currentUser.getUid()).child(chatId);

        if (pinnedChatIds.contains(chatId)) {
            ref.removeValue();
        } else {
            ref.setValue(true);
        }
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

    public LiveData<String> getError() {
        return error;
    }

    public String getCurrentUserId() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        return firebaseUser != null ? firebaseUser.getUid() : activeUserId;
    }

    public void logout() {
        setUserOnline(false);
        auth.signOut();
    }

    public void setUserOnline(boolean isOnline) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", isOnline);
        updates.put("lastSeen", ServerValue.TIMESTAMP);
        usersReference.child(firebaseUser.getUid()).updateChildren(updates);
    }
}
