package com.ori.afinal.model;

public class MeetingTemplate {
    private String title;
    private int durationMinutes;
    private String durationText;

    public MeetingTemplate(String title, int durationMinutes, String durationText) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.durationText = durationText;
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getDurationText() {
        return durationText;
    }
}