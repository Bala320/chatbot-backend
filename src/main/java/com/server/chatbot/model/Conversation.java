package com.server.chatbot.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.Type;

import com.server.chatbot.service.UserPreference;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Entity
@Table(name="conversations")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Transient
    private UserPreference preferences;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    private String pageKey;

    private Instant expiresAt;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<ChatMessage> history;

    public UserPreference getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreference preferences) {
        this.preferences = preferences;
    }

}
