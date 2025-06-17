package com.example.barberbookingapp.models;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// Model class for a hair dresser (worker)
// Contains fields for username, email, phone, role, holidays, and appointments

public class Worker {
    private String id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private List<Appointments> appointments;
    private List<String> holidays;

    public Worker() {
        this.holidays = new ArrayList<>();
        this.appointments = new ArrayList<>();
    }

    public Worker(String username, String email, String phone, String role, List<Appointments> appointments) {
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.appointments = appointments != null ? appointments : new ArrayList<>();
        this.holidays = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Appointments> getAppointments() {
        return appointments != null ? appointments : new ArrayList<>();
    }

    public void setAppointments(List<Appointments> appointments) {
        this.appointments = appointments != null ? appointments : new ArrayList<>();
    }

    public List<String> getHolidays() {
        return holidays != null ? holidays : new ArrayList<>();
    }

    public void setHolidays(List<String> holidays) {
        this.holidays = holidays != null ? holidays : new ArrayList<>();
    }

    public void addHoliday(String date) {
        if (this.holidays == null) {
            this.holidays = new ArrayList<>();
        }
        if (!this.holidays.contains(date)) {
            this.holidays.add(date);
        }
    }

    public void removeHoliday(String date) {
        if (this.holidays != null) {
            this.holidays.remove(date);
        }
    }

    public boolean isHoliday(String date) {
        return this.holidays != null && this.holidays.contains(date);
    }
}