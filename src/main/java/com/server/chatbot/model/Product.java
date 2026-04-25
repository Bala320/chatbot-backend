package com.server.chatbot.model;

import java.util.*;

public class Product {

    private int id;
    private String brand;
    private String title;
    private String category;

    private List<String> features;

    private int newPrice;
    private int oldPrice;

    private int stars;
    private int reviewsCount;

    private String badge;
    private String accent;
    private String delivery;

    // Getters & Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public int getNewPrice() { return newPrice; }
    public void setNewPrice(int newPrice) { this.newPrice = newPrice; }

    public int getOldPrice() { return oldPrice; }
    public void setOldPrice(int oldPrice) { this.oldPrice = oldPrice; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }

    public String getDelivery() { return delivery; }
    public void setDelivery(String delivery) { this.delivery = delivery; }
}
