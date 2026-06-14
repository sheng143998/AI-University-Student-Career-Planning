package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.vo.AiAdviceVO;
import com.itsheng.service.config.PythonAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PythonGoalsAdviceClient {

    static final String GOALS_ADVICE_PATH = "/internal/goals/advice";

    private final ObjectMapper objectMapper;
    private final PythonAiProperties pythonAiProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiAdviceVO generateGoalAdvice(Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(pythonAiProperties.getBaseUrl()) + GOALS_ADVICE_PATH))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.statusCode(), response.body());
        } catch (BaseException e) {
            throw e;
        } catch (HttpTimeoutException e) {
            throw new BaseException("目标AI建议服务超时，请稍后重试", e);
        } catch (ConnectException e) {
            throw new BaseException("目标AI建议服务暂不可用", e);
        } catch (JsonProcessingException e) {
            log.warn("Python Goals-RAG 响应不是 JSON: {}", e.getMessage());
            throw new BaseException("目标AI建议服务暂不可用", e);
        } catch (Exception e) {
            log.warn("Python Goals-RAG 调用失败: {}", e.getMessage(), e);
            throw new BaseException("目标AI建议生成失败，请稍后重试", e);
        }
    }

    private AiAdviceVO parseResponse(int statusCode, String body) throws Exception {
        if (statusCode == 400 || statusCode == 422) {
            throw new BaseException("目标AI建议生成失败，请检查目标上下文");
        }
        if (statusCode == 504) {
            throw new BaseException("目标AI建议服务超时，请稍后重试");
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new BaseException("目标AI建议服务暂不可用");
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.path("content");
        if (!contentNode.isTextual() || contentNode.asText().trim().isEmpty()) {
            throw new BaseException("目标AI建议生成结果为空");
        }
        AiAdviceVO advice = new AiAdviceVO(contentNode.asText().trim());
        JsonNode evidenceNode = root.path("evidenceReferences");
        if (evidenceNode.isArray()) {
            advice.setEvidenceReferences(objectMapper.convertValue(evidenceNode, List.class));
        }
        JsonNode diagnosticsNode = root.path("retrievalDiagnostics");
        if (diagnosticsNode.isObject()) {
            advice.setRetrievalDiagnostics(objectMapper.convertValue(diagnosticsNode, Map.class));
        }
        return advice;
    }

    private long timeoutSeconds() {
        Integer timeout = pythonAiProperties.getTimeoutSeconds();
        return timeout != null && timeout > 0 ? timeout : 20;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8090" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
