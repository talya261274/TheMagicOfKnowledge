package com.example.themagicofknowledge.models;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String avatar;

    public Map<String, UserChild> childrenList;

    public UserParent() {
        this.childrenList = new HashMap<>();
    }

    public UserParent(String id, String firstName, String lastName, String email,
                      String phone, String birthDate, String userName,
                      String password, boolean isAdmin, Map<String, UserChild> childrenList) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.birthDate = birthDate;
        this.userName = userName;
        this.password = password;
        this.isAdmin = isAdmin;
        this.childrenList = childrenList != null ? childrenList : new HashMap<>();
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

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
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

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Map<String, UserChild> getChildrenList() {
        if (childrenList == null) {
            childrenList = new HashMap<>();
        }
        return childrenList;
    }

    public void setChildrenList(Map<String, UserChild> childrenList) {
        this.childrenList = childrenList;
    }

    @Exclude
    public List<UserChild> getChildrenListAsList() {
        if (childrenList == null) return new ArrayList<>();
        return new ArrayList<>(childrenList.values());
    }

    @NonNull
    @Override
    public String toString() {
        return "UserParent{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", childrenListCount=" + (childrenList != null ? childrenList.size() : 0) +
                '}';
    }
}