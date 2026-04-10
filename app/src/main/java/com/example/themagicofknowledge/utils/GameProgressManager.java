package com.example.themagicofknowledge.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class GameProgressManager {

    public static void saveProgress(String parentId, String childId,
                                    String level, String subject,
                                    boolean completed, long timeSeconds) {

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(childId)
                .child("progress")
                .child(level)
                .child(subject);

        ref.get().addOnSuccessListener(snapshot -> {
            int currentAttempts = 0;

            if (snapshot.exists() && snapshot.hasChild("attempts")) {
                currentAttempts = snapshot.child("attempts").getValue(Integer.class);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("completed", completed);
            data.put("attempts", currentAttempts + 1);
            data.put("timeSeconds", timeSeconds);

            ref.updateChildren(data);
        });
    }

    public static void getAttempts(String parentId, String childId,
                                   String level, String subject,
                                   AttemptCallback callback) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("childrenList")
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

    public static void updateTotalTime(String parentId, String childId, long additionalSeconds) {
        DatabaseReference childRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(parentId)
                .child("childrenList")
                .child(childId);

        childRef.child("totalTimeSeconds").get().addOnSuccessListener(snapshot -> {
            long currentTime = 0;
            if (snapshot.exists()) {
                Object value = snapshot.getValue();
                if (value instanceof Long) {
                    currentTime = (Long) value;
                } else if (value instanceof Integer) {
                    currentTime = ((Integer) value).longValue();
                }
            }

            childRef.child("totalTimeSeconds").setValue(currentTime + additionalSeconds);
        });
    }

    public interface AttemptCallback {
        void onResult(int attempts);
    }
}