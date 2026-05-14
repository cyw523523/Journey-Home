package com.guitu.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guitu.security.WebSocketAuthHandshakeInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionOwners = new ConcurrentHashMap<>();

    public MessageSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object attribute = session.getAttributes().get(WebSocketAuthHandshakeInterceptor.USER_ID_ATTRIBUTE);
        if (!(attribute instanceof Long userId)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        sessionOwners.put(session.getId(), userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void notifyMessageCreated(Long conversationId, Long senderId, Long receiverId) {
        Map<String, Object> payload = Map.of(
                "type", "message-created",
                "conversationId", conversationId,
                "actorId", senderId
        );
        sendToUser(senderId, payload);
        sendToUser(receiverId, payload);
    }

    public void notifyConversationRead(Long conversationId, Long readerId, Long ownerId) {
        Map<String, Object> payload = Map.of(
                "type", "conversation-read",
                "conversationId", conversationId,
                "actorId", readerId
        );
        sendToUser(ownerId, payload);
    }

    private void sendToUser(Long userId, Map<String, Object> payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ignored) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions.toArray(WebSocketSession[]::new)) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException ignored) {
                unregister(session);
            }
        }
    }

    private void unregister(WebSocketSession session) {
        Long userId = sessionOwners.remove(session.getId());
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }
}
