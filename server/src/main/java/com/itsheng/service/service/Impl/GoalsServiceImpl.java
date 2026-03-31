package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.pojo.dto.GoalCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneCreateDTO;
import com.itsheng.pojo.dto.GoalMilestoneUpdateDTO;
import com.itsheng.pojo.dto.GoalUpdateDTO;
import com.itsheng.pojo.entity.Goal;
import com.itsheng.pojo.entity.GoalMilestone;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.GoalMilestoneMapper;
import com.itsheng.service.service.GoalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalsServiceImpl implements GoalsService {

    private final GoalMapper goalMapper;
    private final GoalMilestoneMapper milestoneMapper;
    private final ObjectMapper objectMapper;

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
            List<GoalMilestone> milestoneList = milestoneMapper.findByGoalId(primaryGoal.getId());
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
        List<GoalMilestone> milestoneList = milestoneMapper.findByGoalId(goalId);
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
        
        goalMapper.update(goal);
    }

    @Override
    @Transactional
    public void deleteGoal(Long goalId) {
        Long userId = BaseContext.getUserId();
        
        // 先删除里程碑
        milestoneMapper.deleteByGoalId(goalId);
        
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
    public void updateMilestone(Long milestoneId, GoalMilestoneUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        
        GoalMilestone milestone = milestoneMapper.findByIdAndUserId(milestoneId, userId);
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
        
        milestoneMapper.update(milestone);
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
            log.error("Failed to parse JSON list: {}", json, e);
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
}
