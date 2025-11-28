package com.example.themagicofknowledge.models;

import java.util.ArrayList;

public class UserParent {
    public String id;
    public String firstName;
    public String lastName;
    public String email;
    public String phone;
    public String birthDate;
    public String userName;
    public String password;

    public boolean isAdmin;

    public ArrayList<UserChild> childrenList;

    public UserParent(String uid, String fName, String lName, String email, String phone, String BDate, String UName, String password, boolean b) {
    }

    public UserParent(String id, String firstName, String lastName,
                      String email, String phone, String userName, String password,
                      boolean isAdmin, ArrayList<UserChild> childrenList) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.birthDate = phone;
        this.password = password;
        this.isAdmin = isAdmin;
        this.childrenList = childrenList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;

    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String phone) {
        this.birthDate = birthDate;

    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String phone) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public ArrayList<UserChild> getChildrenList() {
        return childrenList;
    }

    public void setChildrenList(ArrayList<UserChild> childrenList) {
        this.childrenList = childrenList;
    }

    @Override
    public String toString() {
        return "UserParent{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", isAdmin=" + isAdmin +
                ", childrenList=" + childrenList +
                '}';
    }
}
