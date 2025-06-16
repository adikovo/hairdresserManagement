package com.example.barberbookingapp.models;

public class InventoryItem {
    private String name;
    private int quantity;
    private String key;

    public InventoryItem() {
    }

    /*
    מחלקה הנועדה לייצוג של פריט מרשימת המלאי הפיירבייס
    לכל פריט יש שדה וכמות
    המפתח שלו הוא מפתח המתקבל מהפיירבייס
    */

    public InventoryItem(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
