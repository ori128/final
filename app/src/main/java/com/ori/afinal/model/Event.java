package com.ori.afinal.model;

import java.util.ArrayList;
import java.util.List;

public class Event {

    private String id;
    private String title;
    private String description;
    private String dateTime; // format: yyyy-MM-dd HH:mm
    private String type;     // meeting / call / event
    private String location;
    private String status;   // ACTIVE / CANCELED / FINISHED
    private int maxNumOfParticipants;
    private User eventAdmin;
    private List<String> invitedUsers; // רשימת ה-UID של המשתמשים המוזמנים

    // חובה ל-Firebase
    public Event() {
        this.status = "ACTIVE";
        this.invitedUsers = new ArrayList<>();
    }

    // קונסטרקטור ליצירת אירוע חדש
    public Event(String id,
                 String title,
                 String description,
                 String dateTime,
                 String type,
                 String location,
                 int maxNumOfParticipants,
                 User eventAdmin) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.type = type;
        this.location = location;
        this.maxNumOfParticipants = maxNumOfParticipants;
        this.eventAdmin = eventAdmin;
        this.status = "ACTIVE";
        this.invitedUsers = new ArrayList<>();
    }

    // --- GETTERS & SETTERS ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMaxNumOfParticipants() { return maxNumOfParticipants; }
    public void setMaxNumOfParticipants(int maxNumOfParticipants) { this.maxNumOfParticipants = maxNumOfParticipants; }

    public User getEventAdmin() { return eventAdmin; }
    public void setEventAdmin(User eventAdmin) { this.eventAdmin = eventAdmin; }

    public List<String> getInvitedUsers() { return invitedUsers; }
    public void setInvitedUsers(List<String> invitedUsers) { this.invitedUsers = invitedUsers; }

    @Override
    public String toString() {
        return "Event{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", invitedCount=" + (invitedUsers != null ? invitedUsers.size() : 0) +
                '}';
    }
}