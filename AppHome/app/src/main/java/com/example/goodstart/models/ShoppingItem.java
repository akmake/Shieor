package com.example.goodstart.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ShoppingItem {
    public static final String CAT_SUPERMARKET = "supermarket";
    public static final String CAT_ELECTRONICS = "electronics";
    public static final String CAT_PHARMACY = "pharmacy";
    public static final String CAT_CLOTHING = "clothing";
    public static final String CAT_HOME = "home";
    public static final String CAT_OTHER = "other";

    private String id;
    private String name;
    private boolean checked;
    private String addedBy;
    private String addedByName;
    private String category;
    private int quantity;
    private @ServerTimestamp Date timestamp;

    public ShoppingItem() {}

    public ShoppingItem(String name, String addedBy, String addedByName, String category) {
        this.name = name;
        this.addedBy = addedBy;
        this.addedByName = addedByName;
        this.category = category != null ? category : CAT_SUPERMARKET;
        this.checked = false;
        this.quantity = 1;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
    public String getAddedByName() { return addedByName; }
    public void setAddedByName(String addedByName) { this.addedByName = addedByName; }
    public String getCategory() { return category != null ? category : CAT_SUPERMARKET; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity > 0 ? quantity : 1; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
