package com.ori.afinal.model;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private String id;
    private String title;
    private String location;
    private String description;
    private String date;        // תאריך הפגישה (למשל 12/05/2026)
    private String startTime;   // שעת התחלה
    private String endTime;     // שעת סיום
    private boolean isOnline;   // האם מקוון
    private String type;        // נשמור כ-"Meeting" כברירת מחדל
    private List<String> invitedUserIds; // רשימת ה-ID של המוזמנים

    public Event() {
        // בנאי ריק חובה עבור Firebase
        this.invitedUserIds = new ArrayList<>();
    }

    public Event(String id, String title, String location, String description, String date, String startTime, String endTime, boolean isOnline) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOnline = isOnline;
        this.type = "Meeting";
        this.invitedUserIds = new ArrayList<>();
    }

    // פונקציית עזר לתצוגה ב-Adapter הישן
    public String getDateTime() {
        return date + " " + startTime + " - " + endTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getInvitedUserIds() { return invitedUserIds; }
    public void setInvitedUserIds(List<String> invitedUserIds) { this.invitedUserIds = invitedUserIds; }

    // פונקציית עזר להוספת מוזמן
    public void addInvitee(String userId) {
        if (this.invitedUserIds == null) {
            this.invitedUserIds = new ArrayList<>();
        }
        this.invitedUserIds.add(userId);
    }
}