package com.example.themagicofknowledge.models;

public class SubjectStat {
    private String subjectName;
    private int attempts;
    private long timeInSeconds;
    private boolean completed;
    private int progressPercent;

    public SubjectStat() {}

    public SubjectStat(String subjectName, int attempts, long timeInSeconds, boolean completed, int progressPercent) {
        this.subjectName = subjectName;
        this.attempts = attempts;
        this.timeInSeconds = timeInSeconds;
        this.completed = completed;
        this.progressPercent = progressPercent;
    }

    // Getters & Setters
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public long getTimeInSeconds() { return timeInSeconds; }
    public void setTimeInSeconds(long timeInSeconds) { this.timeInSeconds = timeInSeconds; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
}