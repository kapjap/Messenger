package com.example.messenger;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class MessengerApp extends Application {

    private static final long BACKGROUND_DELAY_MS = 1500L;
    private static final long HEARTBEAT_INTERVAL_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int startedActivities = 0;
    private boolean isForeground = false;

    private final Runnable markOfflineRunnable = new Runnable() {
        @Override
        public void run() {
            if (startedActivities == 0) {
                isForeground = false;
                setPresence(false);
                handler.removeCallbacks(heartbeatRunnable);
            }
        }
    };

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isForeground) {
                setPresence(true);
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applyTheme(this);
        registerPresenceLifecycle();
    }

    private void registerPresenceLifecycle() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivities++;
                if (!isForeground) {
                    isForeground = true;
                    handler.removeCallbacks(markOfflineRunnable);
                    setupDisconnectPresence();
                    setPresence(true);
                    handler.removeCallbacks(heartbeatRunnable);
                    handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0) {
                    handler.removeCallbacks(markOfflineRunnable);
                    handler.postDelayed(markOfflineRunnable, BACKGROUND_DELAY_MS);
                }
            }

            @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) { }
            @Override public void onActivityResumed(@NonNull Activity activity) { }
            @Override public void onActivityPaused(@NonNull Activity activity) { }
            @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
            @Override public void onActivityDestroyed(@NonNull Activity activity) { }
        });
    }

    private void setupDisconnectPresence() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid());

        userRef.child("online").onDisconnect().setValue(false);
        userRef.child("typing").onDisconnect().setValue(false);
        userRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
    }

    private void setPresence(boolean online) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("online", online);
        updates.put("lastSeen", ServerValue.TIMESTAMP);

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(user.getUid())
                .updateChildren(updates);
    }
}
