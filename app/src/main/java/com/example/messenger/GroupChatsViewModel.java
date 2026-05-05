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

public class GroupChatsViewModel extends ViewModel {

    private final MutableLiveData<List<GroupChatItem>> groupChats = new MutableLiveData<>(new ArrayList<>());
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("GroupMessages");

    private final Map<String, Group> userGroups = new HashMap<>();
    private final Map<String, GroupMessage> lastMessages = new HashMap<>();
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private String currentUserName = "";
    private boolean loaded = false;

    public LiveData<List<GroupChatItem>> getGroupChats() { return groupChats; }
    public String getCurrentUserId() { return currentUser != null ? currentUser.getUid() : null; }
    public String getCurrentUserName() { return currentUserName == null ? "" : currentUserName; }

    public void loadGroups() {
        String uid = getCurrentUserId();
        if (uid == null || uid.trim().isEmpty()) {
            groupChats.setValue(new ArrayList<>());
            return;
        }

        loadCurrentUserName(uid);

        if (loaded) return;
        loaded = true;

        groupsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userGroups.clear();
                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    Group group = groupSnapshot.getValue(Group.class);
                    if (group == null) continue;
                    if (group.getId() == null || group.getId().trim().isEmpty()) group.setId(groupSnapshot.getKey());
                    if (group.hasMember(uid)) {
                        userGroups.put(group.getId(), group);
                    }
                }
                publish();
                attachGroupMessagesListeners(uid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                groupChats.setValue(new ArrayList<>());
            }
        });
    }

    private void loadCurrentUserName(String uid) {
        usersRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    String fullName = (user.getName() + " " + user.getLastName()).trim();
                    if (!fullName.isEmpty()) {
                        currentUserName = fullName;
                    } else if (!user.getEmail().isEmpty()) {
                        currentUserName = user.getEmail();
                    } else {
                        currentUserName = uid;
                    }
                } else {
                    currentUserName = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : uid;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                currentUserName = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : uid;
            }
        });
    }

    private void attachGroupMessagesListeners(String uid) {
        for (String groupId : userGroups.keySet()) {
            groupMessagesRef.child(groupId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GroupMessage lastMessage = null;
                    int unreadCount = 0;

                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        GroupMessage message = messageSnapshot.getValue(GroupMessage.class);
                        if (message == null) continue;
                        if (message.getId() == null || message.getId().trim().isEmpty()) message.setId(messageSnapshot.getKey());
                        if (lastMessage == null || message.getTimestamp() > lastMessage.getTimestamp()) lastMessage = message;
                        if (!message.isReadBy(uid) && (message.getSenderId() == null || !uid.equals(message.getSenderId()))) unreadCount++;
                    }

                    lastMessages.put(groupId, lastMessage);
                    unreadCounts.put(groupId, unreadCount);
                    publish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    publish();
                }
            });
        }
    }

    private void publish() {
        List<GroupChatItem> items = new ArrayList<>();
        for (Group group : userGroups.values()) {
            GroupChatItem item = new GroupChatItem(group);
            GroupMessage last = lastMessages.get(group.getId());
            item.setLastMessage(last != null ? last.getText() : "Начните общение");
            item.setLastMessageTime(last != null ? last.getTimestamp() : group.getCreatedAt());
            Integer unread = unreadCounts.get(group.getId());
            item.setUnreadCount(unread == null ? 0 : unread);
            items.add(item);
        }
        Collections.sort(items, Comparator.comparingLong(GroupChatItem::getLastMessageTime).reversed());
        groupChats.setValue(items);
    }

    public static class GroupChatItem {
        private final Group group;
        private String lastMessage = "";
        private long lastMessageTime;
        private int unreadCount;
        public GroupChatItem(Group group) { this.group = group; }
        public Group getGroup() { return group; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        public long getLastMessageTime() { return lastMessageTime; }
        public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    }
}
