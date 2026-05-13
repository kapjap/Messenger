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

public class LoginVIewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;

    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();

    public LoginVIewModel() {
        auth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference("Users");

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            checkUserProfile(currentUser, false);
        }
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
    }

    public void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        auth.signOut();
                        error.setValue("Не удалось войти в аккаунт");
                        return;
                    }
                    checkUserProfile(firebaseUser, true);
                })
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    private void checkUserProfile(FirebaseUser firebaseUser, boolean showMissingProfileError) {
        if (firebaseUser == null) {
            user.setValue(null);
            return;
        }

        usersReference.child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    user.setValue(firebaseUser);
                } else {
                    auth.signOut();
                    user.setValue(null);
                    if (showMissingProfileError) {
                        error.setValue("Профиль пользователя не найден. Зарегистрируйтесь заново.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                auth.signOut();
                user.setValue(null);
                error.setValue(databaseError.getMessage());
            }
        });
    }
}
