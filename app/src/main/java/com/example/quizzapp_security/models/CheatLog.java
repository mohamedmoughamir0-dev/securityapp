package com.example.quizzapp_security.models;

public class CheatLog {
    public String user_id;
    public String quiz_id;
    public String alert_type;
    public String details;

    public CheatLog(String user_id, String quiz_id, String alert_type, String details) {
        this.user_id = user_id;
        this.quiz_id = quiz_id;
        this.alert_type = alert_type;
        this.details = details;
    }
}