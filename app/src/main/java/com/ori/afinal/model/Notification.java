package com.ori.afinal.model;

public class Notification {

    private String id;
    private String userId; // למי מיועדת ההתראה (מי שיקבל אותה)
    private String title;  // כותרת ההתראה (למשל: "שינוי בפגישה")
    private String message; // תוכן ההתראה (למשל: "הפגישה 'צוות' הוזזה לשעה 14:00")
    private String type; // סוג ההתראה: "INVITE", "UPDATE", "INFO", "REMOVED"
    private String eventId; // ה-ID של הפגישה הרלוונטית (כדי שנוכל ללחוץ ולעבור אליה)
    private long timestamp; // זמן יצירת ההתראה (לסידור לפי סדר כרונולוגי)
    private boolean isRead; // האם המשתמש כבר קרא את ההתראה?

    // קונסטרקטור ריק (חובה עבור Firebase)
    public Notification() {
    }

    public Notification(String id, String userId, String title, String message, String type, String eventId, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}