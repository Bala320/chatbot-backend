package com.server.chatbot.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.server.chatbot.model.ChatMessage;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.repository.ConversationRepository;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    public Conversation getConversation(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Session is missing");
        }

        return conversationRepository
                .findBySessionIdAndPageKey(sessionId, "default-page")
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation not found"));
    }

    public void saveConversation(Conversation conversation) {
        conversationRepository.save(conversation);
    }

    public void addMessage(Conversation conversation, String role, String content) {
        List<ChatMessage> history = conversation.getHistory();
        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(new ChatMessage(role, content));
        conversation.setHistory(history);
    }
}
