package com.example.themagicofknowledge.models;

public class SentenceQuestion {
    private String sentence;       // המשפט עם הקו הריק
    private String correctAnswer;  // המילה שהילד צריך להקליד
    private String hintImage;      // שם התמונה ב-drawable

    public SentenceQuestion() {}

    public SentenceQuestion(String sentence, String correctAnswer, String hintImage) {
        this.sentence = sentence;
        this.correctAnswer = correctAnswer;
        this.hintImage = hintImage;
    }

    public String getSentence() {
        return sentence;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getHintImage() {
        return hintImage;
    }

    public void setHintImage(String hintImage) {
        this.hintImage = hintImage;
    }
}