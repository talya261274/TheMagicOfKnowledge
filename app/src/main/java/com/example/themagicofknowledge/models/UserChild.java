package com.example.themagicofknowledge.models;

public class UserChild {

    String id;
    String parentId;
    String name;
    int age;
    int currentLevel;
    double gradeAvg;
    long totalTimeSeconds;

    public UserChild() {
    }


    public UserChild(String id, String parentId, String name, int age) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.age = age;
        this.currentLevel = 0;
        this.gradeAvg = 0.0;
        this.totalTimeSeconds = 0;
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

    public String getFormattedTime() {
        long hours = totalTimeSeconds / 3600;
        long minutes = (totalTimeSeconds % 3600) / 60;
        long seconds = totalTimeSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void addTime(long secondsToAdd) {
        this.totalTimeSeconds += secondsToAdd;
    }

    @Override
    public String toString() {
        return "UserChild{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", currentLevel=" + currentLevel +
                ", gradeAvg=" + gradeAvg +
                ", totalTimeSeconds=" + totalTimeSeconds +
                '}';
    }
}
