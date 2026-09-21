package org.ihebut.patent.patent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ihebut.patent.patent.dto.AiChatRequest;
import org.ihebut.patent.patent.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class DashScopeChatService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String appId;
    private final String patentAppId;
    private final String baseUrl;
    private final String defaultModel;
    private final int timeoutMs;

    public DashScopeChatService(
            ObjectMapper objectMapper,
            @Value("${ai.dashscope.api-key:}") String apiKey,
            @Value("${ai.dashscope.app-id:}") String appId,
            @Value("${ai.dashscope.app-id-patent:}") String patentAppId,
            @Value("${ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") String baseUrl,
            @Value("${ai.dashscope.model:qwen-plus}") String defaultModel,
            @Value("${ai.dashscope.timeout-ms:30000}") int timeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.appId = appId == null ? "" : appId.trim();
        this.patentAppId = patentAppId == null ? "" : patentAppId.trim();
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.defaultModel = defaultModel == null ? "qwen-plus" : defaultModel.trim();
        this.timeoutMs = timeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        return chatWithMessages(
                request == null ? null : request.getModel(),
                request == null ? null : request.getTemperature(),
                request == null ? null : request.getMaxTokens(),
                request == null ? List.of() : List.of(new Message("user", request.getQuestion()))
        );
    }

    public AiChatResponse chatWithMessages(String modelOverride, Double temperature, Integer maxTokens, List<Message> inputMessages) {
        return executeChat(modelOverride, temperature, maxTokens, inputMessages, false, delta -> {});
    }

    public AiChatResponse streamChatWithMessages(String modelOverride, Double temperature, Integer maxTokens, List<Message> inputMessages, StreamListener listener) {
        return executeChat(modelOverride, temperature, maxTokens, inputMessages, true, listener);
    }

    private AiChatResponse executeChat(String modelOverride, Double temperature, Integer maxTokens, List<Message> inputMessages, boolean stream, StreamListener listener) {
        ensureConfig();
        if (inputMessages == null || inputMessages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        String model = (modelOverride == null || modelOverride.isBlank()) ? defaultModel : modelOverride.trim();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("stream", stream);
        ArrayNode messagesNode = payload.putArray("messages");
        for (Message message : inputMessages) {
            if (message == null || message.role() == null || message.role().isBlank() || message.content() == null || message.content().isBlank()) {
                continue;
            }
            ObjectNode item = objectMapper.createObjectNode();
            item.put("role", message.role().trim());
            item.put("content", message.content().trim());
            messagesNode.add(item);
        }
        if (temperature != null) {
            payload.put("temperature", temperature);
        }
        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();

        try {
            if (!stream) {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return parseBlockingResponse(model, response);
            }
            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            return parseStreamResponse(model, response, listener == null ? delta -> {} : listener);
        } catch (Exception e) {
            throw new IllegalStateException("调用通义千问失败: " + e.getMessage());
        }
    }

    private AiChatResponse parseBlockingResponse(String model, HttpResponse<String> response) throws Exception {
        ensureSuccess(response.statusCode(), response.body());
        JsonNode root = objectMapper.readTree(response.body());
        String requestId = root.has("request_id") ? root.path("request_id").asText(null) : root.path("id").asText(null);
        String answer = extractContent(root);
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("通义千问返回内容为空");
        }
        return new AiChatResponse(answer, model, requestId);
    }

    private AiChatResponse parseStreamResponse(String model, HttpResponse<java.io.InputStream> response, StreamListener listener) throws Exception {
        ensureSuccess(response.statusCode(), null);
        StringBuilder answer = new StringBuilder();
        String requestId = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if ("[DONE]".equals(payload)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(payload);
                if (requestId == null) {
                    requestId = root.has("request_id") ? root.path("request_id").asText(null) : root.path("id").asText(null);
                }
                String delta = extractDelta(root);
                if (delta != null && !delta.isEmpty()) {
                    answer.append(delta);
                    listener.onDelta(delta);
                }
            }
        }
        return new AiChatResponse(answer.toString(), model, requestId);
    }

    public AiChatResponse chatWithApp(String prompt, String sessionId, boolean usePatentAppId) {
        String targetAppId = usePatentAppId ? patentAppId : appId;
        if (targetAppId == null || targetAppId.isBlank()) {
            String key = usePatentAppId ? "ai.dashscope.app-id-patent" : "ai.dashscope.app-id";
            throw new IllegalStateException("未配置通义千问 App ID，请检查 " + key);
        }
        ensureConfig();

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode input = payload.putObject("input");
        input.put("prompt", prompt);
        if (sessionId != null && !sessionId.isBlank()) {
            input.put("session_id", sessionId);
        }

        String appUrl = "https://dashscope.aliyuncs.com/api/v1/apps/" + targetAppId + "/completion";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(appUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response.statusCode(), response.body());
            JsonNode root = objectMapper.readTree(response.body());
            String requestId = root.path("request_id").asText(null);
            String answer = root.path("output").path("text").asText(null);
            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("通义千问 App API 返回内容为空");
            }
            return new AiChatResponse(answer, "app-" + targetAppId, requestId);
        } catch (Exception e) {
            throw new IllegalStateException("调用通义千问 App API 失败: " + e.getMessage());
        }
    }

    public AiChatResponse chatWithApp(String prompt, String sessionId) {
        return chatWithApp(prompt, sessionId, false);
    }

    private void ensureConfig() {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("未配置通义千问 API Key");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("未配置通义千问请求地址");
        }
    }

    private String writeJson(ObjectNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("请求序列化失败");
        }
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        String snippet = body == null ? "" : body;
        if (snippet.length() > 500) {
            snippet = snippet.substring(0, 500);
        }
        throw new IllegalStateException("通义千问返回异常: HTTP " + statusCode + ", body=" + snippet);
    }

    private String extractContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode message = choices.get(0).path("message");
        return message.has("content") ? message.path("content").asText() : null;
    }

    private String extractDelta(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode delta = choices.get(0).path("delta");
        if (delta.has("content")) {
            return delta.path("content").asText("");
        }
        JsonNode message = choices.get(0).path("message");
        if (message.has("content")) {
            return message.path("content").asText("");
        }
        return null;
    }

    public record Message(String role, String content) {}

    @FunctionalInterface
    public interface StreamListener {
        void onDelta(String delta);
    }
}
