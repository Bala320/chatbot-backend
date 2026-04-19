package com.server.chatbot.repository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.chatbot.model.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findBySessionIdAndPageKey(
        String sessionId,
        String pageKey
    );
}
