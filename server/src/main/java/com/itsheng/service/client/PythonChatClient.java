package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.ChatMessage;
import com.itsheng.pojo.vo.ChatDailySuggestionsVO;
import com.itsheng.service.config.PythonAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python Chat/RAG 服务客户端
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PythonChatClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    /**
     * 调用 Python RAG 服务生成 Chat 回复
     */
    public ChatCompletionResult complete(Long userId, Long conversationId, String content, Long resumeId,
                                         List<ChatMessage> history, String parsedDataJson) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("conversationId", conversationId);
        payload.put("content", content);
        payload.put("resumeId", resumeId);
        payload.put("history", buildHistory(history));
        payload.put("parsedData", parseJsonObject(parsedDataJson));
        payload.put("retrievalOptions", buildRetrievalOptions(userId, resumeId));

        try {
            JsonNode root = postJson("/api/v1/chat/complete", payload, properties.getChatTimeoutSeconds());
            String responseContent = getText(root, "content");
            if (responseContent == null || responseContent.isBlank()) {
                return ChatCompletionResult.builder()
                        .content("AI 暂时没有生成有效回复，请换个问法再试。")
                        .build();
            }

            return ChatCompletionResult.builder()
                    .content(responseContent)
                    .title(getText(root, "title"))
                    .suggestionQuestions(readTextArray(root.get("suggestionQuestions")))
                    .build();
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("Python Chat 服务响应超时: userId={}, conversationId={}, timeout={}s",
                    userId, conversationId, properties.getChatTimeoutSeconds());
            return ChatCompletionResult.builder()
                    .content("AI 服务响应超时，请稍后重试。")
                    .build();
        } catch (PythonAiHttpException e) {
            if (e.isClientError()) {
                log.warn("Python Chat 请求参数无效: userId={}, conversationId={}, status={}, body={}",
                        userId, conversationId, e.getStatusCode(), summarizeBody(e.getResponseBody()));
                return ChatCompletionResult.builder()
                        .content("AI 请求参数无效，请刷新后重试。")
                        .build();
            }
            log.error("Python Chat 服务下游错误: userId={}, conversationId={}, status={}",
                    userId, conversationId, e.getStatusCode());
            return ChatCompletionResult.builder()
                    .content("AI 服务暂不可用，请稍后重试。")
                    .build();
        } catch (Exception e) {
            log.warn("Python Chat 服务不可用: userId={}, conversationId={}, error={}",
                    userId, conversationId, e.getMessage());
            return ChatCompletionResult.builder()
                    .content("AI 服务暂不可用，请稍后重试。")
                    .build();
        }
    }

    /**
     * 调用 Python 服务生成每日建议
     */
    public ChatDailySuggestionsVO dailySuggestions(Long userId, Long resumeId, String parsedDataJson) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("resumeId", resumeId);
        payload.put("parsedData", parseJsonObject(parsedDataJson));

        JsonNode root = postJson("/api/v1/chat/daily-suggestions", payload, properties.getDailySuggestionsTimeoutSeconds());
        List<ChatDailySuggestionsVO.SuggestionItem> suggestions = readSuggestionItems(root.get("suggestions"));
        List<ChatDailySuggestionsVO.QuickQuestion> quickQuestions = readQuickQuestions(root.get("quickQuestions"));
        if (suggestions.isEmpty() || quickQuestions.isEmpty()) {
            throw new IOException("Python Chat daily suggestions response is empty or invalid");
        }
        return ChatDailySuggestionsVO.builder()
                .suggestions(suggestions)
                .quickQuestions(quickQuestions)
                .build();
    }

    private JsonNode postJson(String path, Map<String, Object> payload, Integer timeoutSeconds) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getChatBaseUrl().replaceAll("/$", "") + path))
                .timeout(Duration.ofSeconds(timeoutSeconds == null ? 30 : timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new PythonAiHttpException(response.statusCode(), response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private List<Map<String, Object>> buildHistory(List<ChatMessage> history) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (history == null) {
            return result;
        }
        for (ChatMessage message : history) {
            Map<String, Object> item = new HashMap<>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            item.put("createdAt", message.getCreateTime() != null ? message.getCreateTime().format(DATE_TIME_FORMATTER) : null);
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> buildRetrievalOptions(Long userId, Long resumeId) {
        Map<String, Object> metadataFilter = new HashMap<>();
        metadataFilter.put("userId", userId);
        metadataFilter.put("documentTypes", List.of("resume", "job", "chat_context"));
        metadataFilter.put("resumeId", resumeId);
        metadataFilter.put("visibilityScope", "private");

        Map<String, Object> retrievalOptions = new HashMap<>();
        retrievalOptions.put("multiQuery", true);
        retrievalOptions.put("hybridSearch", true);
        retrievalOptions.put("ragFusion", true);
        retrievalOptions.put("metadataFilter", metadataFilter);
        return retrievalOptions;
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node != null && node.isObject()) {
                return objectMapper.convertValue(node, Map.class);
            }
        } catch (Exception e) {
            log.warn("解析简历 parsedData 失败，将使用空对象: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    private String getText(JsonNode root, String fieldName) {
        JsonNode node = root == null ? null : root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private List<String> readTextArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode item : node) {
            result.add(item.asText());
        }
        return result;
    }

    private List<ChatDailySuggestionsVO.SuggestionItem> readSuggestionItems(JsonNode node) {
        List<ChatDailySuggestionsVO.SuggestionItem> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode item : node) {
            result.add(ChatDailySuggestionsVO.SuggestionItem.builder()
                    .title(getText(item, "title"))
                    .text(getText(item, "text"))
                    .build());
        }
        return result;
    }

    private List<ChatDailySuggestionsVO.QuickQuestion> readQuickQuestions(JsonNode node) {
        List<ChatDailySuggestionsVO.QuickQuestion> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode item : node) {
            result.add(ChatDailySuggestionsVO.QuickQuestion.builder()
                    .title(getText(item, "title"))
                    .text(getText(item, "text"))
                    .build());
        }
        return result;
    }

    private String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String clean = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
    }

    private static class PythonAiHttpException extends IOException {
        private final int statusCode;
        private final String responseBody;

        PythonAiHttpException(int statusCode, String responseBody) {
            super("Python AI service returned status " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getResponseBody() {
            return responseBody;
        }

        boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }
    }

    @Data
    @Builder
    public static class ChatCompletionResult {
        private String content;
        private String title;
        private List<String> suggestionQuestions;
    }
}
