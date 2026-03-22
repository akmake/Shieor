package com.example.goodstart.models;

import java.util.Date;

public class ZmanItem {
    private String id;
    private String label;
    private String sublabel;
    private Date time;
    private boolean isAlarmEnabled;
    private String icon;

    public ZmanItem(String id, String label, String sublabel, Date time, String icon) {
        this.id = id;
        this.label = label;
        this.sublabel = sublabel;
        this.time = time;
        this.icon = icon;
        this.isAlarmEnabled = false;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getSublabel() { return sublabel; }
    public Date getTime() { return time; }
    public boolean isAlarmEnabled() { return isAlarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { isAlarmEnabled = alarmEnabled; }
    public String getIcon() { return icon; }
}
