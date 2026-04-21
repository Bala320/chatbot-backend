package com.server.chatbot.service;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import com.server.chatbot.model.Conversation;

@Service
public class ConversationCacheService {

    private static final Logger log = LoggerFactory.getLogger(ConversationCacheService.class);
    private static final String KEY_PREFIX = "conversation";

    private final RedisTemplate<String, Conversation> redisTemplate;
    private final Duration conversationTtl;

    public ConversationCacheService(
            RedisTemplate<String, Conversation> redisTemplate,
            @Value("${app.cache.conversation-ttl-seconds:604800}") long conversationTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.conversationTtl = Duration.ofSeconds(conversationTtlSeconds);
    }

    public Optional<Conversation> get(String sessionId, String pageKey) {
        String key = buildKey(sessionId, pageKey);
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (DataAccessException | SerializationException | ClassCastException ex) {
            log.warn(
                    "Redis read failed for sessionId={} pageKey={}. Evicting stale cache and falling back to PostgreSQL.",
                    sessionId,
                    pageKey
            );
            evictQuietly(key);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn(
                    "Unexpected Redis cache error for sessionId={} pageKey={}. Falling back to PostgreSQL.",
                    sessionId,
                    pageKey
            );
            evictQuietly(key);
            return Optional.empty();
        }
    }

    public void put(Conversation conversation) {
        try {
            String key = buildKey(conversation.getSessionId(), conversation.getPageKey());
            redisTemplate.opsForValue().set(key, conversation, conversationTtl);
        } catch (DataAccessException ex) {
            log.warn(
                    "Redis write failed for sessionId={} pageKey={}. Conversation remains in PostgreSQL.",
                    conversation.getSessionId(),
                    conversation.getPageKey()
            );
        }
    }

    public void evict(String sessionId, String pageKey) {
        try {
            redisTemplate.delete(buildKey(sessionId, pageKey));
        } catch (DataAccessException ex) {
            log.warn("Redis delete failed for sessionId={} pageKey={}.", sessionId, pageKey);
        }
    }

    private void evictQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Failed to evict stale Redis key={}.", key);
        }
    }

    public String buildKey(String sessionId, String pageKey) {
        return KEY_PREFIX + ":" + sessionId + ":" + pageKey;
    }
}
