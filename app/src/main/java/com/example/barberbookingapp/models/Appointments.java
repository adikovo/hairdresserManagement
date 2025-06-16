package com.example.barberbookingapp.models;

// Model class for an appointment
// Contains fields for service type, user ID, status, date/time, and hair dresser username

public class Appointments {
    private String serviceType;
    private String clientId;
    private String status;
    private String dateTime;
    private String hairdresserUsername; // Username of the hairdresser for this appointment

    public Appointments() {}


    /*
    Class for representing an appointment in Firebase
    Relevant fields are: client ID, service type, date and time
    The client ID is created from client creation and the key received from Firebase
    * */

    public Appointments(String serviceType, String clientId, String status, String dateTime, String hairdresserUsername) {
        this.serviceType = serviceType;
        this.clientId = clientId;
        this.status = status;
        this.dateTime = dateTime;
        this.hairdresserUsername = hairdresserUsername;
    }


    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getHairdresserUsername() {
        return hairdresserUsername;
    }

    public void setHairdresserUsername(String hairdresserUsername) {
        this.hairdresserUsername = hairdresserUsername;
    }
}

