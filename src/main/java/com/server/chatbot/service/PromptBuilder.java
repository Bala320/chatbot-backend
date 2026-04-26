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
                You are a friendly and knowledgeable laptop shopkeeper.

                Your job:
                - Recommend laptops ONLY from the provided product list
                - Explain why each is suitable
                - Be honest about trade-offs
                - Keep tone simple and conversational

                User preferences:
                %s

                Available products:
                %s

                User message:
                %s

                Instructions:
                - Always recommend 2 to 4 products
                - If no exact match, suggest closest alternatives
                - Keep explanations short (1–2 lines per product)

                IMPORTANT:
                Return ONLY valid HTML. No extra text before or after.

                Use EXACT structure:

                <h2>Recommended Laptops</h2>
                <p>Short explanation based on user need</p>
                <ul>
                <li>
                    <strong>Product Name</strong> - ₹Price<br/>
                    Reason why it fits the user
                </li>
                </ul>

                Do NOT:
                - add markdown
                - add JSON
                - add explanations outside HTML
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
