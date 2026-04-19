package com.server.chatbot.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.chatbot.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final SessionService sessionService;

    public AuthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/session")
    public ResponseEntity<?> createSession(HttpServletResponse response) {
            String sessionId = sessionService.createSession(response);

            return ResponseEntity.ok(sessionId);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

        sessionService.refreshSession(request, response);
        return ResponseEntity.ok().body(Map.of("ok", true));
    }
}
