package com.example.goodstart.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class HouseholdTask {
    private String id;
    private String title;
    private String description;
    private String assignedTo;
    private String assignedToName;
    private String createdBy;
    private String createdByName;
    private boolean completed;
    private boolean urgent;
    private @ServerTimestamp Date timestamp;

    public HouseholdTask() {}

    public HouseholdTask(String title, String description, String assignedTo,
                         String assignedToName, String createdBy, String createdByName, boolean urgent) {
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
        this.assignedToName = assignedToName;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.completed = false;
        this.urgent = urgent;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
