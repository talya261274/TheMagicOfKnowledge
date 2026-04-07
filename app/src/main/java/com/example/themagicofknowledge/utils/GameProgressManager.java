package com.example.themagicofknowledge.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class GameProgressManager {

    public static void saveProgress(String parentId, String childId,
                                    String level, String subject,
                                    boolean completed, int attempts,
                                    long timeSeconds) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("progress")
                .child(level)
                .child(subject);

        Map<String, Object> data = new HashMap<>();
        data.put("completed", completed);
        data.put("attempts", attempts);
        data.put("timeSeconds", timeSeconds);

        ref.updateChildren(data);
    }

    public static void getAttempts(String parentId, String childId,
                                   String level, String subject,
                                   AttemptCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("children")
                .child(childId)
                .child("progress")
                .child(level)
                .child(subject)
                .child("attempts");

        ref.get().addOnSuccessListener(snapshot -> {
            int attempts = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
            callback.onResult(attempts);
        });
    }

    public interface AttemptCallback {
        void onResult(int attempts);
    }
}