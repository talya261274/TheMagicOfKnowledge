package com.example.themagicofknowledge.models;

public class FlashCard {
    private String image;
    private String name;
    private String subject;

    public FlashCard() {}

    public FlashCard(String image, String name, String subject) {
        this.image = image;
        this.name = name;
        this.subject = subject;
    }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    @Override
    public String toString() {
        return "FlashCard{" +
                "image='" + image + '\'' +
                ", name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}