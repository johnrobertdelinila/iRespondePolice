package com.example.oteptudlong.irespondepolice;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Report implements Serializable {

    private String strDate, strTime, uid, location_name, police_report_ids, status, images, incident, otwIds, arrivedIds, description, key;
    private Double latitude, longtitude;

    public Report() {}

    // hello world

    public Report(String strDate, String strTime, String uid, String location_name, String police_report_ids, String status, String images, String incident, String otwIds, String arrivedIds, String description, String key, Double latitude, Double longtitude) {
        this.strDate = strDate;
        this.strTime = strTime;
        this.uid = uid;
        this.location_name = location_name;
        this.police_report_ids = police_report_ids;
        this.status = status;
        this.images = images;
        this.incident = incident;
        this.otwIds = otwIds;
        this.arrivedIds = arrivedIds;
        this.description = description;
        this.key = key;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public String getStrDate() {
        return strDate;
    }

    public void setStrDate(String strDate) {
        this.strDate = strDate;
    }

    public String getStrTime() {
        return strTime;
    }

    public void setStrTime(String strTime) {
        this.strTime = strTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getLocation_name() {
        return location_name;
    }

    public void setLocation_name(String location_name) {
        this.location_name = location_name;
    }

    public String getPolice_report_ids() {
        return police_report_ids;
    }

    public void setPolice_report_ids(String police_report_ids) {
        this.police_report_ids = police_report_ids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
    }

    public String getOtwIds() {
        return otwIds;
    }

    public void setOtwIds(String otwIds) {
        this.otwIds = otwIds;
    }

    public String getArrivedIds() {
        return arrivedIds;
    }

    public void setArrivedIds(String arrivedIds) {
        this.arrivedIds = arrivedIds;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
