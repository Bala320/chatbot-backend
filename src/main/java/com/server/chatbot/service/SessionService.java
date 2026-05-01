package com.server.chatbot.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.server.chatbot.model.ChatMessage;
import com.server.chatbot.model.Conversation;
import com.server.chatbot.model.RefreshToken;
import com.server.chatbot.repository.ConversationRepository;
import com.server.chatbot.repository.RefreshTokenRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class SessionService {

    private static final String ACCESS_COOKIE_NAME = "op_access";
    private static final String REFRESH_COOKIE_NAME = "op_refresh";
    private static final String SYSTEM_PROMPT = """
        You are a helpful AI assistant.

        Rules:
        - Only answer based on partner data
        - Be concise and clear
        - If you don't know, say "I don't have that information"
        - Do not hallucinate
        """;

    private final TokenService tokenService;
    private final ConversationRepository conversationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConversationService conversationService;

    @Value("${app.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${app.cookies.same-site:None}")
    private String sameSite;

    public SessionService(
            TokenService tokenService,
            ConversationRepository conversationRepository,
            RefreshTokenRepository refreshTokenRepository,
            ConversationService conversationService) {
        this.tokenService = tokenService;
        this.conversationRepository = conversationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.conversationService = conversationService;
    }

    public String createSession(HttpServletResponse response) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = tokenService.generateAccessToken(sessionId);
        String refreshToken = tokenService.generateRefreshToken();

        storeRefreshToken(refreshToken, sessionId);
        initializeConversation(sessionId);

        addCookie(response, ACCESS_COOKIE_NAME, accessToken, TokenService.ACCESS_EXPIRY_SECONDS);
        addCookie(response, REFRESH_COOKIE_NAME, refreshToken, TokenService.REFRESH_EXPIRY_SECONDS);

        return sessionId;
    }

    public void refreshSession(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token cookie is missing");
        }

        RefreshToken stored = refreshTokenRepository
                .findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Refresh token is invalid"));

        if (stored.getExpiresAt().isBefore(tokenService.now())) {
            refreshTokenRepository.deleteById(refreshToken);
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token has expired");
        }

        String sessionId = stored.getSid();
        refreshTokenRepository.deleteById(refreshToken);

        String newAccess = tokenService.generateAccessToken(sessionId);
        String newRefresh = tokenService.generateRefreshToken();

        storeRefreshToken(newRefresh, sessionId);
        addCookie(response, ACCESS_COOKIE_NAME, newAccess, TokenService.ACCESS_EXPIRY_SECONDS);
        addCookie(response, REFRESH_COOKIE_NAME, newRefresh, TokenService.REFRESH_EXPIRY_SECONDS);
    }

    public void storeRefreshToken(String token, String sessionId) {
        RefreshToken refreshToken = new RefreshToken(
                token,
                sessionId,
                tokenService.refreshExpiryTime()
        );

        refreshTokenRepository.save(refreshToken);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(resolveSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String resolveSameSite() {
        if (sameSite == null || sameSite.isBlank()) {
            return secureCookies ? "None" : "Lax";
        }
        return sameSite;
    }

    private void initializeConversation(String sessionId) {
        List<ChatMessage> history = new ArrayList<>();
        // history.add(new ChatMessage(
        //         "system",
        //         "You are a helpful chatbot restricted to partner data."
        // ));
        // history.add(new ChatMessage(
        //         "system",
        //         "Default partner data (no partners configured)"
        // ));
        history.add(new ChatMessage("system", SYSTEM_PROMPT));
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setPageKey("default-page");
        conversation.setHistory(history);
        conversation.setExpiresAt(tokenService.refreshExpiryTime());

        conversationRepository.save(conversation);
        conversationService.cacheConversation(conversation);
    }
}
