package com.example.themagicofknowledge.models;

public class FlashCard {
    int imageResId;
    String answer;
    private int soundResId;

    public FlashCard() {
    }

    public FlashCard(int imageResId, String answer, int soundResId) {
        this.imageResId = imageResId;
        this.answer = answer;
        this.soundResId = soundResId;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public int getSoundResId() {
        return soundResId;
    }

    public void setSoundResId(int soundResId) {
        this.soundResId = soundResId;
    }

    @Override
    public String toString() {
        return "FlashCard{" +
                "imageResId=" + imageResId +
                ", answer='" + answer + '\'' +
                ", soundResId=" + soundResId +
                '}';
    }
}



