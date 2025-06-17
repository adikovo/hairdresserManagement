package com.example.barberbookingapp.models;

// Model class for an appointment
// Contains fields for service type, user ID, date/time, and hair dresser username

public class Appointments {
    private String serviceType;
    private String clientId;
    private String dateTime;
    private String hairdresserUsername; // Username of the hairdresser for this appointment
    private boolean personalAppointment; // Flag to indicate if this is a personal appointment

    public Appointments() {
    }

    /*
     * Class for representing an appointment in Firebase
     * Relevant fields are: client ID, service type, date and time
     * The client ID is created from client creation and the key received from
     * Firebase
     */

    public Appointments(String serviceType, String clientId, String dateTime, String hairdresserUsername) {
        this.serviceType = serviceType;
        this.clientId = clientId;
        this.dateTime = dateTime;
        this.hairdresserUsername = hairdresserUsername;
        this.personalAppointment = false;
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

    public boolean isPersonalAppointment() {
        return personalAppointment;
    }

    public void setPersonalAppointment(boolean personalAppointment) {
        this.personalAppointment = personalAppointment;
    }
}
