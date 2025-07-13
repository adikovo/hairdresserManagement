package com.example.hairsalonbookingapp.models;

// Model class for an appointment
// Contains fields for service type, user ID, date/time, and hair dresser username

public class Appointments {
    private String serviceType;
    private String clientId;
    private String dateTime;
    private String hairdresserUsername;
    private boolean personalAppointment;
    private String duration;
    private String status;
    private String calendarEventId;

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
        this.duration = getDurationForService(serviceType);
        this.status = "scheduled";
    }

    public Appointments(String serviceType, String clientId, String dateTime, String hairdresserUsername,
            String duration) {
        this.serviceType = serviceType;
        this.clientId = clientId;
        this.dateTime = dateTime;
        this.hairdresserUsername = hairdresserUsername;
        this.personalAppointment = false;
        this.duration = duration;
        this.status = "scheduled";
    }

    public String getDuration() {
        return duration;
    }

    // method to get duration for a service type
    private String getDurationForService(String serviceType) {
        if (serviceType == null)
            return "30";

        if (serviceType.contains("Classic Mens Cut"))
            return "30";
        if (serviceType.contains("Premium Womens Cut"))
            return "60";
        if (serviceType.contains("Kids Cut"))
            return "30";
        if (serviceType.contains("Classic Shave"))
            return "15";
        if (serviceType.contains("Full Color"))
            return "90";
        if (serviceType.contains("Root Touch-up Color"))
            return "60";
        if (serviceType.contains("Full Highlight Service"))
            return "120";
        if (serviceType.contains("Quick Blow Dry"))
            return "30";
        if (serviceType.contains("Styling Blow Dry"))
            return "45";
        if (serviceType.contains("Beard Trim and Shape"))
            return "20";
        if (serviceType.contains("Keratin Straightening"))
            return "180";
        if (serviceType.contains("Japanese Straightening"))
            return "240";
        if (serviceType.contains("Deep Conditioning Treatment"))
            return "45";
        if (serviceType.contains("Hydrating Hair Mask"))
            return "30";
        if (serviceType.contains("Event Styling"))
            return "60";

        return "30"; // Default duration
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

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCalendarEventId() {
        return calendarEventId;
    }

    public void setCalendarEventId(String calendarEventId) {
        this.calendarEventId = calendarEventId;
    }
}
