package com.example.themagicofknowledge.models;

public class UserChild {

    public String id;
    public String age;
    public String level; //// רמה לימודית
    public String progress;

    public UserChild() {
    }



    public UserChild(String id, String userName, String password, String age, String level, String progress) {
        this.id = id;
        this.age = age;
        this.level = level;
        this.progress = progress;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "UserChild{" +
                "id='" + id + '\'' +
                ", age='" + age + '\'' +
                ", level='" + level + '\'' +
                ", progress='" + progress + '\'' +
                '}';
    }

}
