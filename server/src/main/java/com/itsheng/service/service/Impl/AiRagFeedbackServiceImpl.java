package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.dto.AiRagFeedbackDTO;
import com.itsheng.pojo.dto.AiRagSettingsDTO;
import com.itsheng.pojo.entity.CareerReport;
import com.itsheng.pojo.entity.ChatMessage;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.pojo.vo.AiRagFeedbackVO;
import com.itsheng.pojo.vo.AiRagSettingsVO;
import com.itsheng.service.client.PythonRagFeedbackClient;
import com.itsheng.service.mapper.CareerReportMapper;
import com.itsheng.service.mapper.ChatMessageMapper;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.service.AiRagFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiRagFeedbackServiceImpl implements AiRagFeedbackService {

    private static final Set<String> TARGET_TYPES = Set.of(
            "CHAT_MESSAGE", "RESUME_ANALYSIS", "JOB_MATCH", "MARKET_INSIGHT", "REPORT",
            "ROADMAP", "GOAL_ADVICE", "NOTIFICATION_AI_ADVICE"
    );
    private static final Set<String> LANGUAGES = Set.of("zh-CN", "en-US");
    private static final Set<String> FEEDBACK_USAGE_SCOPES = Set.of("local_eval_only", "personalization", "disabled");
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PythonRagFeedbackClient pythonRagFeedbackClient;
    private final ObjectMapper objectMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ResumeMapper resumeMapper;
    private final CareerReportMapper careerReportMapper;
    private final GoalMapper goalMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final UserRoadmapStepsMapper userRoadmapStepsMapper;
    private final Map<Long, AiRagSettingsDTO> settingsStore = new ConcurrentHashMap<>();

    @Override
    public AiRagFeedbackVO submitFeedback(AiRagFeedbackDTO dto) {
        Long userId = currentUserId();
        validateFeedback(dto);
        if (!ownsTarget(userId, dto.getTargetType(), dto.getTargetId())) {
            throw new BaseException("无权反馈该 AI 结果");
        }

        Map<String, Object> payload = buildFeedbackPayload(userId, dto);
        JsonNode data = callFeedback(payload);
        return AiRagFeedbackVO.builder()
                .feedbackId(text(data, "feedback_id"))
                .accepted(data.path("accepted").asBoolean(false))
                .usedFor(list(data.get("used_for")))
                .qualityDimensions(map(data.get("quality_dimensions")))
                .diagnostics(map(data.get("diagnostics")))
                .build();
    }

    @Override
    public AiRagSettingsVO getSettings() {
        Long userId = currentUserId();
        return toSettingsVO(settingsStore.computeIfAbsent(userId, ignored -> defaultSettings()), false, null, null);
    }

    @Override
    public AiRagSettingsVO updateSettings(AiRagSettingsDTO dto) {
        Long userId = currentUserId();
        AiRagSettingsDTO normalized = normalizeSettings(dto);
        JsonNode data = callPreferencesValidate(buildPreferencesPayload(userId, normalized));
        settingsStore.put(userId, normalized);
        return toSettingsVO(normalized, true, map(data.get("metadata_filters")), map(data.get("diagnostics")));
    }

    private void validateFeedback(AiRagFeedbackDTO dto) {
        if (dto == null || blank(dto.getTargetType()) || blank(dto.getTargetId()) || dto.getRating() == null) {
            throw new BaseException("AI 反馈参数错误");
        }
        if (!TARGET_TYPES.contains(dto.getTargetType())) {
            throw new BaseException("AI 反馈目标类型不支持");
        }
        if (dto.getRating() < -1 || dto.getRating() > 1) {
            throw new BaseException("AI 反馈评分只能为 -1、0 或 1");
        }
        if (dto.getComment() != null && dto.getComment().length() > 500) {
            throw new BaseException("AI 反馈说明不能超过 500 字");
        }
    }

    private boolean ownsTarget(Long userId, String targetType, String targetId) {
        Long id = parseLong(targetId);
        if (id == null && !"NOTIFICATION_AI_ADVICE".equals(targetType)) {
            return false;
        }
        return switch (targetType) {
            case "CHAT_MESSAGE" -> {
                ChatMessage message = chatMessageMapper.selectById(id);
                yield message != null && userId.equals(message.getUserId());
            }
            case "RESUME_ANALYSIS" -> resumeMapper.selectByIdAndUserId(id, userId) != null;
            case "JOB_MATCH", "MARKET_INSIGHT" -> jobCategoryMapper.selectById(id) != null;
            case "REPORT" -> {
                CareerReport report = careerReportMapper.selectById(id);
                yield report != null && userId.equals(report.getUserId());
            }
            case "GOAL_ADVICE" -> goalMapper.findByIdAndUserId(id, userId) != null;
            case "ROADMAP" -> {
                UserRoadmapSteps roadmap = userRoadmapStepsMapper.selectByIdAndUserId(id, userId);
                yield roadmap != null;
            }
            case "NOTIFICATION_AI_ADVICE" -> ownsNotificationAdviceSource(userId, targetId);
            default -> false;
        };
    }

    private boolean ownsNotificationAdviceSource(Long userId, String targetId) {
        if (blank(targetId)) {
            return false;
        }
        String[] parts = targetId.trim().split(":", 2);
        if (parts.length != 2 || blank(parts[0]) || blank(parts[1])) {
            return false;
        }
        String sourceType = parts[0].trim();
        if ("NOTIFICATION_AI_ADVICE".equals(sourceType)) {
            return false;
        }
        return ownsTarget(userId, sourceType, parts[1].trim());
    }

    private Map<String, Object> buildFeedbackPayload(Long userId, AiRagFeedbackDTO dto) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", dto.getTargetType());
        target.put("id", dto.getTargetId());
        target.put("page", dto.getPage());

        Map<String, Object> feedback = new LinkedHashMap<>();
        feedback.put("rating", dto.getRating());
        feedback.put("reason_tags", safeList(dto.getReasonTags()));
        feedback.put("comment", dto.getComment());
        feedback.put("user_action", dto.getUserAction());

        Map<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("trace_id", dto.getRetrievalTraceId());
        retrieval.put("evidence_ref_ids", safeList(dto.getEvidenceRefIds()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", requestId("rag-feedback", userId));
        payload.put("user_id", userId);
        payload.put("target", target);
        payload.put("feedback", feedback);
        payload.put("retrieval", retrieval);
        return payload;
    }

    private Map<String, Object> buildPreferencesPayload(Long userId, AiRagSettingsDTO dto) {
        Map<String, Object> preferences = new LinkedHashMap<>();
        preferences.put("preferred_city", dto.getPreferredCity());
        preferences.put("preferred_industries", safeList(dto.getPreferredIndustries()));
        preferences.put("preferred_job_levels", safeList(dto.getPreferredJobLevels()));
        preferences.put("career_direction", dto.getCareerDirection());
        preferences.put("result_language", dto.getResultLanguage());
        preferences.put("feedback_usage_scope", dto.getFeedbackUsageScope());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", requestId("rag-preference", userId));
        payload.put("user_id", userId);
        payload.put("preferences", preferences);
        return payload;
    }

    private JsonNode callFeedback(Map<String, Object> payload) {
        try {
            return successData(pythonRagFeedbackClient.submitFeedback(payload), "AI 反馈服务暂不接受该反馈");
        } catch (PythonRagFeedbackClient.PythonRagTimeoutException e) {
            throw new BaseException("AI 反馈服务超时，请稍后重试", e);
        } catch (PythonRagFeedbackClient.PythonRagClientException e) {
            log.warn("Python RAG 反馈服务返回异常: status={}, body={}", e.getStatusCode(), e.getResponseBody());
            throw new BaseException(e.getStatusCode() >= 400 && e.getStatusCode() < 500
                    ? "AI 反馈参数错误"
                    : "AI 反馈服务暂不可用", e);
        } catch (PythonRagFeedbackClient.PythonRagUnavailableException e) {
            throw new BaseException("AI 反馈服务暂不可用", e);
        }
    }

    private JsonNode callPreferencesValidate(Map<String, Object> payload) {
        try {
            return successData(pythonRagFeedbackClient.validatePreferences(payload), "AI 设置参数错误");
        } catch (PythonRagFeedbackClient.PythonRagTimeoutException e) {
            throw new BaseException("AI 设置校验服务超时，请稍后重试", e);
        } catch (PythonRagFeedbackClient.PythonRagClientException e) {
            log.warn("Python RAG 偏好校验服务返回异常: status={}, body={}", e.getStatusCode(), e.getResponseBody());
            throw new BaseException(e.getStatusCode() >= 400 && e.getStatusCode() < 500
                    ? "AI 设置参数错误"
                    : "AI 设置校验服务暂不可用", e);
        } catch (PythonRagFeedbackClient.PythonRagUnavailableException e) {
            throw new BaseException("AI 设置校验服务暂不可用", e);
        }
    }

    private JsonNode successData(JsonNode root, String failureMessage) {
        if (root == null || root.path("code").asInt(0) != 1 || !root.has("data")) {
            throw new BaseException(failureMessage);
        }
        return root.get("data");
    }

    private AiRagSettingsDTO normalizeSettings(AiRagSettingsDTO dto) {
        if (dto == null) {
            throw new BaseException("AI 设置参数错误");
        }
        AiRagSettingsDTO normalized = defaultSettings();
        normalized.setEnableAiAdviceNotifications(firstNonNull(dto.getEnableAiAdviceNotifications(), normalized.getEnableAiAdviceNotifications()));
        normalized.setEnableRagPersonalization(firstNonNull(dto.getEnableRagPersonalization(), normalized.getEnableRagPersonalization()));
        normalized.setPreferredCity(trimToNull(dto.getPreferredCity()));
        normalized.setPreferredIndustries(limitList(dto.getPreferredIndustries(), 10));
        normalized.setPreferredJobLevels(limitList(dto.getPreferredJobLevels(), 10));
        normalized.setCareerDirection(trimToNull(dto.getCareerDirection()));
        normalized.setResultLanguage(blank(dto.getResultLanguage()) ? "zh-CN" : dto.getResultLanguage().trim());
        normalized.setFeedbackUsageScope(blank(dto.getFeedbackUsageScope()) ? "local_eval_only" : dto.getFeedbackUsageScope().trim());
        if (!LANGUAGES.contains(normalized.getResultLanguage()) || !FEEDBACK_USAGE_SCOPES.contains(normalized.getFeedbackUsageScope())) {
            throw new BaseException("AI 设置参数错误");
        }
        return normalized;
    }

    private AiRagSettingsDTO defaultSettings() {
        AiRagSettingsDTO dto = new AiRagSettingsDTO();
        dto.setEnableAiAdviceNotifications(true);
        dto.setEnableRagPersonalization(true);
        dto.setPreferredIndustries(Collections.emptyList());
        dto.setPreferredJobLevels(Collections.emptyList());
        dto.setResultLanguage("zh-CN");
        dto.setFeedbackUsageScope("local_eval_only");
        return dto;
    }

    private AiRagSettingsVO toSettingsVO(AiRagSettingsDTO dto, Boolean updated, Map<String, Object> filters, Map<String, Object> diagnostics) {
        return AiRagSettingsVO.builder()
                .enableAiAdviceNotifications(dto.getEnableAiAdviceNotifications())
                .enableRagPersonalization(dto.getEnableRagPersonalization())
                .preferredCity(dto.getPreferredCity())
                .preferredIndustries(safeList(dto.getPreferredIndustries()))
                .preferredJobLevels(safeList(dto.getPreferredJobLevels()))
                .careerDirection(dto.getCareerDirection())
                .resultLanguage(dto.getResultLanguage())
                .feedbackUsageScope(dto.getFeedbackUsageScope())
                .updated(updated)
                .effectiveFilters(filters)
                .diagnostics(diagnostics)
                .build();
    }

    private Long currentUserId() {
        Long userId = BaseContext.getUserId();
        if (userId == null) {
            throw new BaseException("未登录");
        }
        return userId;
    }

    private String requestId(String prefix, Long userId) {
        return prefix + "-" + userId + "-" + LocalDateTime.now().format(REQUEST_TIME_FORMATTER) + "-" + UUID.randomUUID();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> list(JsonNode node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> result.add(item.asText()));
        return result;
    }

    private Map<String, Object> map(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, Map.class);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private List<String> limitList(List<String> values, int limit) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .limit(limit)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T firstNonNull(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
