package com.example.quizzapp_security.models;

public class SessionLog {
    public String user_id;
    public String email;
    public String started_at;
    public String ended_at;
    public Integer score;
    public Integer total;

    public SessionLog(String user_id, String email) {
        this.user_id = user_id;
        this.email = email;
    }
}
