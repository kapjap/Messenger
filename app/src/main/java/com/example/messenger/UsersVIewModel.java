package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class UsersVIewModel extends ViewModel {

    private FirebaseAuth auth;
    private DatabaseReference usersReference;

    private MutableLiveData<FirebaseUser> user = new MutableLiveData<>();
    private MutableLiveData<List<User>> users = new MutableLiveData<>();

    public UsersVIewModel() {

        auth = FirebaseAuth.getInstance();

        auth.addAuthStateListener(firebaseAuth ->
                user.setValue(firebaseAuth.getCurrentUser())
        );

        usersReference = FirebaseDatabase.getInstance().getReference("Users");

        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser == null) return;

                List<User> usersFromDb = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    User u = dataSnapshot.getValue(User.class);
                    if (u == null) continue;

                    // 🔥 ВАЖНО — ЗАДАЁМ ID ВРУЧНУЮ
                    String uid = dataSnapshot.getKey();
                    u = new User(uid, u.getName(), u.getLastName(), u.getAge(), u.isOnline());

                    if (!currentUser.getUid().equals(uid)) {
                        usersFromDb.add(u);
                    }
                }

                users.setValue(usersFromDb);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
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
    }
}