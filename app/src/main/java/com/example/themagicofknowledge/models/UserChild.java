package com.example.themagicofknowledge.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class UserChild {

    private String id;
    private String parentId;
    private String name;
    private String avatar;
    private String ageGroup;
    private int age;
    private String currentLevel;
    private long totalTimeSeconds;
    private Map<String, Object> progress = new HashMap<>();
    private Map<String, Boolean> completedSubjects = new HashMap<>();

    // ⭐ חדש - ציון המבדק (null = לא עשה מבדק עדיין)
    private Double lastPlacementScore;

    public UserChild() {
    }

    public UserChild(String id, String parentId, String name, int age) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.age = age;
        this.totalTimeSeconds = 0;
        this.lastPlacementScore = null;  // ⭐ ילד חדש - לא עשה מבדק עדיין

        if (age >= 3 && age <= 4) this.ageGroup = "3-4";
        else if (age >= 5 && age <= 6) this.ageGroup = "5-6";
        else if (age >= 7 && age <= 8) this.ageGroup = "7-8";
        else this.ageGroup = "general";

        this.currentLevel = this.ageGroup;

        initializeDefaultProgress();
    }

    private void initializeDefaultProgress() {
        Map<String, Object> subjects = new HashMap<>();
        String[] subjectNames = {"animals", "numbers", "colors", "letters", "shapes", "bodyparts"};

        for (String subject : subjectNames) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("attempts", 0);
            stats.put("completed", false);
            stats.put("progressPercent", 0);
            stats.put("timeSeconds", 0);

            subjects.put(subject, stats);
        }

        this.progress.put(this.ageGroup, subjects);
    }

    // ===== Getters and Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }

    public long getTotalTimeSeconds() { return totalTimeSeconds; }
    public void setTotalTimeSeconds(long totalTimeSeconds) { this.totalTimeSeconds = totalTimeSeconds; }

    public Map<String, Object> getProgress() { return progress; }
    public void setProgress(Map<String, Object> progress) { this.progress = progress; }

    public Map<String, Boolean> getCompletedSubjects() { return completedSubjects; }
    public void setCompletedSubjects(Map<String, Boolean> completedSubjects) { this.completedSubjects = completedSubjects; }

    // ⭐ Getter ו-Setter לשדה החדש
    public Double getLastPlacementScore() { return lastPlacementScore; }
    public void setLastPlacementScore(Double lastPlacementScore) { this.lastPlacementScore = lastPlacementScore; }

    @Exclude
    public String getFormattedTime() {
        long hours = totalTimeSeconds / 3600;
        long minutes = (totalTimeSeconds % 3600) / 60;
        long seconds = totalTimeSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("parentId", parentId);
        result.put("name", name);
        result.put("avatar", avatar);
        result.put("ageGroup", ageGroup);
        result.put("age", age);
        result.put("currentLevel", currentLevel);
        result.put("totalTimeSeconds", totalTimeSeconds);
        result.put("progress", progress);
        result.put("completedSubjects", completedSubjects);
        result.put("lastPlacementScore", lastPlacementScore);
        return result;
    }

    @Override
    public String toString() {
        return "UserChild{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ageGroup='" + ageGroup + '\'' +
                ", lastPlacementScore=" + lastPlacementScore +
                '}';
    }
}