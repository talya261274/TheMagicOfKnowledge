package com.example.themagicofknowledge.models;

import java.util.List;

public class Question {
    // השמות חייבים להיות זהים בדיוק למה שיש ב-Firebase
    public String questionText;
    public List<String> options;
    public int correctAnswerIndex;
    public String mediaUrl;

    public Question() {
    }

    public Question(String questionText, List<String> options, int correctAnswerIndex, String mediaUrl) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.mediaUrl = mediaUrl;
    }

    // גטרים וסטרים (אופציונלי אבל מומלץ)
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}