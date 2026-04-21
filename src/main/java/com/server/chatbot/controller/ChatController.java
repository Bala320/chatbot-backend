package com.server.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.server.chatbot.dto.ChatRequest;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.service.ChatService;
import com.server.chatbot.service.ConversationService;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    private final ConversationService conversationService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<String> chat(
            HttpServletRequest request,
            @RequestBody ChatRequest req) {

        String sessionId = (String) request.getAttribute("sessionId");
        if (sessionId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing authenticated session");
        }

        String response = chatService.handleChat(sessionId, req.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/history")
    public String postMethodName(HttpServletRequest request) {
        //TODO: process POST request
        String sessionId = (String) request.getAttribute("sessionId");

        Conversation conversation = conversationService.getConversation(sessionId);

        
        return entity;
    }
    
}
