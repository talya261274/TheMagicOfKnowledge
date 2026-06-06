package com.example.themagicofknowledge.models;

public class FlashCard {
    private String imageResId;
    private String answer;
    private String subject;

    public FlashCard() {}

    public FlashCard(String imageResId, String answer, String subject) {
        this.imageResId = imageResId;
        this.answer = answer;
        this.subject = subject;
    }

    public String getImageResId() {
        return imageResId;
    }

    public void setImageResId(String imageResId) {
        this.imageResId = imageResId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String toString() {
        return "FlashCard{" +
                "imageResId=" + imageResId +
                ", answer='" + answer + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}