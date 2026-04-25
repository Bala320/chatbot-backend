package com.server.chatbot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.chatbot.model.Product;
import com.server.chatbot.service.OpenAIService;
import com.server.chatbot.service.ProductService;
import com.server.chatbot.service.SessionService;
import com.server.chatbot.service.UserPreference;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final SessionService sessionService;

    private final ProductService productService;

    private final OpenAIService openAIService;

    public AuthController(SessionService sessionService, ProductService productService, OpenAIService openAIService) {
        this.sessionService = sessionService;
        this.productService = productService;
        this.openAIService = openAIService;
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

    @GetMapping("/test-search")
    public List<Product> testSearch() {

        UserPreference pref = new UserPreference();
        pref.setBudget(80000);

        return productService.search(pref);
    }

    @GetMapping("/test-extract")
    public UserPreference testExtract(@RequestParam String msg) {
        return openAIService.extractPreferences(msg);
    }
}
