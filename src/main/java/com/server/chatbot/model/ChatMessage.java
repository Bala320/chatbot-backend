package com.server.chatbot.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class ChatMessage implements Serializable {

    private String role;
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
