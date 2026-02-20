package com.example.themagicofknowledge.models;

import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.Exclude;

public class UserChild {

    String id;
    String parentId;
    String name;
    String ageGroup; // יכול להיות "3-4", "5-6", "7-8"
    int age;
    int currentLevel;
    double gradeAvg;
    long totalTimeSeconds;

    public UserChild() {
        // Constructor ריק נדרש ל-Firebase
    }

    public UserChild(String id, String parentId, String name, int age) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.age = age;
        this.currentLevel = 0;
        this.gradeAvg = 0.0;
        this.totalTimeSeconds = 0;

        // קביעת קבוצת הגיל אוטומטית לפי הגיל שהוזן
        if (age >= 3 && age <= 4) {
            this.ageGroup = "3-4";
        } else if (age >= 5 && age <= 6) {
            this.ageGroup = "5-6";
        } else if (age >= 7 && age <= 8) {
            this.ageGroup = "7-8";
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public double getGradeAvg() {
        return gradeAvg;
    }

    public void setGradeAvg(double gradeAvg) {
        this.gradeAvg = gradeAvg;
    }

    public long getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(long totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }

    @Exclude
    public String getFormattedTime() {
        long hours = totalTimeSeconds / 3600;
        long minutes = (totalTimeSeconds % 3600) / 60;
        long seconds = totalTimeSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void addTime(long secondsToAdd) {
        this.totalTimeSeconds += secondsToAdd;
    }

    // המרה ל-Map עבור Firebase
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("parentId", parentId);
        result.put("name", name);
        result.put("ageGroup", ageGroup);
        result.put("age", age);
        result.put("currentLevel", currentLevel);
        result.put("gradeAvg", gradeAvg);
        result.put("totalTimeSeconds", totalTimeSeconds);
        return result;
    }

    @Override
    public String toString() {
        return "UserChild{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", name='" + name + '\'' +
                ", ageGroup='" + ageGroup + '\'' +
                ", age=" + age +
                ", currentLevel=" + currentLevel +
                ", gradeAvg=" + gradeAvg +
                ", totalTimeSeconds=" + totalTimeSeconds +
                '}';
    }
}
