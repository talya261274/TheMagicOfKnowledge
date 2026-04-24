package com.example.themagicofknowledge.models;

public class UnifiedQuestion {
    private Type type;
    private Object data;

    public UnifiedQuestion() {
    }

    public UnifiedQuestion(Type type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "UnifiedQuestion{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }

    public enum Type {AUDIO, IMAGE, MATCHING, SENTENCE, MEMORY}
}