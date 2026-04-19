package com.server.chatbot.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

     @Id
    private String refreshToken;

    private String sid;

    private Instant expiresAt;

    public RefreshToken() {}

    public RefreshToken(String refreshToken, String sid, Instant expiresAt) {
        this.refreshToken = refreshToken;
        this.sid = sid;
        this.expiresAt = expiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getSid() {
        return sid;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
