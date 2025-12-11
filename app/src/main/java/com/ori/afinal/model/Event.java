package com.ori.afinal.model;

public class Event {

    private String id;
    private String title;
    private String description;
    private String dateTime;
    private String type;
    private String location;
    private String status;
    private Integer maxNumOfParticipants;


    protected  User eventAdmin;

    public Event(String id, String title, String description, String dateTime, String type, String location, String status, Integer maxNumOfParticipants, User eventAdmin) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.type = type;
        this.location = location;
        this.status = status;
        this.maxNumOfParticipants = maxNumOfParticipants;
        this.eventAdmin = eventAdmin;
    }






    public Event() {
        this.status = "will happen";
    }

    // --- GETTERS & SETTERS ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxNumOfParticipants() {
        return maxNumOfParticipants;
    }

    public void setMaxNumOfParticipants(Integer maxNumOfParticipants) {
        this.maxNumOfParticipants = maxNumOfParticipants;
    }

    public User getEventAdmin() {
        return eventAdmin;
    }

    public void setEventAdmin(User eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", status='" + status + '\'' +
                ", maxNumOfParticipants=" + maxNumOfParticipants +
                ", eventAdmin=" + eventAdmin +
                '}';
    }
}
