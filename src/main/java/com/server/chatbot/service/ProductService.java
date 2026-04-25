package com.server.chatbot.service;

import tools.jackson.core.type.TypeReference; 
import com.server.chatbot.model.Product;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class ProductService {

    private List<Product> products;

    @PostConstruct
    public void loadProducts() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = getClass().getResourceAsStream("/products.json");

            products = mapper.readValue(is, new TypeReference<List<Product>>() {});

            System.out.println("Loaded products: " + products.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load products.json", e);
        }
    }

    public List<Product> getAllProducts() {
        return products;
    }

    public List<Product> search(UserPreference pref) {

        // safety: if no preference, return top products
        if (pref == null) {
            return products.stream().limit(5).toList();
        }

        return products.stream()

            // budget → use newPrice
            .filter(p -> pref.getBudget() == null || p.getNewPrice() <= pref.getBudget())

            // useCase → map to category
            .filter(p -> pref.getUseCase() == null ||
                    (p.getCategory() != null &&
                     pref.getUseCase().stream()
                        .anyMatch(u -> p.getCategory().toLowerCase().contains(u.toLowerCase()))))

            .filter(p -> pref.getBrand() == null ||
                    (p.getBrand() != null &&
                    p.getBrand().equalsIgnoreCase(pref.getBrand())))

            .limit(5)
            .toList();
    }
}