package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PythonReportsAiClient {

    private static final String GENERATE_SUPPORT_PATH = "/api/v1/reports/generate-support";

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ReportsSupportResult generateSupport(Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getReportsBaseUrl()) + GENERATE_SUPPORT_PATH))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getReportsTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonReportsClientException(response.statusCode(), response.body());
            }
            return parseSupportResponse(response.body());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonReportsTimeoutException(e);
        } catch (PythonReportsInvalidResponseException e) {
            throw e;
        } catch (PythonReportsClientException e) {
            throw e;
        } catch (IOException e) {
            throw new PythonReportsUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonReportsUnavailableException(e);
        }
    }

    private ReportsSupportResult parseSupportResponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json == null || !json.isObject()) {
                throw new IllegalArgumentException("response must be a JSON object");
            }

            String status = json.path("status").asText("");
            if (!"OK".equals(status) && !"EMPTY_RETRIEVAL".equals(status)) {
                throw new IllegalArgumentException("unsupported reports response status");
            }
            JsonNode evidenceRefs = json.get("evidenceRefs");
            if (evidenceRefs == null || !evidenceRefs.isArray()) {
                throw new IllegalArgumentException("evidenceRefs must be an array");
            }
            JsonNode ragDiagnostics = json.get("ragDiagnostics");
            if (ragDiagnostics == null || !ragDiagnostics.isObject()) {
                throw new IllegalArgumentException("ragDiagnostics must be an object");
            }
            JsonNode aiSuggestions = json.get("aiSuggestions");
            if (aiSuggestions != null && !aiSuggestions.isTextual() && !aiSuggestions.isNull()) {
                throw new IllegalArgumentException("aiSuggestions must be a string");
            }
            return new ReportsSupportResult(
                    status,
                    aiSuggestions == null || aiSuggestions.isNull() ? "" : aiSuggestions.asText(),
                    evidenceRefs,
                    ragDiagnostics
            );
        } catch (IOException | IllegalArgumentException e) {
            throw new PythonReportsInvalidResponseException(e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "http://127.0.0.1:8090" : normalized;
    }

    public record ReportsSupportResult(
            String status,
            String aiSuggestions,
            JsonNode evidenceRefs,
            JsonNode ragDiagnostics
    ) {
    }

    @Getter
    public static class PythonReportsClientException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        PythonReportsClientException(int statusCode, String responseBody) {
            super("Python Reports AI service returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    public static class PythonReportsTimeoutException extends RuntimeException {
        PythonReportsTimeoutException(Throwable cause) {
            super("Python Reports AI service timeout", cause);
        }
    }

    public static class PythonReportsUnavailableException extends RuntimeException {
        PythonReportsUnavailableException(Throwable cause) {
            super("Python Reports AI service unavailable", cause);
        }
    }

    public static class PythonReportsInvalidResponseException extends RuntimeException {
        PythonReportsInvalidResponseException(Throwable cause) {
            super("Python Reports AI service returned an invalid response", cause);
        }
    }
}
