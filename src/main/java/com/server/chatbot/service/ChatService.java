package com.server.chatbot.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.server.chatbot.dto.ChatResponse;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.model.Product;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ChatService {

    private final ConversationService conversationService;
    private final OpenAIService openAIService;
    private final ProductService productService;
    private final PromptBuilder promptBuilder;

    public ChatService(ConversationService conversationService, OpenAIService openAIService, ProductService productService, PromptBuilder promptBuilder) {
        this.conversationService = conversationService;
        this.openAIService = openAIService;
        this.productService = productService;
        this.promptBuilder = promptBuilder;
    }

    private UserPreference merge(UserPreference oldPref, UserPreference newPref) {

        if (oldPref == null) return newPref;
        if (newPref == null) return oldPref;

        if (newPref.getBudget() != null) {
            oldPref.setBudget(newPref.getBudget());
        }

        if (newPref.getUseCase() != null) {
            oldPref.setUseCase(newPref.getUseCase());
        }

        if (newPref.getBattery() != null) {
            oldPref.setBattery(newPref.getBattery());
        }

        if (newPref.getPerformance() != null) {
            oldPref.setPerformance(newPref.getPerformance());
        }

        return oldPref;
    }

    public ChatResponse handleChat(String sessionId, String message) {

        validateMessage(message);

        // 1. Load conversation
        Conversation conversation = conversationService.getConversation(sessionId);
        conversationService.addMessage(conversation, "user", message);

        // 2. Get history
        List<Map<String, String>> history = conversation.getHistory().stream()
                .map(msg -> Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()
                ))
                .toList();

        String reply;

        // ✅ ADD HERE (exact spot)
        if (isGreeting(message)) {
            reply = "Hey! 👋 Looking for a laptop today? Tell me your budget or use case like gaming, coding, etc.";

            // save assistant response
            conversationService.addMessage(conversation, "assistant", reply);
            conversationService.saveConversation(conversation);

            return new ChatResponse(reply, List.of());// 🔥 IMPORTANT: exit early
        }

         // 🔥 3. Extract preferences (AI)
        UserPreference newPref = openAIService.extractPreferences(message);

        // 🔥 4. Load old preferences
        UserPreference oldPref = conversation.getPreferences();

        // 🔥 5. Merge
        UserPreference mergedPref = merge(oldPref, newPref);

        // 🔥 6. Filter products (Java)
        List<Product> products = productService.search(mergedPref);
        try {
             // ⚠️ Handle no results
            if (products.isEmpty()) {
                products = productService.getAllProducts()
                        .stream()
                        .limit(5)
                        .toList();
            }

            // 🔥 ALWAYS build prompt
            String prompt = promptBuilder.buildShopkeeperPrompt(
                    history,
                    mergedPref,
                    products,
                    message
            );

            // 🔥 ALWAYS call LLM
            reply = openAIService.callLLM(prompt);

            // 🔥 9. Save preferences
            conversation.setPreferences(mergedPref);

        } catch (Exception e) {
            // fallback (very important)
            reply = "Something went wrong. Can you rephrase your request?";
            e.printStackTrace();
        }

        // 10. Save response
        conversationService.addMessage(conversation, "assistant", reply);
        conversationService.saveConversation(conversation);
        System.out.println("Products size: " + products.size());
        return new ChatResponse(reply, products);
    }

    private boolean isGreeting(String message) {
        String msg = message.toLowerCase().trim();

        return msg.equals("hi") ||
            msg.equals("hello") ||
            msg.equals("hey") ||
            msg.equals("hi there");
    }

    public SseEmitter handleChatStream(String sessionId, String message) {
        validateMessage(message);

        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            try {
                Conversation conversation = conversationService.getConversation(sessionId);
                conversationService.addMessage(conversation, "user", message);

                List<Map<String, String>> messages = conversation.getHistory().stream()
                        .map(msg -> Map.of(
                                "role", msg.getRole(),
                                "content", msg.getContent()
                        ))
                        .toList();

                StringBuilder fullReply = new StringBuilder();

                openAIService.streamResponse(
                        messages,
                        chunk -> {
                            try {
                                fullReply.append(chunk);
                                emitter.send(chunk);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                conversationService.addMessage(conversation, "assistant", fullReply.toString());
                                conversationService.saveConversation(conversation);
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Message must not be blank");
        }
    }
}
