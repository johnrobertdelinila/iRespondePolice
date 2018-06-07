package com.example.oteptudlong.irespondepolice;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class Report implements Serializable {

    private String uid, location_name, police_report_ids, status, incident, description, police_repondents, key;
    private Object timestamp;
    private Map<String, Double> location_latlng;
    private long numOfRespondee;
    private Map<String, String> images;

    public Report() {}

    public Report(String uid, String location_name, String police_report_ids, String status, String incident, String description, String police_repondents, Map<String, String> timestamp, Map<String, Double> location_latlng, Map<String, String> images) {
        this.uid = uid;
        this.location_name = location_name;
        this.police_report_ids = police_report_ids;
        this.status = status;
        this.incident = incident;
        this.description = description;
        this.police_repondents = police_repondents;
        this.timestamp = timestamp;
        this.location_latlng = location_latlng;
        this.images = images;
    }

    public Map<String, String> getImages() {
        return images;
    }

    public void setImages(Map<String, String> images) {
        this.images = images;
    }

    public long getNumOfRespondee() {
        return numOfRespondee;
    }

    public void setNumOfRespondee(long numOfRespondee) {
        this.numOfRespondee = numOfRespondee;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolice_repondents() {
        return police_repondents;
    }

    public void setPolice_repondents(String police_repondents) {
        this.police_repondents = police_repondents;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Double> getLocation_latlng() {
        return location_latlng;
    }

    public void setLocation_latlng(Map<String, Double> location_latlng) {
        this.location_latlng = location_latlng;
    }
}
