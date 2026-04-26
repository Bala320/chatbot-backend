package com.server.chatbot.dto;

import java.util.List;

import com.server.chatbot.model.Product;

public class ChatResponse {

    private String content;
    private List<Product> products;
    private boolean showProducts;

    public ChatResponse(String content, List<Product> products) {
        this.content = content;
        this.products = products;
        this.showProducts = products != null && !products.isEmpty();
    }

    public String getContent() {
        return content;
    }

    public List<Product> getProducts() {
        return products;
    }

    public boolean isShowProducts() {
        return showProducts;
    }
}
