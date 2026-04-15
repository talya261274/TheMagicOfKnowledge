package com.example.themagicofknowledge.models;

public class MemoryCard {
    public int imageRes;
    public String text;
    public String matchId;
    public boolean isFlipped = false;
    public boolean isMatched = false;

    public MemoryCard(int imageRes, String text, String matchId) {
        this.imageRes = imageRes;
        this.text = text;
        this.matchId = matchId;
    }
}