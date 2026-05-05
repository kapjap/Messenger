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

public class ProfileViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;

    private final MutableLiveData<FirebaseUser> firebaseUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> profileUserLiveData = new MutableLiveData<>();

    public ProfileViewModel() {
        auth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference("Users");

        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            firebaseUserLiveData.setValue(currentUser);
            if (currentUser == null) {
                profileUserLiveData.setValue(null);
            } else {
                setUserOnline(true);
                loadCurrentUser(currentUser.getUid());
            }
        });

        FirebaseUser currentUser = auth.getCurrentUser();
        firebaseUserLiveData.setValue(currentUser);
        if (currentUser != null) {
            setUserOnline(true);
            loadCurrentUser(currentUser.getUid());
        }
    }

    private void loadCurrentUser(String uid) {
        usersReference.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setId(snapshot.getKey());
                    user.setOnline(true);
                }
                profileUserLiveData.setValue(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setUserOnline(boolean isOnline) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;
        usersReference.child(currentUser.getUid()).child("online").setValue(isOnline);
        if (!isOnline) {
            usersReference.child(currentUser.getUid()).child("lastSeen").setValue(System.currentTimeMillis());
        }
    }

    public LiveData<FirebaseUser> getFirebaseUser() {
        return firebaseUserLiveData;
    }

    public LiveData<User> getProfileUser() {
        return profileUserLiveData;
    }

    public void logout() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            auth.signOut();
            return;
        }

        usersReference.child(currentUser.getUid()).child("online").setValue(false)
                .addOnCompleteListener(task -> auth.signOut());
    }
}
