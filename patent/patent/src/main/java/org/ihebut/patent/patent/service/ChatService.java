package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.AiChatResponse;
import org.ihebut.patent.patent.dto.ChatAskRequest;
import org.ihebut.patent.patent.dto.ChatAskResponse;
import org.ihebut.patent.patent.entity.ChatMessage;
import org.ihebut.patent.patent.entity.ChatSession;
import org.ihebut.patent.patent.mapper.ChatMessageMapper;
import org.ihebut.patent.patent.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ChatService {
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DashScopeChatService dashScopeChatService;

    public ChatService(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper, DashScopeChatService dashScopeChatService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.dashScopeChatService = dashScopeChatService;
    }

    public ChatAskResponse ask(long userId, ChatAskRequest request) {
        PreparedChat prepared = prepareConversation(userId, request);
        AiChatResponse aiResponse = dashScopeChatService.chatWithMessages(
                request.getModel(),
                request.getTemperature(),
                request.getMaxTokens(),
                prepared.messages()
        );
        persistAssistantMessage(prepared.session(), userId, aiResponse.getAnswer());
        chatSessionMapper.save(prepared.session());
        return new ChatAskResponse(prepared.session().getId(), aiResponse.getAnswer(), aiResponse.getModel(), aiResponse.getRequestId());
    }

    public ChatAskResponse streamAsk(long userId, ChatAskRequest request, StreamCallbacks callbacks) {
        PreparedChat prepared = prepareConversation(userId, request);
        if (request.getSessionId() == null && callbacks != null) {
            callbacks.onSessionCreated(prepared.session().getId());
        }
        AiChatResponse aiResponse = dashScopeChatService.streamChatWithMessages(
                request.getModel(),
                request.getTemperature(),
                request.getMaxTokens(),
                prepared.messages(),
                callbacks == null ? delta -> { } : callbacks::onDelta
        );
        persistAssistantMessage(prepared.session(), userId, aiResponse.getAnswer());
        chatSessionMapper.save(prepared.session());
        return new ChatAskResponse(prepared.session().getId(), aiResponse.getAnswer(), aiResponse.getModel(), aiResponse.getRequestId());
    }

    public ChatSession renameSession(long userId, long sessionId, String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "title cannot be blank");
        }
        ChatSession session = chatSessionMapper.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));
        session.setTitle(title.trim());
        return chatSessionMapper.save(session);
    }

    public void deleteSession(long userId, long sessionId) {
        ChatSession session = chatSessionMapper.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));
        chatMessageMapper.deleteBySessionId(session.getId());
        chatSessionMapper.delete(session);
    }

    private PreparedChat prepareConversation(long userId, ChatAskRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "question cannot be blank");
        }
        int historyLimit = request.getHistoryLimit() == null ? 20 : request.getHistoryLimit();
        if (historyLimit < 0) {
            historyLimit = 0;
        }
        if (historyLimit > 50) {
            historyLimit = 50;
        }

        ChatSession session;
        if (request.getSessionId() == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(buildTitle(request.getQuestion()));
            session = chatSessionMapper.save(session);
        } else {
            session = chatSessionMapper.findByIdAndUserId(request.getSessionId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found"));
            if (session.getTitle() == null || session.getTitle().isBlank()) {
                session.setTitle(buildTitle(request.getQuestion()));
            }
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(session.getId());
        userMessage.setUserId(userId);
        userMessage.setRole("user");
        userMessage.setContent(request.getQuestion().trim());
        chatMessageMapper.save(userMessage);

        List<ChatMessage> recent = historyLimit == 0
                ? List.of()
                : chatMessageMapper.findTop50BySessionIdOrderByCreatedAtDesc(session.getId());
        List<ChatMessage> history = new ArrayList<>(recent);
        history.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        if (historyLimit > 0 && history.size() > historyLimit) {
            history = history.subList(history.size() - historyLimit, history.size());
        }

        List<DashScopeChatService.Message> messages = new ArrayList<>();
        for (ChatMessage item : history) {
            if (!"user".equals(item.getRole()) && !"assistant".equals(item.getRole()) && !"system".equals(item.getRole())) {
                continue;
            }
            messages.add(new DashScopeChatService.Message(item.getRole(), item.getContent()));
        }
        return new PreparedChat(session, messages);
    }

    private void persistAssistantMessage(ChatSession session, long userId, String answer) {
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setUserId(userId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(answer == null ? "" : answer);
        chatMessageMapper.save(assistantMessage);
    }

    private static String buildTitle(String question) {
        String value = question == null ? "" : question.trim();
        if (value.length() <= 20) {
            return value;
        }
        return value.substring(0, 20);
    }

    private record PreparedChat(ChatSession session, List<DashScopeChatService.Message> messages) {}

    public interface StreamCallbacks {
        default void onSessionCreated(long sessionId) {}

        default void onDelta(String delta) {}
    }
}
