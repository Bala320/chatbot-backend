package com.server.chatbot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.chatbot.service.CartService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public void addToCart(HttpServletRequest request, @RequestBody Map<String, Integer> body) {

        String sessionId = (String) request.getAttribute("sessionId");
        int productId = body.get("productId");

        cartService.addToCart(sessionId, productId);
    }

    @GetMapping
    public List<Map<String, Object>> getCart(HttpServletRequest request) {
        String sessionId = (String) request.getAttribute("sessionId");
        return cartService.getCart(sessionId);
    }

    @DeleteMapping("/clear")
    public void clearCart(HttpServletRequest request) {
        String sessionId = (String) request.getAttribute("sessionId");
        cartService.clearCart(sessionId);
    }

}
