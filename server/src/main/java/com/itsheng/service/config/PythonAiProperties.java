package com.itsheng.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python AI 服务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "fuchuang.ai.python")
public class PythonAiProperties {

    /**
     * Python AI 服务基础地址
     */
    private String baseUrl = "http://127.0.0.1:8090";

    /**
     * Chat-only Python RAG service base URL.
     */
    private String chatBaseUrl = "http://127.0.0.1:8092";

    /**
     * Chat 生成超时时间（秒）
     */
    private Integer chatTimeoutSeconds;

    /**
     * 每日建议超时时间（秒）
     */
    private Integer dailySuggestionsTimeoutSeconds;

    /**
     * 通用 Python AI 超时时间（秒），兼容 fuchuang.ai.python.timeout-seconds。
     */
    private Integer timeoutSeconds;

    /**
     * AI/RAG 反馈与偏好校验超时时间（秒）
     */
    private Integer ragFeedbackTimeoutSeconds;

    private String reportsBaseUrl;

    private Integer reportsTimeoutSeconds;

    /**
     * Resume-AI Python 服务地址，兼容 fuchuang.ai.python.resume-base-url。
     */
    private String resumeBaseUrl;

    /**
     * Resume-AI Python 服务超时时间（秒）。
     */
    private Integer resumeTimeoutSeconds;

    public Integer getChatTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, chatTimeoutSeconds, timeoutSeconds, 60);
    }

    public String getChatBaseUrl() {
        String envValue = System.getenv("FUCHUANG_AI_PYTHON_CHAT_BASE_URL");
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        if (chatBaseUrl != null && !chatBaseUrl.isBlank()) {
            return chatBaseUrl.trim();
        }
        return "http://127.0.0.1:8092";
    }

    public Integer getDailySuggestionsTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_DAILY_SUGGESTIONS_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, dailySuggestionsTimeoutSeconds, timeoutSeconds, 30);
    }

    public Integer getRagFeedbackTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, ragFeedbackTimeoutSeconds, timeoutSeconds, 10);
    }

    public String getReportsBaseUrl() {
        String envValue = System.getenv("FUCHUANG_AI_PYTHON_REPORTS_BASE_URL");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return reportsBaseUrl != null && !reportsBaseUrl.isBlank() ? reportsBaseUrl : baseUrl;
    }

    public Integer getReportsTimeoutSeconds() {
        Integer envValue = readPositiveEnv("FUCHUANG_AI_PYTHON_REPORTS_TIMEOUT_SECONDS");
        return firstPositive(envValue, reportsTimeoutSeconds, timeoutSeconds, 8);
    }

    public String getResumeBaseUrl() {
        String envValue = readStringEnv("FUCHUANG_AI_PYTHON_RESUME_BASE_URL");
        String genericEnvValue = readStringEnv("FUCHUANG_AI_PYTHON_BASE_URL");
        String configuredBaseUrl = "http://127.0.0.1:8090".equals(baseUrl) ? null : baseUrl;
        return firstText(envValue, resumeBaseUrl, genericEnvValue, configuredBaseUrl, "http://127.0.0.1:8091");
    }

    public Integer getResumeTimeoutSeconds() {
        Integer envValue = readPositiveEnv("FUCHUANG_AI_PYTHON_RESUME_TIMEOUT_SECONDS");
        return firstPositive(envValue, resumeTimeoutSeconds, timeoutSeconds, 30);
    }

    private Integer firstPositive(Integer... values) {
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return 30;
    }

    private Integer readPositiveEnv(String name) {
        String raw = System.getenv(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "http://127.0.0.1:8090";
    }

    private String readStringEnv(String name) {
        String raw = System.getenv(name);
        return raw == null || raw.isBlank() ? null : raw.trim();
    }
}
