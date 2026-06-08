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

    public Integer getChatTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_CHAT_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, chatTimeoutSeconds, timeoutSeconds, 60);
    }

    public Integer getDailySuggestionsTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_DAILY_SUGGESTIONS_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, dailySuggestionsTimeoutSeconds, timeoutSeconds, 30);
    }

    public Integer getRagFeedbackTimeoutSeconds() {
        Integer legacyEnvValue = readPositiveEnv("FUCHUANG_PYTHON_AI_RAG_FEEDBACK_TIMEOUT_SECONDS");
        return firstPositive(legacyEnvValue, ragFeedbackTimeoutSeconds, timeoutSeconds, 10);
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
}
