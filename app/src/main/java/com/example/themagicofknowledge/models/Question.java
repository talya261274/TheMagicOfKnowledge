package com.example.themagicofknowledge.models;

import java.util.List;

public class Question {
    private String questionText;
    private List<String> options;
    private List<String> optionLabels;
    private int correctAnswerIndex;
    private String mediaUrl;

    public Question() { }

    public Question(String questionText, List<String> options, List<String> optionLabels, int correctAnswerIndex, String mediaUrl) {
        this.questionText = questionText;
        this.options = options;
        this.optionLabels = optionLabels;
        this.correctAnswerIndex = correctAnswerIndex;
        this.mediaUrl = mediaUrl;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<String> getOptionLabels() {
        return optionLabels;
    }

    public void setOptionLabels(List<String> optionLabels) {
        this.optionLabels = optionLabels;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    @Override
    public String toString() {
        return "Question{" +
                "questionText='" + questionText + '\'' +
                ", options=" + options +
                ", correctAnswerIndex=" + correctAnswerIndex +
                ", mediaUrl='" + mediaUrl + '\'' +
                '}';
    }
}