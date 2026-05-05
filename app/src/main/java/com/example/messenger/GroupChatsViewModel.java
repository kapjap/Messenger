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
    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("GroupMessages");

    public LiveData<List<GroupChatItem>> getGroupChats() { return groupChats; }
    public String getCurrentUserId() { return currentUser != null ? currentUser.getUid() : null; }

    public void loadGroups() {
        String uid = getCurrentUserId();
        if (uid == null) { groupChats.setValue(new ArrayList<>()); return; }

        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Group> userGroups = new ArrayList<>();
                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    Group group = groupSnapshot.getValue(Group.class);
                    if (group == null) continue;
                    if (group.getId() == null || group.getId().trim().isEmpty()) group.setId(groupSnapshot.getKey());
                    if (group.hasMember(uid)) userGroups.add(group);
                }
                loadPreviews(uid, userGroups);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { groupChats.setValue(new ArrayList<>()); }
        });
    }

    private void loadPreviews(String uid, List<Group> groups) {
        if (groups.isEmpty()) { groupChats.setValue(new ArrayList<>()); return; }

        Map<String, GroupChatItem> map = new HashMap<>();
        final int[] loaded = {0};
        for (Group group : groups) {
            map.put(group.getId(), new GroupChatItem(group));
            groupMessagesRef.child(group.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GroupMessage lastMessage = null;
                    int unreadCount = 0;
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        GroupMessage message = messageSnapshot.getValue(GroupMessage.class);
                        if (message == null) continue;
                        if (lastMessage == null || message.getTimestamp() > lastMessage.getTimestamp()) lastMessage = message;
                        if (!message.isReadBy(uid) && (message.getSenderId() == null || !uid.equals(message.getSenderId()))) unreadCount++;
                    }
                    GroupChatItem item = map.get(group.getId());
                    if (item != null) {
                        item.setLastMessage(lastMessage != null ? lastMessage.getText() : "");
                        item.setLastMessageTime(lastMessage != null ? lastMessage.getTimestamp() : group.getCreatedAt());
                        item.setUnreadCount(unreadCount);
                    }
                    loaded[0]++;
                    if (loaded[0] == groups.size()) publish(map);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loaded[0]++;
                    if (loaded[0] == groups.size()) publish(map);
                }
            });
        }
    }

    private void publish(Map<String, GroupChatItem> map) {
        List<GroupChatItem> items = new ArrayList<>(map.values());
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
