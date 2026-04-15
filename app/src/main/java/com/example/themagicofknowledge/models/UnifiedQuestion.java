package com.example.themagicofknowledge.models;

public class UnifiedQuestion {
    public enum Type { AUDIO, IMAGE, MATCHING, SENTENCE, MEMORY }

    private Type type;
    private Object data;

    public UnifiedQuestion(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() { return type; }
    public Object getData() { return data; }
}