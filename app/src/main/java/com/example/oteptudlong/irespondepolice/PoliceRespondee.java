package com.example.oteptudlong.irespondepolice;

public class PoliceRespondee {

    private String police_name, police_respond;

    public PoliceRespondee(String police_name, String police_respond) {
        this.police_name = police_name;
        this.police_respond = police_respond;
    }

    public String getPolice_name() {
        return police_name;
    }

    public void setPolice_name(String police_name) {
        this.police_name = police_name;
    }

    public String getPolice_respond() {
        return police_respond;
    }

    public void setPolice_respond(String police_respond) {
        this.police_respond = police_respond;
    }
}
