package com.example.goodstart.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Expense {
    private String id;
    private String title;
    private double amount;
    private String category;
    private String addedBy;
    private String addedByName;
    private @ServerTimestamp Date timestamp;

    public Expense() {}

    public Expense(String title, double amount, String category,
                   String addedBy, String addedByName) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public String getAddedByName() { return addedByName; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
