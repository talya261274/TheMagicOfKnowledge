package com.example.themagicofknowledge.models;

import java.util.List;

public class Question {
    public String id;
    public String subject;       // "Colors", "Animals", "Math" וכו'
    public String type;          // "DRAG", "SELECT", "AUDIO"
    public String questionText;
    public String mediaUrl;      // תמונה או צליל
    public List<String> options; // תשובות אפשריות
    public int correctAnswerIndex;

    public Question() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", subject='" + subject + '\'' +
                ", type='" + type + '\'' +
                ", questionText='" + questionText + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", options=" + options +
                ", correctAnswerIndex=" + correctAnswerIndex +
                '}';
    }
}