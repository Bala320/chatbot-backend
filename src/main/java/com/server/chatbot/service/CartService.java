package com.server.chatbot.service;

import java.time.Duration;
import java.util.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CartService(@Qualifier("objectRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getKey(String sessionId) {
        return "cart:" + sessionId;
    }

    public List<Map<String, Object>> getCart(String sessionId) {
        Object data = redisTemplate.opsForValue().get(getKey(sessionId));
        if (data == null) return new ArrayList<>();
        return (List<Map<String, Object>>) data;
    }

    public void saveCart(String sessionId, List<Map<String, Object>> cart) {
        redisTemplate.opsForValue().set(getKey(sessionId), cart, Duration.ofDays(7));
    }

    public void addToCart(String sessionId, int productId) {
        List<Map<String, Object>> cart = getCart(sessionId);

        Optional<Map<String, Object>> existing =
                cart.stream().filter(item -> (int) item.get("id") == productId).findFirst();

        if (existing.isPresent()) {
            int qty = (int) existing.get().get("quantity");
            existing.get().put("quantity", qty + 1);
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("id", productId);
            item.put("quantity", 1);
            cart.add(item);
        }

        saveCart(sessionId, cart);
    }

    public void clearCart(String sessionId) {
        redisTemplate.delete("cart:" + sessionId);
    }
}
