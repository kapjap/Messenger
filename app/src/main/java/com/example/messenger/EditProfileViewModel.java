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

import java.util.HashMap;
import java.util.Map;

public class EditProfileViewModel extends ViewModel {

    private static final int MIN_AGE = 1;
    private static final int MAX_AGE = 120;

    private final FirebaseAuth auth;
    private final DatabaseReference usersReference;

    private final MutableLiveData<User> profileUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> saveErrorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSavingLiveData = new MutableLiveData<>(false);

    public EditProfileViewModel() {
        auth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference("Users");
        loadCurrentUser();
    }

    public LiveData<User> getProfileUser() {
        return profileUserLiveData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccessLiveData;
    }

    public LiveData<String> getSaveError() {
        return saveErrorLiveData;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSavingLiveData;
    }

    public boolean isValidAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    private void loadCurrentUser() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            saveErrorLiveData.setValue("Пользователь не авторизован");
            return;
        }

        usersReference.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setId(snapshot.getKey());
                }
                profileUserLiveData.setValue(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                saveErrorLiveData.setValue(error.getMessage());
            }
        });
    }

    public void saveProfile(String name, String lastName, int age, String about) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            saveErrorLiveData.setValue("Пользователь не авторизован");
            return;
        }

        isSavingLiveData.setValue(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("lastName", lastName);
        updates.put("age", age);
        updates.put("about", about);

        usersReference.child(currentUser.getUid())
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    isSavingLiveData.setValue(false);
                    if (task.isSuccessful()) {
                        saveSuccessLiveData.setValue(true);
                    } else {
                        String message = task.getException() == null
                                ? "Ошибка сохранения профиля"
                                : task.getException().getMessage();
                        saveErrorLiveData.setValue(message);
                    }
                });
    }
}
