package com.example.barberbookingapp.models;

// Model class for a user (client or hair dresser)
// Contains fields for username, email, role, and phone number

public class User {
    public String username;
    public String email;
    public String role;
    public String phone;

    /*
    מחלקה זו נועדה לייצוג של כל משתמש בפיירבייס
    כל משתמש מיוצג על ידי שם משתמש, אימייל, תפקיד (ספר או לקוח) ומספר טלפון
    המפתח של המשתמש ניתן על ידי הפיירבייס
    */

    public User() {}
    public User(String username, String email, String role, String phone) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
