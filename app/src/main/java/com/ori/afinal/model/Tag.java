package com.ori.afinal.model;

public class Tag {
    String name;
    String color;
    Integer MaxNumOfParticipants;

    public Tag(String name, String color, Integer numOfParticipants) {
        this.name = name;
        this.color = color;
        MaxNumOfParticipants = numOfParticipants;
    }

    public Tag() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getMaxNumOfParticipants() {
        return  MaxNumOfParticipants;
    }

    public void setMaxNumOfParticipants(Integer maxNumOfParticipants) {
        MaxNumOfParticipants = maxNumOfParticipants;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", NumOfParticipants=" + MaxNumOfParticipants +
                '}';
    }
}
