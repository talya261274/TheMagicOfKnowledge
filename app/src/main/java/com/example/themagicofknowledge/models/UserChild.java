package com.example.themagicofknowledge.models;

public class UserChild {

    public int id;
    ///public String parent_id;
    public int age;
    public String level;//// רמה לימודית
    /// enum: 3-4 5-6 7-8 ???
    public String progress;

    public static int counterUserChild;

    public UserChild() {
    }
    public UserChild(String userName, int age) {
        counterUserChild++;
        id = counterUserChild;
        /// in the database for all parents and childs
        /// find child - find child's parent
        ///  parent_id = parent(that we found).id
        this.age = age;
        switch (age){
            case 3: case 4:
                level = "three-four";
                break;
            case 5: case 6:
                level = "five-six";
                break;
            case 7: case 8:
                level = "seven-eight";
        }
        this.progress = progress;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
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
