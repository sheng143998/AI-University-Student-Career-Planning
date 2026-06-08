package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PythonRagFeedbackClient {

    private static final String FEEDBACK_PATH = "/internal/rag/feedback";
    private static final String PREFERENCES_VALIDATE_PATH = "/internal/rag/preferences/validate";

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode submitFeedback(Map<String, Object> payload) {
        return postJson(FEEDBACK_PATH, payload);
    }

    public JsonNode validatePreferences(Map<String, Object> payload) {
        return postJson(PREFERENCES_VALIDATE_PATH, payload);
    }

    private JsonNode postJson(String path, Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + path))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getRagFeedbackTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonRagClientException(response.statusCode(), response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonRagTimeoutException(e);
        } catch (PythonRagClientException e) {
            throw e;
        } catch (IOException e) {
            throw new PythonRagUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonRagUnavailableException(e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "http://127.0.0.1:8090" : normalized;
    }

    @Getter
    public static class PythonRagClientException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        PythonRagClientException(int statusCode, String responseBody) {
            super("Python RAG service returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    public static class PythonRagTimeoutException extends RuntimeException {
        PythonRagTimeoutException(Throwable cause) {
            super("Python RAG service timeout", cause);
        }
    }

    public static class PythonRagUnavailableException extends RuntimeException {
        PythonRagUnavailableException(Throwable cause) {
            super("Python RAG service unavailable", cause);
        }
    }
}
