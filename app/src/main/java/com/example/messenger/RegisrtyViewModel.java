package com.example.messenger;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisrtyViewModel extends ViewModel {
    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();

    public RegisrtyViewModel() {
        auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    user.setValue(firebaseAuth.getCurrentUser());
                }
            }
        });
        usersReference = FirebaseDatabase.getInstance().getReference("Users");
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<FirebaseUser> getUser() {
        return user;
    }

    public void signUp(String email, String password, String name, String lastName, int age) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> saveUserToDatabase(authResult, email, name, lastName, age))
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }

    private void saveUserToDatabase(AuthResult authResult,
                                    String email,
                                    String name,
                                    String lastName,
                                    int age) {
        FirebaseUser firebaseUser = authResult.getUser();
        if (firebaseUser == null) {
            error.setValue("Не удалось получить данные пользователя.");
            return;
        }

        long now = System.currentTimeMillis();
        User appUser = new User(
                firebaseUser.getUid(),
                name,
                lastName,
                email,
                age,
                "",
                true,
                now,
                now
        );

        usersReference.child(firebaseUser.getUid())
                .setValue(appUser)
                .addOnFailureListener(e -> error.setValue(e.getMessage()));
    }
}
