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
public class PythonResumeAiClient {

    private static final String RESUME_ANALYZE_PATH = "/api/v1/resume/analyze";
    private static final String RESUME_OCR_PATH = "/internal/resume/ocr";

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode analyze(Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getResumeBaseUrl()) + RESUME_ANALYZE_PATH))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getResumeTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonResumeHttpException(response.statusCode(), safeBodySummary(response.body()));
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new PythonResumeSchemaException("empty response body");
            }
            JsonNode body = objectMapper.readTree(response.body());
            validateBody(body);
            return body;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonResumeTimeoutException(e);
        } catch (PythonResumeClientException e) {
            throw e;
        } catch (IOException e) {
            throw new PythonResumeUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonResumeUnavailableException(e);
        }
    }

    public JsonNode ocrPage(Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getResumeBaseUrl()) + RESUME_OCR_PATH))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getResumeOcrTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonResumeHttpException(response.statusCode(), safeBodySummary(response.body()));
            }
            if (response.body() == null || response.body().isBlank()) {
                throw new PythonResumeSchemaException("empty OCR response body");
            }
            JsonNode body = objectMapper.readTree(response.body());
            JsonNode data = validateOcrBody(body);
            return data;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonResumeTimeoutException(e);
        } catch (PythonResumeClientException e) {
            throw e;
        } catch (IOException e) {
            throw new PythonResumeUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonResumeUnavailableException(e);
        }
    }

    private void validateBody(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new PythonResumeSchemaException("response body must be an object");
        }
        JsonNode status = body.get("status");
        if (status == null || !"completed".equals(status.asText())) {
            throw new PythonResumeSchemaException("response status must be completed");
        }
        requireObject(body, "parsed_data");
        requireObject(body, "scores");
        requireArray(body, "highlights");
        requireArray(body, "suggestions");
        requireObject(body, "capability_profile");
        requireObject(body, "rag_diagnostics");
    }

    private JsonNode validateOcrBody(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new PythonResumeSchemaException("OCR response body must be an object");
        }
        int code = body.path("code").asInt(0);
        if (code != 1) {
            throw new PythonResumeSchemaException("OCR response code must be 1");
        }
        JsonNode data = body.path("data");
        if (!data.isObject() || !data.path("text").isTextual()) {
            throw new PythonResumeSchemaException("OCR response data.text must be text");
        }
        return data;
    }

    private void requireObject(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isObject()) {
            throw new PythonResumeSchemaException("response field must be object: " + field);
        }
    }

    private void requireArray(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isArray()) {
            throw new PythonResumeSchemaException("response field must be array: " + field);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "http://127.0.0.1:8091" : normalized;
    }

    private String safeBodySummary(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return "length=" + body.length();
    }

    public static class PythonResumeClientException extends RuntimeException {
        PythonResumeClientException(String message) {
            super(message);
        }

        PythonResumeClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Getter
    public static class PythonResumeHttpException extends PythonResumeClientException {
        private final int statusCode;
        private final String responseBody;

        PythonResumeHttpException(int statusCode, String responseBody) {
            super("Python Resume service returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    public static class PythonResumeTimeoutException extends PythonResumeClientException {
        PythonResumeTimeoutException(Throwable cause) {
            super("Python Resume service timeout", cause);
        }
    }

    public static class PythonResumeUnavailableException extends PythonResumeClientException {
        PythonResumeUnavailableException(Throwable cause) {
            super("Python Resume service unavailable", cause);
        }
    }

    public static class PythonResumeSchemaException extends PythonResumeClientException {
        PythonResumeSchemaException(String message) {
            super("Python Resume service schema error: " + message);
        }
    }
}
