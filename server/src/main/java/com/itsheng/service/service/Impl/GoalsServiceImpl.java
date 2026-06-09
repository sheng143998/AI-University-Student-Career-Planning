package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.dto.GoalCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneUpdateDTO;
import com.itsheng.pojo.dto.GoalUpdateDTO;
import com.itsheng.pojo.entity.Goal;
import com.itsheng.pojo.entity.GoalMilestone;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.client.PythonGoalsAdviceClient;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.GoalMilestoneMapper;
import com.itsheng.service.service.GoalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalsServiceImpl implements GoalsService {

    private final GoalMapper goalMapper;
    private final GoalMilestoneMapper milestoneMapper;
    private final ObjectMapper objectMapper;
    private final PythonGoalsAdviceClient pythonGoalsAdviceClient;
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}|(?:\\+?86[-\\s]?)?1[3-9]\\d{9}|(?:api[_-]?key|token|secret|password)\\s*[:=]\\s*[\\w.-]{6,}|(?:sk|ak|token|secret|key)[-_.][A-Za-z0-9._-]{8,}",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public GoalsOverviewVO overview() {
        Long userId = BaseContext.getUserId();
        
        // 查询主目标
        Goal primaryGoal = goalMapper.findPrimaryByUserId(userId);
        GoalSummaryVO primaryGoalVO = null;
        if (primaryGoal != null) {
            primaryGoalVO = convertToSummaryVO(primaryGoal);
        }
        
        // 查询主目标的里程碑
        List<GoalMilestoneVO> milestones = new ArrayList<>();
        int milestonesCompleted = 0;
        int milestonesTotal = 0;
        SuccessCriteriaVO successCriteria = null;
        List<LongTermAspirationVO> longTermAspirations = new ArrayList<>();
        AiAdviceVO aiAdvice = null;
        
        if (primaryGoal != null) {
            List<GoalMilestone> milestoneList = milestoneMapper.findByGoalIdAndUserId(primaryGoal.getId(), userId);
            milestonesTotal = milestoneList.size();
            for (GoalMilestone m : milestoneList) {
                milestones.add(convertToMilestoneVO(m));
                if ("DONE".equals(m.getStatus())) {
                    milestonesCompleted++;
                }
            }
            
            // 解析成功准则
            successCriteria = parseSuccessCriteria(primaryGoal);
            
            // 解析长期愿景
            longTermAspirations = readJsonList(primaryGoal.getLongTermAspirations(), 
                new TypeReference<List<LongTermAspirationVO>>() {});
            
            // AI建议
            if (primaryGoal.getAiAdvice() != null && !primaryGoal.getAiAdvice().isEmpty()) {
                aiAdvice = new AiAdviceVO(primaryGoal.getAiAdvice());
            }
        }
        
        // 查询并行目标
        List<Goal> parallelGoals = goalMapper.findParallelByUserId(userId);
        List<GoalSummaryVO> parallelGoalVOs = new ArrayList<>();
        for (Goal g : parallelGoals) {
            parallelGoalVOs.add(convertToSummaryVO(g));
        }
        
        return GoalsOverviewVO.builder()
                .primaryGoal(primaryGoalVO)
                .milestones(milestones)
                .milestonesCompleted(milestonesCompleted)
                .milestonesTotal(milestonesTotal)
                .successCriteria(successCriteria != null ? successCriteria : new SuccessCriteriaVO("", new ArrayList<>(), new ArrayList<>()))
                .longTermAspirations(longTermAspirations)
                .aiAdvice(aiAdvice != null ? aiAdvice : new AiAdviceVO(""))
                .parallelGoals(parallelGoalVOs)
                .build();
    }

    @Override
    @Transactional
    public IdVO createGoal(GoalCreateDTO dto) {
        Long userId = BaseContext.getUserId();
        
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(dto.getTitle());
        goal.setGoalDesc(dto.getDesc());
        goal.setStatus(dto.getStatus() != null ? dto.getStatus() : "TODO");
        goal.setProgress(dto.getProgress() != null ? dto.getProgress() : 0);
        goal.setEta(dto.getEta());
        goal.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        
        // 如果设为主目标，先取消其他主目标
        if (Boolean.TRUE.equals(goal.getIsPrimary())) {
            goalMapper.clearPrimaryByUserId(userId);
        }
        
        // 初始化JSON字段为空数组
        goal.setSuccessCompanies("[]");
        goal.setSuccessCities("[]");
        goal.setLongTermAspirations("[]");
        
        goalMapper.insert(goal);
        
        return new IdVO(String.valueOf(goal.getId()));
    }

    @Override
    public GoalDetailVO getGoalDetail(Long goalId) {
        Long userId = BaseContext.getUserId();
        
        Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
        if (goal == null) {
            return null;
        }
        
        // 查询里程碑
        List<GoalMilestone> milestoneList = milestoneMapper.findByGoalIdAndUserId(goalId, userId);
        List<GoalMilestoneVO> milestoneVOs = new ArrayList<>();
        for (GoalMilestone m : milestoneList) {
            milestoneVOs.add(convertToMilestoneVO(m));
        }
        
        return GoalDetailVO.builder()
                .goal(convertToSummaryVO(goal))
                .milestones(milestoneVOs)
                .successCriteria(parseSuccessCriteria(goal))
                .longTermAspirations(readJsonList(goal.getLongTermAspirations(), 
                    new TypeReference<List<LongTermAspirationVO>>() {}))
                .aiAdvice(goal.getAiAdvice() != null ? new AiAdviceVO(goal.getAiAdvice()) : new AiAdviceVO(""))
                .build();
    }

    @Override
    @Transactional
    public AiAdviceVO generateAiAdvice(Long goalId) {
        Long userId = BaseContext.getUserId();
        Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
        if (goal == null) {
            throw new BaseException("目标不存在");
        }

        AiAdviceVO advice = pythonGoalsAdviceClient.generateGoalAdvice(buildGoalAdvicePayload(userId, goal));
        int updated = goalMapper.updateAiAdviceByIdAndUserId(goalId, userId, advice.getContent());
        if (updated != 1) {
            throw new BaseException("目标不存在");
        }
        return advice;
    }

    @Override
    @Transactional
    public void updateGoal(Long goalId, GoalUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        
        Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
        if (goal == null) {
            return;
        }
        
        if (dto.getTitle() != null) {
            goal.setTitle(dto.getTitle());
        }
        if (dto.getDesc() != null) {
            goal.setGoalDesc(dto.getDesc());
        }
        if (dto.getStatus() != null) {
            goal.setStatus(dto.getStatus());
        }
        if (dto.getProgress() != null) {
            goal.setProgress(dto.getProgress());
        }
        if (dto.getEta() != null) {
            goal.setEta(dto.getEta());
        }
        if (dto.getIsPrimary() != null) {
            if (Boolean.TRUE.equals(dto.getIsPrimary())) {
                goalMapper.clearPrimaryByUserId(userId);
            }
            goal.setIsPrimary(dto.getIsPrimary());
        }
        
        // 更新成功准则
        if (dto.getSuccessCriteria() != null) {
            SuccessCriteriaVO sc = dto.getSuccessCriteria();
            goal.setSuccessSalary(sc.getSalary());
            goal.setSuccessCompanies(writeJson(sc.getCompanies()));
            goal.setSuccessCities(writeJson(sc.getCities()));
        }
        
        // 更新长期愿景
        if (dto.getLongTermAspirations() != null) {
            goal.setLongTermAspirations(writeJson(dto.getLongTermAspirations()));
        }
        
        // 更新AI建议
        if (dto.getAiAdvice() != null) {
            goal.setAiAdvice(dto.getAiAdvice().getContent());
        }
        
        goalMapper.updateByIdAndUserId(goal);
    }

    @Override
    @Transactional
    public void deleteGoal(Long goalId) {
        Long userId = BaseContext.getUserId();
        Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
        if (goal == null) {
            return;
        }
        
        // 先删除里程碑
        milestoneMapper.deleteByGoalIdAndUserId(goalId, userId);
        
        // 删除目标
        goalMapper.deleteByIdAndUserId(goalId, userId);
    }

    @Override
    @Transactional
    public IdVO createMilestone(Long goalId, GoalMilestoneCreateDTO dto) {
        Long userId = BaseContext.getUserId();
        
        // 验证目标存在
        Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
        if (goal == null) {
            return null;
        }
        
        GoalMilestone milestone = new GoalMilestone();
        milestone.setGoalId(goalId);
        milestone.setUserId(userId);
        milestone.setTitle(dto.getTitle());
        milestone.setMilestoneDesc(dto.getDesc());
        milestone.setStatus(dto.getStatus() != null ? dto.getStatus() : "TODO");
        milestone.setProgress(dto.getProgress() != null ? dto.getProgress() : 0);
        milestone.setSortOrder(dto.getOrder() != null ? dto.getOrder() : 1);
        
        milestoneMapper.insert(milestone);
        
        return new IdVO(String.valueOf(milestone.getId()));
    }

    @Override
    @Transactional
    public void updateMilestone(Long goalId, Long milestoneId, GoalMilestoneUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        
        GoalMilestone milestone = milestoneMapper.findByGoalIdAndIdAndUserId(goalId, milestoneId, userId);
        if (milestone == null) {
            return;
        }
        
        if (dto.getTitle() != null) {
            milestone.setTitle(dto.getTitle());
        }
        if (dto.getDesc() != null) {
            milestone.setMilestoneDesc(dto.getDesc());
        }
        if (dto.getStatus() != null) {
            milestone.setStatus(dto.getStatus());
            // 如果状态变为DONE，自动设置进度为100
            if ("DONE".equals(dto.getStatus())) {
                milestone.setProgress(100);
            }
        }
        if (dto.getProgress() != null) {
            milestone.setProgress(dto.getProgress());
        }
        if (dto.getOrder() != null) {
            milestone.setSortOrder(dto.getOrder());
        }
        
        milestoneMapper.updateByIdAndUserId(milestone);
    }
    
    // ===== Helper Methods =====
    
    private GoalSummaryVO convertToSummaryVO(Goal goal) {
        return GoalSummaryVO.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .desc(goal.getGoalDesc())
                .status(goal.getStatus())
                .progress(goal.getProgress())
                .eta(goal.getEta())
                .isPrimary(goal.getIsPrimary())
                .build();
    }
    
    private GoalMilestoneVO convertToMilestoneVO(GoalMilestone m) {
        return GoalMilestoneVO.builder()
                .id(m.getId())
                .goalId(m.getGoalId())
                .title(m.getTitle())
                .desc(m.getMilestoneDesc())
                .status(m.getStatus())
                .progress(m.getProgress())
                .order(m.getSortOrder())
                .build();
    }
    
    private SuccessCriteriaVO parseSuccessCriteria(Goal goal) {
        List<String> companies = readJsonList(goal.getSuccessCompanies(), new TypeReference<List<String>>() {});
        List<String> cities = readJsonList(goal.getSuccessCities(), new TypeReference<List<String>>() {});
        return new SuccessCriteriaVO(
            goal.getSuccessSalary() != null ? goal.getSuccessSalary() : "",
            companies,
            cities
        );
    }
    
    private <T> List<T> readJsonList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Failed to parse JSON list, length: {}, errorType: {}",
                    json.length(), e.getClass().getSimpleName(), e);
            return new ArrayList<>();
        }
    }
    
    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to write JSON", e);
            return "[]";
        }
    }

    private Map<String, Object> buildGoalAdvicePayload(Long userId, Goal goal) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", String.valueOf(userId));
        payload.put("goal", Map.of(
                "id", String.valueOf(goal.getId()),
                "title", safeText(goal.getTitle(), 160),
                "desc", safeText(goal.getGoalDesc(), 240),
                "status", safeText(goal.getStatus(), 40),
                "progress", goal.getProgress() == null ? 0 : goal.getProgress(),
                "eta", safeText(goal.getEta(), 80),
                "isPrimary", Boolean.TRUE.equals(goal.getIsPrimary())
        ));

        List<Map<String, Object>> milestones = new ArrayList<>();
        for (GoalMilestone milestone : milestoneMapper.findByGoalIdAndUserId(goal.getId(), userId)) {
            if (!userId.equals(milestone.getUserId())) {
                continue;
            }
            milestones.add(Map.of(
                    "id", String.valueOf(milestone.getId()),
                    "title", safeText(milestone.getTitle(), 160),
                    "desc", safeText(milestone.getMilestoneDesc(), 220),
                    "status", safeText(milestone.getStatus(), 40),
                    "progress", milestone.getProgress() == null ? 0 : milestone.getProgress(),
                    "order", milestone.getSortOrder() == null ? 0 : milestone.getSortOrder()
            ));
        }
        payload.put("milestones", milestones);
        payload.put("successCriteria", Map.of(
                "salary", safeText(goal.getSuccessSalary(), 80),
                "companies", sanitizeList(readJsonList(goal.getSuccessCompanies(), new TypeReference<List<String>>() {}), 80),
                "cities", sanitizeList(readJsonList(goal.getSuccessCities(), new TypeReference<List<String>>() {}), 80)
        ));
        payload.put("longTermAspirations", sanitizeAspirations(
                readJsonList(goal.getLongTermAspirations(), new TypeReference<List<LongTermAspirationVO>>() {})
        ));
        payload.put("retrievalOptions", Map.of(
                "chunking", "recursive",
                "summaryIndex", true,
                "metadataFilters", Map.of(
                        "userId", String.valueOf(userId),
                        "goalId", String.valueOf(goal.getId()),
                        "documentTypes", List.of("goal", "milestone", "successCriteria"),
                        "visibilityScope", "USER_PRIVATE"
                ),
                "multiQuery", true,
                "hybridSearch", List.of("bm25", "embedding"),
                "fusion", "rag_fusion_rrf",
                "rankingModel", "deterministic_fallback"
        ));
        return payload;
    }

    private List<String> sanitizeList(List<String> values, int maxChars) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String text = safeText(value, maxChars);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }

    private List<Map<String, String>> sanitizeAspirations(List<LongTermAspirationVO> aspirations) {
        List<Map<String, String>> result = new ArrayList<>();
        for (LongTermAspirationVO aspiration : aspirations) {
            result.add(Map.of(
                    "title", safeText(aspiration.getTitle(), 120),
                    "desc", safeText(aspiration.getDesc(), 180)
            ));
        }
        return result;
    }

    private String safeText(String value, int maxChars) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = SENSITIVE_PATTERN.matcher(value).replaceAll("[REDACTED]").replaceAll("\\s+", " ").trim();
        return clean.length() <= maxChars ? clean : clean.substring(0, maxChars);
    }
}
