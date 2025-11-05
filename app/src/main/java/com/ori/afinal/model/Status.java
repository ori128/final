package com.ori.afinal.model;

public class Status {

    Boolean invited;
    Boolean accepted;
    Boolean  declined;
    Boolean maybe;

    public Status(Boolean invited, Boolean accepted, Boolean declined, Boolean maybe) {
        this.invited = invited;
        this.accepted = accepted;
        this.declined = declined;
        this.maybe = maybe;
    }

    public Status() {
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

    public Boolean getMaybe() {
        return maybe;
    }

    public void setMaybe(Boolean maybe) {
        this.maybe = maybe;
    }

    @Override
    public String toString() {
        return "Status{" +
                "invited=" + invited +
                ", accepted=" + accepted +
                ", declined=" + declined +
                ", maybe=" + maybe +
                '}';
    }
}
