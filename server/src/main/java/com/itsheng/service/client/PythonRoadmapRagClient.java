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
public class PythonRoadmapRagClient {

    private static final String PERSONALIZED_RECOMMENDATIONS_PATH = "/api/roadmap/recommendations/personalized";

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public JsonNode generatePersonalizedRecommendations(Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + PERSONALIZED_RECOMMENDATIONS_PATH))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getRoadmapTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonRoadmapRagException(response.statusCode(), response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonRoadmapRagTimeoutException(e);
        } catch (PythonRoadmapRagException e) {
            throw e;
        } catch (IOException e) {
            throw new PythonRoadmapRagUnavailableException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonRoadmapRagUnavailableException(e);
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
    public static class PythonRoadmapRagException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        PythonRoadmapRagException(int statusCode, String responseBody) {
            super("Python Roadmap-RAG service returned HTTP " + statusCode);
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }
    }

    public static class PythonRoadmapRagTimeoutException extends RuntimeException {
        PythonRoadmapRagTimeoutException(Throwable cause) {
            super("Python Roadmap-RAG service timeout", cause);
        }
    }

    public static class PythonRoadmapRagUnavailableException extends RuntimeException {
        PythonRoadmapRagUnavailableException(Throwable cause) {
            super("Python Roadmap-RAG service unavailable", cause);
        }
    }
}
