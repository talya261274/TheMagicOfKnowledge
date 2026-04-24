package com.example.themagicofknowledge.models;

public class GameProgress {
    private int attempts;
    private boolean completed;
    private long timeSeconds;

    public GameProgress() {
    }

    public GameProgress(int attempts, boolean completed, long timeSeconds) {
        this.attempts = attempts;
        this.completed = completed;
        this.timeSeconds = timeSeconds;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getTimeSeconds() {
        return timeSeconds;
    }

    public void setTimeSeconds(long timeSeconds) {
        this.timeSeconds = timeSeconds;
    }
}