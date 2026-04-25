package com.server.chatbot.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.server.chatbot.model.Product;

@Service
public class PromptBuilder {

    public String buildShopkeeperPrompt(
            List<Map<String, String>> history,
            UserPreference pref,
            List<Product> products,
            String userMessage) {

            return """
                You are a friendly laptop shopkeeper.

                Rules:
                - Recommend ONLY from given products
                - Be honest about pros/cons
                - Keep it simple and helpful

                User preferences:
                %s

                Available products:
                %s

                User message:
                %s

                Give recommendation:

                If no exact match, suggest closest alternatives and explain why.
                """.formatted(
                                prefToString(pref),
                                productsToString(products),
                                userMessage
                        );
                    }

    private String prefToString(UserPreference pref) {
        return String.format(
                "Budget: %s, UseCase: %s, Battery: %s, Performance: %s",
                pref.getBudget(),
                pref.getUseCase(),
                pref.getBattery(),
                pref.getPerformance()
        );
    }

    private String productsToString(List<Product> products) {
        return products.stream()
                .map(p -> String.format(
                        "%s (%s) - ₹%d - Rating: %d stars",
                        p.getTitle(),
                        p.getCategory(),
                        p.getNewPrice(),
                        p.getStars()
                ))
                .toList()
                .toString();
    }
}
