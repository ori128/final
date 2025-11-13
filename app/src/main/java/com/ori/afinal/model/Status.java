package com.ori.afinal.model;

public class Status {
    String UserId;
    Boolean invited;
    Boolean accepted;
    Boolean declined;

    public Status(String userId, Boolean invited, Boolean accepted, Boolean declined) {
        UserId = userId;
        this.invited = invited;
        this.accepted = accepted;
        this.declined = declined;
    }

    public Status() {
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public Boolean getInvited() {
        return invited;
    }

    public void setInvited(Boolean invited) {
        this.invited = invited;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getDeclined() {
        return declined;
    }

    public void setDeclined(Boolean declined) {
        this.declined = declined;
    }

    @Override
    public String toString() {
        return "Status{" +
                "UserId='" + UserId + '\'' +
                ", invited=" + invited +
                ", accepted=" + accepted +
                ", declined=" + declined +
                '}';
    }
}



