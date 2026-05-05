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
import java.util.List;

public class FavoritesViewModel extends ViewModel {

    public static class FavoriteItem {
        private String messageId;
        private String text;
        private long timestamp;
        private String companionId;
        private String companionName;
        private boolean favorite;

        public FavoriteItem() {
        }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getCompanionId() { return companionId; }
        public void setCompanionId(String companionId) { this.companionId = companionId; }
        public String getCompanionName() { return companionName; }
        public void setCompanionName(String companionName) { this.companionName = companionName; }
        public boolean isFavorite() { return favorite; }
        public void setFavorite(boolean favorite) { this.favorite = favorite; }
    }

    private final MutableLiveData<List<FavoriteItem>> favorites = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final DatabaseReference favoritesRef;
    private final String uid;

    public FavoritesViewModel() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : null;
        favoritesRef = FirebaseDatabase.getInstance().getReference("Favorites");

        if (uid == null || uid.trim().isEmpty()) {
            error.setValue("Пользователь не авторизован");
            return;
        }

        observeFavorites();
    }

    private void observeFavorites() {
        favoritesRef.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FavoriteItem> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    FavoriteItem item = child.getValue(FavoriteItem.class);
                    if (item != null) {
                        if (item.getMessageId() == null || item.getMessageId().trim().isEmpty()) {
                            item.setMessageId(child.getKey());
                        }
                        list.add(item);
                    }
                }

                Collections.sort(list, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                favorites.setValue(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error.setValue(databaseError.getMessage());
            }
        });
    }

    public void removeFromFavorites(String messageId) {
        if (uid == null || messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        favoritesRef.child(uid).child(messageId).removeValue()
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    public LiveData<List<FavoriteItem>> getFavorites() {
        return favorites;
    }

    public LiveData<String> getError() {
        return error;
    }
}
