package com.server.chatbot.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class OpenAIService {

    private static final String DEFAULT_OPENAI_COMPAT_URL = "https://generativelanguage.googleapis.com/v1beta/openai";
    private static final String DEFAULT_NATIVE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final WebClient openAiCompatClient;
    private final WebClient nativeGeminiClient;

    @Value("${app.gemini.key}")
    private String apiKey;

    @Value("${app.gemini.openai-url:${app.gemini.url:" + DEFAULT_OPENAI_COMPAT_URL + "}}")
    private String openAiCompatBaseUrl;

    @Value("${app.gemini.native-url:" + DEFAULT_NATIVE_URL + "}")
    private String nativeBaseUrl;

    @Value("${app.gemini.model}")
    private String model;

    public OpenAIService(
            @Value("${app.gemini.openai-url:${app.gemini.url:" + DEFAULT_OPENAI_COMPAT_URL + "}}") String openAiCompatBaseUrl,
            @Value("${app.gemini.native-url:" + DEFAULT_NATIVE_URL + "}") String nativeBaseUrl) {
        this.openAiCompatClient = WebClient.builder()
                .baseUrl(openAiCompatBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.nativeGeminiClient = WebClient.builder()
                .baseUrl(nativeBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String getChatResponse(List<Map<String, String>> messages) {
        validateApiKey();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages
        );

        try {
            Map<String, Object> response = openAiCompatClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractChatCompletionText(response);
        } catch (WebClientResponseException.NotFound ex) {
            return getNativeGeminiResponse(messages);
        } catch (WebClientResponseException ex) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Gemini request failed: " + ex.getStatusCode().value() + " " + ex.getStatusText(),
                    ex
            );
        }
    }

    private String extractChatCompletionText(Map<String, Object> response) {
        if (response == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini response was empty");
        }

        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini response did not contain choices");
        }

        Object firstChoiceObject = choices.get(0);
        if (!(firstChoiceObject instanceof Map<?, ?> firstChoice)) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini response choice format was invalid");
        }

        Object messageObject = firstChoice.get("message");
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini response message format was invalid");
        }

        Object contentObject = messageMap.get("content");
        if (!(contentObject instanceof String content) || content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini response did not contain message content");
        }

        return content;
    }

    private String getNativeGeminiResponse(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
                "contents", toGeminiContents(messages)
        );

        try {
            Map<String, Object> response = nativeGeminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractNativeGeminiText(response);
        } catch (WebClientResponseException ex) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Gemini request failed after fallback: " + ex.getStatusCode().value() + " " + ex.getStatusText(),
                    ex
            );
        }
    }

    private List<Map<String, Object>> toGeminiContents(List<Map<String, String>> messages) {
        return messages.stream()
                .map(message -> Map.of(
                        "role", toGeminiRole(message.get("role")),
                        "parts", List.of(Map.of("text", message.getOrDefault("content", "")))
                ))
                .collect(Collectors.toList());
    }

    private String toGeminiRole(String role) {
        if ("assistant".equalsIgnoreCase(role) || "model".equalsIgnoreCase(role)) {
            return "model";
        }
        return "user";
    }

    private String extractNativeGeminiText(Map<String, Object> response) {
        if (response == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback response was empty");
        }

        Object candidatesObject = response.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback response did not contain candidates");
        }

        Object firstCandidateObject = candidates.get(0);
        if (!(firstCandidateObject instanceof Map<?, ?> firstCandidate)) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback candidate format was invalid");
        }

        Object contentObject = firstCandidate.get("content");
        if (!(contentObject instanceof Map<?, ?> contentMap)) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback content format was invalid");
        }

        Object partsObject = contentMap.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback response did not contain parts");
        }

        String text = parts.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(part -> part.get("text"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.joining("\n"))
                .trim();

        if (text.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini fallback response did not contain text");
        }

        return text;
    }

    public void streamResponse(List<Map<String, String>> messages, Consumer<String> onChunk, Runnable onComplete) {
        String reply = getChatResponse(messages);
        onChunk.accept(reply);
        onComplete.run();
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Gemini API key is not configured");
        }
    }

    private String cleanJson(String json) {
        // Remove markdown code blocks if present
        if (json.contains("```json")) {
            json = json.replaceAll("```json", "").replaceAll("```", "");
        } else if (json.contains("```")) {
            json = json.replaceAll("```", "");
        }
        return json.trim();
    }

    public String callLLM(String prompt) {

        List<Map<String, String>> messages = List.of(
            Map.of("role", "user", "content", prompt)
        );

        return getChatResponse(messages);
    }

    public UserPreference parseJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            json = cleanJson(json);

            UserPreference pref = mapper.readValue(json, UserPreference.class);

            // safety defaults
            if (pref.getUseCase() == null) {
                pref.setUseCase(List.of());
            }
            System.out.println("Extracted Pref: " + pref);
            return pref;

        } catch (Exception e) {
            System.out.println("Failed JSON: " + json);
            return new UserPreference(); // fallback instead of crash
        }
    }

    public String classifyIntent(String message) {
        String prompt = """
        Classify this message into:
        GREETING, PRODUCT_QUERY, CASUAL_CHAT

        Message: "%s"

        Return only one word.
        """.formatted(message);

        return callLLM(prompt);
    }

    public UserPreference extractPreferences(String message) {

        String prompt = """
        Extract laptop requirements from this message.

        Message: "%s"

        Rules:
        - budget should be a number (no symbols)
        - use_case can include: gaming, coding, office, student, productivity
        - battery: low, medium, high
        - performance: low, medium, high
        - If something is missing, return null for that field

        Return ONLY JSON (no explanation):
        {
            "budget": number,
            "useCase": [],
            "battery": "low|medium|high",
            "performance": "low|medium|high",
            "brand": "string"
        }
        """.formatted(message);

        String response = callLLM(prompt);

        return parseJson(response);
    }

    public String getRawCompletion(String prompt) {
        return callLLM(prompt);
    }
}
