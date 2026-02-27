package com.example.themagicofknowledge.models;

public class FlashCard {
    private int imageResId;
    private String answer;

    public FlashCard() {
    }

    public FlashCard(int imageResId, String answer) {
        this.imageResId = imageResId;
        this.answer = answer;
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

    @Override
    public String toString() {
        return "FlashCard{" +
                "imageResId=" + imageResId +
                ", answer='" + answer + '\'' +
                '}';
    }
}