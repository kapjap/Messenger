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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ActivityFeedViewModel extends ViewModel {

    private final MutableLiveData<ActivityStats> statsLiveData = new MutableLiveData<>(ActivityStats.empty());
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    private final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
    private final DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("Messages");
    private final DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("Groups");
    private final DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("GroupMessages");

    public LiveData<ActivityStats> getStats() {
        return statsLiveData;
    }

    public void loadActivity() {
        String uid = currentUser != null ? currentUser.getUid() : null;
        if (uid == null || uid.trim().isEmpty()) {
            statsLiveData.setValue(ActivityStats.empty());
            return;
        }

        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot messagesSnapshot) {
                groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot groupsSnapshot) {
                        calculate(uid, messagesSnapshot, groupsSnapshot);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        statsLiveData.setValue(ActivityStats.empty());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                statsLiveData.setValue(ActivityStats.empty());
            }
        });
    }

    private void calculate(String uid, DataSnapshot messagesSnapshot, DataSnapshot groupsSnapshot) {
        int totalSent = 0;
        int unread = 0;
        final int[] weekly = new int[7];
        long latestTs = 0;
        String latestAction = "Активность пока отсутствует";

        Set<String> activeChats = new HashSet<>();
        Map<String, Integer> contactActivity = new HashMap<>();

        long startOfWeek = getStartOfWeekMillis();
        long now = System.currentTimeMillis();

        for (DataSnapshot chatSnapshot : messagesSnapshot.getChildren()) {
            for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                Message message = messageSnapshot.getValue(Message.class);
                if (message == null) continue;

                boolean related = uid.equals(message.getSenderId()) || uid.equals(message.getReceiverId());
                if (!related) continue;

                activeChats.add(chatSnapshot.getKey());

                if (uid.equals(message.getSenderId())) {
                    totalSent++;
                    String otherId = message.getReceiverId();
                    if (otherId != null && !otherId.trim().isEmpty()) {
                        contactActivity.put(otherId, contactActivity.getOrDefault(otherId, 0) + 1);
                    }
                }

                if (uid.equals(message.getReceiverId()) && !message.isRead()) {
                    unread++;
                }

                if (message.getTimestamp() > latestTs) {
                    latestTs = message.getTimestamp();
                    latestAction = uid.equals(message.getSenderId()) ? "Отправлено сообщение" : "Получено сообщение";
                }

                if (uid.equals(message.getSenderId()) && message.getTimestamp() >= startOfWeek && message.getTimestamp() <= now) {
                    int dayIndex = dayIndexFromMonday(message.getTimestamp());
                    if (dayIndex >= 0 && dayIndex < 7) {
                        weekly[dayIndex]++;
                    }
                }
            }
        }

        List<String> userGroupIds = new ArrayList<>();
        for (DataSnapshot groupSnapshot : groupsSnapshot.getChildren()) {
            Group group = groupSnapshot.getValue(Group.class);
            if (group == null) continue;
            if (group.getId() == null || group.getId().trim().isEmpty()) group.setId(groupSnapshot.getKey());
            if (group.hasMember(uid)) {
                userGroupIds.add(group.getId());
                activeChats.add(group.getId());
            }
        }

        final int finalTotalSent = totalSent;
        final int finalUnread = unread;
        final int finalActiveChatsCount = activeChats.size();
        final int finalGroupChatsCount = userGroupIds.size();
        final Map<String, Integer> finalContactActivity = new HashMap<>(contactActivity);

        if (userGroupIds.isEmpty()) {
            publishResult(uid, finalTotalSent, finalActiveChatsCount, finalUnread, 0, latestTs, latestAction, weekly, finalContactActivity);
            return;
        }

        final int[] loaded = {0};
        final int[] unreadGroups = {0};
        final String[] latestActionHolder = {latestAction};
        final long[] latestTsHolder = {latestTs};

        for (String groupId : userGroupIds) {
            groupMessagesRef.child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot gmSnapshot : snapshot.getChildren()) {
                        GroupMessage gm = gmSnapshot.getValue(GroupMessage.class);
                        if (gm == null) continue;

                        if (!gm.isReadBy(uid) && (gm.getSenderId() == null || !uid.equals(gm.getSenderId()))) {
                            unreadGroups[0]++;
                        }

                        if (uid.equals(gm.getSenderId()) && gm.getTimestamp() >= startOfWeek && gm.getTimestamp() <= now) {
                            int idx = dayIndexFromMonday(gm.getTimestamp());
                            if (idx >= 0 && idx < 7) weekly[idx]++;
                        }

                        if (gm.getTimestamp() > latestTsHolder[0]) {
                            latestTsHolder[0] = gm.getTimestamp();
                            latestActionHolder[0] = uid.equals(gm.getSenderId())
                                    ? "Отправлено сообщение в группе"
                                    : "Получено сообщение в группе";
                        }
                    }

                    loaded[0]++;
                    if (loaded[0] == finalGroupChatsCount) {
                        publishResult(uid, finalTotalSent, finalActiveChatsCount, finalUnread + unreadGroups[0], finalGroupChatsCount, latestTsHolder[0], latestActionHolder[0], weekly, finalContactActivity);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loaded[0]++;
                    if (loaded[0] == finalGroupChatsCount) {
                        publishResult(uid, finalTotalSent, finalActiveChatsCount, finalUnread + unreadGroups[0], finalGroupChatsCount, latestTsHolder[0], latestActionHolder[0], weekly, finalContactActivity);
                    }
                }
            });
        }
    }

    private void publishResult(String uid,
                               int totalSent,
                               int activeChats,
                               int unread,
                               int groupChats,
                               long latestTs,
                               String latestActionText,
                               int[] weekly,
                               Map<String, Integer> contactActivity) {

        if (totalSent == 0 && activeChats == 0 && unread == 0 && groupChats == 0 && latestTs == 0L && isZeroWeek(weekly)) {
            statsLiveData.setValue(ActivityStats.empty());
            return;
        }

        String lastAction = latestTs > 0
                ? latestActionText + " · " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(latestTs)
                : "Активность пока отсутствует";

        resolveTopContact(uid, contactActivity, topContact -> {
            ActivityStats stats = new ActivityStats(totalSent, activeChats, unread, groupChats, lastAction, topContact, weekly);
            statsLiveData.setValue(stats);
        });
    }

    private void resolveTopContact(String uid, Map<String, Integer> contactActivity, TopContactCallback callback) {
        if (contactActivity.isEmpty()) {
            callback.onReady("Активность пока отсутствует");
            return;
        }

        String topId = null;
        int max = 0;
        for (Map.Entry<String, Integer> entry : contactActivity.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                topId = entry.getKey();
            }
        }

        if (topId == null) {
            callback.onReady("Активность пока отсутствует");
            return;
        }

        String finalTopId = topId;
        int finalMax = max;
        usersRef.child(topId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                String name = user != null && user.getName() != null && !user.getName().trim().isEmpty()
                        ? user.getName()
                        : finalTopId;
                callback.onReady(name + " · " + finalMax + " сообщений");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onReady(finalTopId + " · " + finalMax + " сообщений");
            }
        });
    }

    private boolean isZeroWeek(int[] weekly) {
        for (int count : weekly) if (count != 0) return false;
        return true;
    }

    private long getStartOfWeekMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private int dayIndexFromMonday(long ts) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ts);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        return (day + 5) % 7;
    }

    private interface TopContactCallback {
        void onReady(String topContactText);
    }

    public static class ActivityStats {
        public final int totalSent;
        public final int activeChats;
        public final int unread;
        public final int groupChats;
        public final String lastAction;
        public final String topContact;
        public final int[] week;

        public ActivityStats(int totalSent, int activeChats, int unread, int groupChats, String lastAction, String topContact, int[] week) {
            this.totalSent = totalSent;
            this.activeChats = activeChats;
            this.unread = unread;
            this.groupChats = groupChats;
            this.lastAction = lastAction;
            this.topContact = topContact;
            this.week = week;
        }

        public static ActivityStats empty() {
            return new ActivityStats(0, 0, 0, 0, "Активность пока отсутствует", "Активность пока отсутствует", new int[]{0, 0, 0, 0, 0, 0, 0});
        }
    }
}
