package com.example.themagicofknowledge.models;

public class MemoryCard {
    public int imageRes;
    public String text;
    public String matchId;
    public boolean isFlipped = false;
    public boolean isMatched = false;

    public MemoryCard() {
    }

    public MemoryCard(int imageRes, String text, String matchId) {
        this.imageRes = imageRes;
        this.text = text;
        this.matchId = matchId;
    }

    public int getImageRes() {
        return imageRes;
    }

    public void setImageRes(int imageRes) {
        this.imageRes = imageRes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean flipped) {
        isFlipped = flipped;
    }

    public boolean isMatched() {
        return isMatched;
    }

    public void setMatched(boolean matched) {
        isMatched = matched;
    }

    @Override
    public String toString() {
        return "MemoryCard{" +
                "imageRes=" + imageRes +
                ", text='" + text + '\'' +
                ", matchId='" + matchId + '\'' +
                ", isFlipped=" + isFlipped +
                ", isMatched=" + isMatched +
                '}';
    }
}