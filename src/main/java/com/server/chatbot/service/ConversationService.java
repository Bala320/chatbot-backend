package com.server.chatbot.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.server.chatbot.model.ChatMessage;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.repository.ConversationRepository;

import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ConversationService {

    public static final String DEFAULT_PAGE_KEY = "default-page";

    private final ConversationRepository conversationRepository;
    private final ConversationCacheService conversationCacheService;
    private final TokenService tokenService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationCacheService conversationCacheService,
            TokenService tokenService) {
        this.conversationRepository = conversationRepository;
        this.conversationCacheService = conversationCacheService;
        this.tokenService = tokenService;
    }

    public Conversation getConversation(String sessionId) {
        validateSessionId(sessionId);

        Conversation cachedConversation = conversationCacheService
                .get(sessionId, DEFAULT_PAGE_KEY)
                .orElse(null);

        if (cachedConversation != null) {
            if (isExpired(cachedConversation)) {
                conversationCacheService.evict(sessionId, DEFAULT_PAGE_KEY);
                throw new ResponseStatusException(GONE, "Conversation has expired");
            }

            return cachedConversation;
        }

        Conversation conversation = conversationRepository
                .findBySessionIdAndPageKey(sessionId, DEFAULT_PAGE_KEY)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation not found"));

        if (isExpired(conversation)) {
            conversationCacheService.evict(sessionId, DEFAULT_PAGE_KEY);
            throw new ResponseStatusException(GONE, "Conversation has expired");
        }

        conversationCacheService.put(conversation);
        return conversation;
    }

    public void saveConversation(Conversation conversation) {
        Conversation savedConversation = conversationRepository.save(conversation);
        conversationCacheService.put(savedConversation);
    }

    public void cacheConversation(Conversation conversation) {
        conversationCacheService.put(conversation);
    }

    public void addMessage(Conversation conversation, String role, String content) {
        List<ChatMessage> history = conversation.getHistory();
        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(new ChatMessage(role, content));
        conversation.setHistory(history);
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Session is missing");
        }
    }

    private boolean isExpired(Conversation conversation) {
        return conversation.getExpiresAt() != null && conversation.getExpiresAt().isBefore(tokenService.now());
    }
}
