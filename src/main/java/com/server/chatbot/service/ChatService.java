package com.server.chatbot.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.server.chatbot.model.Conversation;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ChatService {

    private final ConversationService conversationService;
    private final OpenAIService openAIService;

    public ChatService(ConversationService conversationService, OpenAIService openAIService) {
        this.conversationService = conversationService;
        this.openAIService = openAIService;
    }

    public String handleChat(String sessionId, String message) {
        validateMessage(message);

        Conversation conversation = conversationService.getConversation(sessionId);
        conversationService.addMessage(conversation, "user", message);

        List<Map<String, String>> messages = conversation.getHistory().stream()
                .map(msg -> Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()
                ))
                .toList();

        String reply = openAIService.getChatResponse(messages);
        conversationService.addMessage(conversation, "assistant", reply);
        conversationService.saveConversation(conversation);

        return reply;
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
