package com.server.chatbot.service;

import java.util.List;

public class UserPreference {

    private Integer budget;
    private List<String> useCase;
    private String battery;
    private String performance;
    private String brand;

    public Integer getBudget() {
        return budget;
    }

    public void setBudget(Integer budget) {
        this.budget = budget;
    }

    public List<String> getUseCase() {
        return useCase;
    }

    public void setUseCase(List<String> useCase) {
        this.useCase = useCase;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getPerformance() {
        return performance;
    }

    public void setPerformance(String performance) {
        this.performance = performance;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
