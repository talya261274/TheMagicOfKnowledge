package com.example.themagicofknowledge.models;

import java.util.ArrayList;
import java.util.List;

public class LevelProgress {
    private String ageGroup;
    private List<SubjectStat> subjects;
    private boolean isCurrent;
    private boolean isLocked;        // האם הרמה נעולה (עתידית)
    private boolean isCompleted;
    private boolean isExpanded;      // האם פתוחה כרגע (UI)

    public LevelProgress(String ageGroup) {
        this.ageGroup = ageGroup;
        this.subjects = new ArrayList<>();
        this.isCurrent = false;
        this.isLocked = false;
        this.isCompleted = false;
        this.isExpanded = false;
    }

    // === Getters & Setters ===
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public List<SubjectStat> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectStat> subjects) { this.subjects = subjects; }

    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean current) { isCurrent = current; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    // === חישובי סטטיסטיקה ===
    public long getTotalTimeSeconds() {
        long total = 0;
        for (SubjectStat s : subjects) {
            total += s.getTimeInSeconds();
        }
        return total;
    }

    public int getTotalAttempts() {
        int total = 0;
        for (SubjectStat s : subjects) {
            total += s.getAttempts();
        }
        return total;
    }

    public int getAveragePercent() {
        if (subjects.isEmpty()) return 0;
        int total = 0;
        for (SubjectStat s : subjects) {
            total += s.getProgressPercent();
        }
        return total / subjects.size();
    }
}