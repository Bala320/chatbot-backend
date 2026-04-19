package com.server.chatbot.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class TokenService {

    public static final int ACCESS_EXPIRY_SECONDS = 15 * 60;
    public static final int REFRESH_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    @Value("${security.jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 characters long");
        }

        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String sessionId) {
        return Jwts.builder()
                .claim("sid", sessionId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (ACCESS_EXPIRY_SECONDS * 1000L)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Instant refreshExpiryTime() {
        return now().plusSeconds(REFRESH_EXPIRY_SECONDS);
    }

    public Instant now() {
        return Instant.now();
    }

    public String validateTokenAndGetSessionId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("sid", String.class);
    }
}
