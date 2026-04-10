package com.example.themagicofknowledge.models;

public class SubjectStat {
    private String subjectName;
    private int attempts;
    private long timeInSeconds;

    public SubjectStat() {}

    public SubjectStat(String subjectName, int attempts, long timeInSeconds) {
        this.subjectName = subjectName;
        this.attempts = attempts;
        this.timeInSeconds = timeInSeconds;
    }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public long getTimeInSeconds() { return timeInSeconds; }
    public void setTimeInSeconds(long timeInSeconds) { this.timeInSeconds = timeInSeconds; }
}