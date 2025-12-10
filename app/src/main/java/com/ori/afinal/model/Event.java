package com.ori.afinal.model;

public class Event {


    protected  String id;

    private String title;
    private String description;
    private String dateTime;//calander
    private String type;
    private String imageBase64;
    private String location;

    protected  String status;



    Integer maxNumOfParticipants;


    public Event(String id, String title, String description, String dateTime, String type, String imageBase64, String location, String status, Integer maxNumOfParticipants) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.type = type;
        this.imageBase64 = imageBase64;
        this.location = location;
        this.status = status;
        this.maxNumOfParticipants = maxNumOfParticipants;
    }

    public Event() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    @Override
    public String toString() {
        return "Event{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", type='" + type + '\'' +
                ", imageBase64='" + (imageBase64 != null ? "Image Included" : "No Image") + '\'' +
                '}';
    }
}
