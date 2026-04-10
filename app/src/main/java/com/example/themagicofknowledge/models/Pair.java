package com.example.themagicofknowledge.models;

public class Pair {
    private String left;
    private String right;
    private String id;

    public Pair() {}

    public Pair(String left, String right, String id) {
        this.left = left;
        this.right = right;
        this.id = id;
    }

    public String getLeft() {
        return left;
    }

    public void setLeft(String left) {
        this.left = left;
    }

    public String getRight() {
        return right;
    }

    public void setRight(String right) {
        this.right = right;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}