package com.example.oteptudlong.irespondepolice;

public class PoliceReport {

    private String date, time, case_no, incident, citizen_report_id, witness, detail_of_event, actions_taken, police_id;
    private Double latitude, longtitude;

    public PoliceReport() {}

    public PoliceReport(String date, String time, String case_no, String incident, String citizen_report_id, String witness, String detail_of_event, String actions_taken, String police_id, Double latitude, Double longtitude) {
        this.date = date;
        this.time = time;
        this.case_no = case_no;
        this.incident = incident;
        this.citizen_report_id = citizen_report_id;
        this.witness = witness;
        this.detail_of_event = detail_of_event;
        this.actions_taken = actions_taken;
        this.police_id = police_id;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCase_no() {
        return case_no;
    }

    public void setCase_no(String case_no) {
        this.case_no = case_no;
    }

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
    }

    public String getCitizen_report_id() {
        return citizen_report_id;
    }

    public void setCitizen_report_id(String citizen_report_id) {
        this.citizen_report_id = citizen_report_id;
    }

    public String getWitness() {
        return witness;
    }

    public void setWitness(String witness) {
        this.witness = witness;
    }

    public String getDetail_of_event() {
        return detail_of_event;
    }

    public void setDetail_of_event(String detail_of_event) {
        this.detail_of_event = detail_of_event;
    }

    public String getActions_taken() {
        return actions_taken;
    }

    public void setActions_taken(String actions_taken) {
        this.actions_taken = actions_taken;
    }

    public String getPolice_id() {
        return police_id;
    }

    public void setPolice_id(String police_id) {
        this.police_id = police_id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(Double longtitude) {
        this.longtitude = longtitude;
    }
}
