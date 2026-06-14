package com.itsheng.service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.service.config.PythonAiProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PythonDashboardAiClient {

    private static final String TARGET_JOB_MATCH_PATH = "/internal/dashboard/target-job/match";

    private final ObjectMapper objectMapper;
    private final PythonAiProperties pythonAiProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TargetJobMatchResult matchTargetJob(Map<String, Object> payload) {
        try {
            String endpoint = normalizeBaseUrl(pythonAiProperties.getBaseUrl()) + TARGET_JOB_MATCH_PATH;
            String payloadJson = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(pythonAiProperties.getDashboardTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 || response.statusCode() == 422) {
                throw new DashboardAiException("Dashboard-AI 请求参数错误");
            }
            if (response.statusCode() == 204) {
                throw new DashboardAiException("暂无可用岗位匹配结果");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String responseBody = response.body();
                log.warn("Python Dashboard-AI 调用失败，status={}, responseLength={}",
                        response.statusCode(), responseBody == null ? 0 : responseBody.length());
                throw new DashboardAiException("Dashboard-AI 服务暂不可用");
            }
            return parseTargetJobMatch(response.body());
        } catch (DashboardAiException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new DashboardAiException("Dashboard-AI 服务超时，请稍后重试", e);
        } catch (IOException e) {
            if (e instanceof ConnectException) {
                throw new DashboardAiException("Dashboard-AI 服务暂不可用", e);
            }
            throw new DashboardAiException("Dashboard-AI 服务暂不可用", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DashboardAiException("Dashboard-AI 服务暂不可用", e);
        } catch (Exception e) {
            throw new DashboardAiException("Dashboard-AI 服务暂不可用", e);
        }
    }

    private TargetJobMatchResult parseTargetJobMatch(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        int code = root.path("code").asInt(0);
        String msg = root.path("msg").asText("");
        if (code == 0) {
            if ("NO_MATCH".equalsIgnoreCase(msg)) {
                throw new DashboardAiException("暂无可用岗位匹配结果");
            }
            throw new DashboardAiException("Dashboard-AI 请求参数错误");
        }

        JsonNode matchedJob = root.path("data").path("matched_job");
        Long jobId = matchedJob.path("job_id").isNumber() ? matchedJob.path("job_id").asLong() : null;
        String jobName = matchedJob.path("job_name").asText(null);
        double score = matchedJob.path("score").asDouble(0.0);
        if (jobId == null) {
            throw new DashboardAiException("暂无可用岗位匹配结果");
        }

        JsonNode diagnostics = root.path("data").path("retrieval");
        log.debug("Python Dashboard-AI diagnostics: fusionMethod={}, candidateCount={}, evidenceCount={}",
                diagnostics.path("fusion_method").asText(""),
                diagnostics.path("candidate_count").asInt(0),
                diagnostics.path("selected_evidence_ids").size());
        return new TargetJobMatchResult(jobId, jobName, score, diagnostics);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record TargetJobMatchResult(Long jobId, String jobName, double score, JsonNode diagnostics) {
    }

    @Getter
    public static class DashboardAiException extends RuntimeException {
        private final String userMessage;

        public DashboardAiException(String userMessage) {
            super(userMessage);
            this.userMessage = userMessage;
        }

        public DashboardAiException(String userMessage, Throwable cause) {
            super(userMessage, cause);
            this.userMessage = userMessage;
        }
    }
}
