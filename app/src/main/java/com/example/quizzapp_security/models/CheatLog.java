package com.example.quizzapp_security.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheatLog implements java.io.Serializable {
    public String user_id;
    public String quiz_id;
    public String alert_type;
    public String details;
    public String timestamp;

    public CheatLog(String user_id, String quiz_id, String alert_type, String details) {
        this.user_id    = user_id;
        this.quiz_id    = quiz_id;
        this.alert_type = alert_type;
        this.details    = details;
        this.timestamp  = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
