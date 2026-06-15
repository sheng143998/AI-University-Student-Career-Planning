package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PythonMarketAiClient {

    private static final String MARKET_INSIGHT_PATH = "/api/v1/market/insight";
    private static final String MARKET_SOFT_SKILLS_PATH = "/api/v1/market/soft-skills";
    private static final String JOB_CLASSIFY_PATH = "/internal/market/jobs/classify";
    private static final String JOB_INDEX_PATH = "/internal/market/jobs/index";
    private static final String JOB_SEARCH_PATH = "/internal/market/jobs/search";

    private final PythonAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public JsonNode generateMarketInsight(Map<String, Object> payload) {
        return postJson(MARKET_INSIGHT_PATH, payload);
    }

    public JsonNode generateSoftSkills(Map<String, Object> payload) {
        return postJson(MARKET_SOFT_SKILLS_PATH, payload);
    }

    public JsonNode classifyJob(Map<String, Object> payload) {
        JsonNode data = resultData(postJson(JOB_CLASSIFY_PATH, payload));
        if (!data.isObject()) {
            throw new PythonMarketInvalidResponseException("job classification data must be an object");
        }
        return data;
    }

    public JobIndexResult indexJobs(Map<String, Object> payload) {
        JsonNode data = resultData(postJson(JOB_INDEX_PATH, payload));
        JsonNode recordsNode = data.path("records");
        if (!recordsNode.isArray()) {
            throw new PythonMarketInvalidResponseException("job index records must be an array");
        }

        List<JobIndexRecord> records = new ArrayList<>();
        for (JsonNode item : recordsNode) {
            if (!item.isObject()) {
                continue;
            }
            String id = item.path("id").asText(null);
            Long jobId = item.path("job_id").isNumber() ? item.path("job_id").asLong() : null;
            String embedding = item.path("embedding").asText(null);
            String contentHash = item.path("content_hash").asText(null);
            JsonNode metadataNode = item.path("metadata");
            if (id == null || id.isBlank() || jobId == null || embedding == null || embedding.isBlank()) {
                continue;
            }
            records.add(new JobIndexRecord(
                    id,
                    jobId,
                    embedding,
                    metadataNode.isObject() ? writeJson(metadataNode) : "{}",
                    contentHash
            ));
        }
        JsonNode diagnostics = data.path("diagnostics");
        return new JobIndexResult(records, diagnostics.isObject() ? diagnostics : objectMapper.createObjectNode());
    }

    public JobSearchResult searchJobs(Map<String, Object> payload) {
        JsonNode data = resultData(postJson(JOB_SEARCH_PATH, payload));
        JsonNode jobIdsNode = data.path("job_ids");
        if (!jobIdsNode.isArray()) {
            throw new PythonMarketInvalidResponseException("job search job_ids must be an array");
        }

        List<Long> jobIds = new ArrayList<>();
        for (JsonNode item : jobIdsNode) {
            if (item.isNumber()) {
                jobIds.add(item.asLong());
            }
        }
        JsonNode retrieval = data.path("retrieval");
        return new JobSearchResult(jobIds, data.path("scores"), retrieval.isObject() ? retrieval : objectMapper.createObjectNode());
    }

    private JsonNode postJson(String path, Map<String, Object> payload) {
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.getBaseUrl()) + path))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getMarketTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 422) {
                throw new PythonMarketValidationException(response.body());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PythonMarketUnavailableException("Python Market AI service returned HTTP " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (PythonMarketException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new PythonMarketTimeoutException(e);
        } catch (IOException e) {
            if (e instanceof ConnectException) {
                throw new PythonMarketUnavailableException(e);
            }
            throw new PythonMarketInvalidResponseException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PythonMarketUnavailableException(e);
        }
    }

    private JsonNode resultData(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new PythonMarketInvalidResponseException("response must be a JSON object");
        }
        int code = root.path("code").asInt(0);
        if (code != 1) {
            throw new PythonMarketValidationException(root.path("msg").asText("VALIDATION_ERROR"));
        }
        return root.path("data");
    }

    private String writeJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException e) {
            throw new PythonMarketInvalidResponseException(e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "http://127.0.0.1:8090" : normalized;
    }

    public record JobIndexRecord(String id, Long jobId, String embedding, String metadata, String contentHash) {
    }

    public record JobIndexResult(List<JobIndexRecord> records, JsonNode diagnostics) {
    }

    public record JobSearchResult(List<Long> jobIds, JsonNode scores, JsonNode retrieval) {
    }

    public static class PythonMarketException extends RuntimeException {
        PythonMarketException(String message) {
            super(message);
        }

        PythonMarketException(Throwable cause) {
            super(cause);
        }
    }

    @Getter
    public static class PythonMarketValidationException extends PythonMarketException {
        private final String responseBody;

        PythonMarketValidationException(String responseBody) {
            super("Python Market AI validation failed");
            this.responseBody = responseBody;
        }
    }

    public static class PythonMarketTimeoutException extends PythonMarketException {
        PythonMarketTimeoutException(Throwable cause) {
            super(cause);
        }
    }

    public static class PythonMarketUnavailableException extends PythonMarketException {
        PythonMarketUnavailableException(String message) {
            super(message);
        }

        PythonMarketUnavailableException(Throwable cause) {
            super(cause);
        }
    }

    public static class PythonMarketInvalidResponseException extends PythonMarketException {
        PythonMarketInvalidResponseException(String message) {
            super(message);
        }

        PythonMarketInvalidResponseException(Throwable cause) {
            super(cause);
        }
    }
}
