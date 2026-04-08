package com.ori.afinal.model;

public class User {
    String id;
    String fname;
    String lname;
    String phone;
    String email;
    String password;
    String fullName; // הוספנו את השדה הרשמי
    Boolean admin;

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public User(String id, String fname, String lname, String phone, String email, String password, Boolean admin) {
        this.id = id;
        this.fname = fname;
        this.lname = lname;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.admin = admin;
        this.fullName = fname + " " + lname; // מתעדכן אוטומטית ביצירה
    }

    public User(String id, String fname, String lname, String phone, String email, String password) {
        this.id = id;
        this.fname = fname;
        this.lname = lname;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.fullName = fname + " " + lname;
    }

    public User() {
    }

    public User(String id, String fname, String lname) {
        this.id = id;
        this.fname = fname;
        this.lname = lname;
        this.fullName = fname + " " + lname;
    }

    // --- הנה התיקון הקריטי שמונע שגיאות ---
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    // --------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
        this.fullName = this.fname + " " + this.lname; // מתעדכן אוטומטית
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
        this.fullName = this.fname + " " + this.lname; // מתעדכן אוטומטית
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", fullName='" + fullName + '\'' +
                '}';
    }

    public void setUid(String uid) {
        this.id = uid;
    }
}