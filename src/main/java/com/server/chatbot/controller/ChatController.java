package com.server.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.server.chatbot.dto.ChatRequest;
import com.server.chatbot.dto.ChatResponse;
import com.server.chatbot.model.ChatMessage;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.service.ChatService;
import com.server.chatbot.service.ConversationService;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            HttpServletRequest request,
            @RequestBody ChatRequest req) {

        String sessionId = (String) request.getAttribute("sessionId");
        if (sessionId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing authenticated session");
        }
        System.out.println("CHAT_SESSION: " + sessionId);
        ChatResponse response = chatService.handleChat(sessionId, req.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public List<ChatMessage> postMethodName(HttpServletRequest request) {
        
        String sessionId = (String) request.getAttribute("sessionId");

        System.out.println("SESSION_FROM_JWT: " + sessionId);

        Conversation conversation = conversationService.getConversation(sessionId);

        return  conversation.getHistory();
    }
    
}
