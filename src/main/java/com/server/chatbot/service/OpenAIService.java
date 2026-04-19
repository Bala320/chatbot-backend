package com.server.chatbot.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class OpenAIService {

    // private static final String DEFAULT_MODEL = "gpt-4.1-mini";

    private final WebClient webClient;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.base.url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    public OpenAIService(@Value("${openai.base.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        System.out.println("BASE URL = " + baseUrl);
    }

    public String getChatResponse(List<Map<String, String>> messages) {
        validateApiKey();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages
        );

        Map<String, Object> response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response was empty");
        }

        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response did not contain choices");
        }

        Object firstChoiceObject = choices.get(0);
        if (!(firstChoiceObject instanceof Map<?, ?> firstChoice)) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response choice format was invalid");
        }

        Object messageObject = firstChoice.get("message");
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response message format was invalid");
        }

        Object contentObject = messageMap.get("content");
        if (!(contentObject instanceof String content) || content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response did not contain message content");
        }

        return content;
    }

    public void streamResponse(List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onComplete) {
        String reply = getChatResponse(messages);
        onChunk.accept(reply);
        onComplete.run();
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI API key is not configured");
        }
    }
}
