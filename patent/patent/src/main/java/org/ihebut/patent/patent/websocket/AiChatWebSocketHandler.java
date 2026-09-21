package org.ihebut.patent.patent.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihebut.patent.patent.dto.ChatAskRequest;
import org.ihebut.patent.patent.dto.ChatAskResponse;
import org.ihebut.patent.patent.security.AppAuthPrincipal;
import org.ihebut.patent.patent.security.JwtService;
import org.ihebut.patent.patent.service.ChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiChatWebSocketHandler extends TextWebSocketHandler {
    private final ChatService chatService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AiChatWebSocketHandler(ChatService chatService, JwtService jwtService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            return;
        }
        session.getAttributes().put("userId", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            sendEvent(session, "answer.error", Map.of("message", "未登录，无法建立 AI 连接"));
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            if (!"chat.ask".equals(root.path("type").asText(""))) {
                sendEvent(session, "answer.error", Map.of("message", "暂不支持该消息类型"));
                return;
            }

            ChatAskRequest request = objectMapper.treeToValue(root, ChatAskRequest.class);
            ChatAskResponse response = chatService.streamAsk(userId, request, new ChatService.StreamCallbacks() {
                @Override
                public void onSessionCreated(long sessionId) {
                    safeSend(session, "session.created", Map.of("sessionId", sessionId));
                }

                @Override
                public void onDelta(String delta) {
                    safeSend(session, "answer.delta", Map.of("delta", delta));
                }
            });

            sendEvent(session, "answer.done", Map.of(
                    "sessionId", response.getSessionId(),
                    "answer", response.getAnswer() == null ? "" : response.getAnswer(),
                    "model", response.getModel(),
                    "requestId", response.getRequestId()
            ));
        } catch (ResponseStatusException ex) {
            sendEvent(session, "answer.error", Map.of("message", ex.getReason() == null ? "AI 问答失败，请稍后重试。" : ex.getReason()));
        } catch (Exception ex) {
            sendEvent(session, "answer.error", Map.of("message", ex.getMessage() == null ? "AI 问答失败，请稍后重试。" : ex.getMessage()));
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        Object cached = session.getAttributes().get("userId");
        if (cached instanceof Long userId) {
            return userId;
        }

        String token = parseQuery(session, "token");
        if (token == null || token.isBlank()) {
            return null;
        }

        AppAuthPrincipal principal = jwtService.parseAuthPrincipal(token);
        if (principal == null || principal.userId() == null || principal.isAdmin()) {
            return null;
        }
        session.getAttributes().put("userId", principal.userId());
        return principal.userId();
    }

    private String parseQuery(WebSocketSession session, String key) {
        if (session.getUri() == null || session.getUri().getRawQuery() == null) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : session.getUri().getRawQuery().split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int index = pair.indexOf('=');
            String rawKey = index >= 0 ? pair.substring(0, index) : pair;
            String rawValue = index >= 0 ? pair.substring(index + 1) : "";
            values.put(
                    URLDecoder.decode(rawKey, StandardCharsets.UTF_8),
                    URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
            );
        }
        return values.get(key);
    }

    private void sendEvent(WebSocketSession session, String type, Map<String, ?> payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("payload", payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(body)));
    }

    private void safeSend(WebSocketSession session, String type, Map<String, ?> payload) {
        try {
            sendEvent(session, type, payload);
        } catch (IOException ignored) {
        }
    }
}
