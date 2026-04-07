package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRoadmapStepsMapper userRoadmapStepsMapper;
    private final UserCareerDataMapper userCareerDataMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getSummary(Long userId) {
        log.info("获取仪表盘汇总信息，userId: {}", userId);

        // 从 user_career_data 表读取数据
        UserCareerData careerData = userCareerDataMapper.selectByUserId(userId);
        if (careerData == null || careerData.getJobProfile() == null) {
            log.warn("用户尚未上传简历或无职业数据，userId: {}", userId);
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. job_profile
        try {
            if (careerData.getJobProfile() != null && !careerData.getJobProfile().equals("{}")) {
                Map<String, Object> jobProfile = objectMapper.readValue(careerData.getJobProfile(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                result.put("job_profile", jobProfile);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 job_profile JSON 失败", e);
        }

        // 2. match_summary
        try {
            if (careerData.getMatchSummary() != null && !careerData.getMatchSummary().isEmpty()) {
                Map<String, Object> matchSummary = objectMapper.readValue(careerData.getMatchSummary(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                result.put("match_summary", matchSummary);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 match_summary JSON 失败", e);
        }

        // 3. market_trends
        try {
            if (careerData.getMarketTrends() != null && !careerData.getMarketTrends().equals("[]")) {
                List<Object> marketTrends = objectMapper.readValue(careerData.getMarketTrends(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
                result.put("market_trends", marketTrends);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 market_trends JSON 失败", e);
        }

        // 4. skill_radar
        try {
            if (careerData.getSkillRadar() != null && !careerData.getSkillRadar().isEmpty()) {
                Map<String, Object> skillRadar = objectMapper.readValue(careerData.getSkillRadar(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                result.put("skill_radar", skillRadar);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 skill_radar JSON 失败", e);
        }

        // 5. actions
        try {
            if (careerData.getActions() != null && !careerData.getActions().equals("[]")) {
                List<Object> actions = objectMapper.readValue(careerData.getActions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
                result.put("actions", actions);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 actions JSON 失败", e);
        }

        return result;
    }

    @Override
    public Map<String, Object> getRoadmap(Long userId) {
        log.info("获取用户职业发展路径，userId: {}", userId);

        // 1. 查询是否已有 roadmap
        UserRoadmapSteps existing = userRoadmapStepsMapper.selectByUserId(userId);
        if (existing != null && existing.getSteps() != null) {
            log.info("用户已有职业发展路径数据，userId: {}", userId);
            return buildRoadmapResponse(existing);
        }

        // 2. 不存在则自动创建：基于用户目标岗位的垂直晋升路径
        log.info("用户无职业发展路径数据，开始自动创建，userId: {}", userId);
        UserRoadmapSteps roadmap = createRoadmapFromTargetJob(userId);
        if (roadmap == null) {
            return null;
        }
        userRoadmapStepsMapper.insert(roadmap);
        log.info("用户职业发展路径已创建，userId: {}, roadmapId: {}", userId, roadmap.getId());

        return buildRoadmapResponse(roadmap);
    }

    @Override
    public List<Map<String, Object>> updateCurrentStep(Long userId, Integer currentStepIndex) {
        log.info("更新用户当前阶段，userId: {}, newIndex: {}", userId, currentStepIndex);

        // 1. 查询现有 roadmap
        UserRoadmapSteps roadmap = userRoadmapStepsMapper.selectByUserId(userId);
        if (roadmap == null) {
            throw new RuntimeException("请先上传简历以生成职业规划");
        }

        // 2. 解析 steps 并验证索引
        List<Map<String, Object>> stepsList;
        try {
            stepsList = objectMapper.readValue(roadmap.getSteps(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (JsonProcessingException e) {
            log.error("解析 steps JSON 失败", e);
            throw new RuntimeException("职业发展路径数据异常");
        }

        if (currentStepIndex < 0 || currentStepIndex >= stepsList.size()) {
            throw new IllegalArgumentException("阶段索引超出有效范围");
        }

        // 3. 更新 current_step_index
        roadmap.setCurrentStepIndex(currentStepIndex);
        roadmap.setUpdateTime(LocalDateTime.now());

        // 4. 重新计算 active 状态并更新 steps JSON
        List<Map<String, Object>> updatedSteps = recalculateStepsActive(stepsList, currentStepIndex);
        try {
            roadmap.setSteps(objectMapper.writeValueAsString(updatedSteps));
        } catch (JsonProcessingException e) {
            log.error("序列化 steps JSON 失败", e);
            throw new RuntimeException("更新职业发展路径失败");
        }

        userRoadmapStepsMapper.update(roadmap);
        log.info("用户当前阶段已更新，userId: {}, newIndex: {}", userId, currentStepIndex);

        return updatedSteps;
    }

    /**
     * 基于用户目标岗位创建垂直晋升路径
     */
    private UserRoadmapSteps createRoadmapFromTargetJob(Long userId) {
        // 获取用户职业数据中的目标岗位
        UserCareerData careerData = userCareerDataMapper.selectByUserId(userId);
        if (careerData == null || careerData.getTargetJobId() == null) {
            log.warn("用户尚未上传简历或无目标岗位，无法创建职业发展路径，userId: {}", userId);
            return null;
        }

        Long targetJobId = careerData.getTargetJobId();

        // 查询目标岗位
        JobCategory targetJob = jobCategoryMapper.selectById(targetJobId);
        if (targetJob == null) {
            log.warn("目标岗位不存在，jobId: {}", targetJobId);
            return null;
        }

        // 查询同一 job_category_code 基础类别的所有级别（垂直晋升路径）
        String baseCategoryCode = targetJob.getJobCategoryCode()
                .replaceAll("_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "");
        List<JobCategory> verticalPath = jobCategoryMapper.selectVerticalPathByCategoryCode(baseCategoryCode);

        if (verticalPath == null || verticalPath.isEmpty()) {
            log.warn("未找到垂直晋升路径，baseCategoryCode: {}", baseCategoryCode);
            return null;
        }

        // 构建 steps 列表
        List<Map<String, Object>> steps = new ArrayList<>();
        int index = 0;
        for (JobCategory job : verticalPath) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("job_id", job.getId());
            step.put("title", job.getJobCategoryName());
            step.put("level", job.getJobLevel());
            step.put("level_name", job.getJobLevelName());

            // 根据级别设置时间目标
            String timeGoal = switch (job.getJobLevel()) {
                case "INTERNSHIP" -> "目标：第 0-1 年";
                case "JUNIOR" -> "目标：第 1-3 年";
                case "MID" -> "目标：第 3-5 年";
                case "SENIOR" -> "目标：第 5-8 年";
                default -> "目标：待定";
            };
            step.put("time", timeGoal);

            // 图标根据级别设置
            String icon = switch (job.getJobLevel()) {
                case "INTERNSHIP" -> "person";
                case "JUNIOR" -> "star";
                case "MID" -> "diamond";
                case "SENIOR" -> "flag";
                default -> "circle";
            };
            step.put("icon", icon);

            // 匹配状态和 active 状态
            if (index == 0) {
                step.put("status", "85% 匹配");
                step.put("active", true);
            } else {
                step.put("status", "待解锁");
                step.put("active", false);
            }

            steps.add(step);
            index++;
        }

        String stepsJson;
        try {
            stepsJson = objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            log.error("序列化 roadmap steps 失败", e);
            return null;
        }

        return UserRoadmapSteps.builder()
                .userId(userId)
                .jobProfileId(targetJobId)
                .currentStepIndex(0)
                .steps(stepsJson)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    /**
     * 构建 roadmap 响应数据
     */
    private Map<String, Object> buildRoadmapResponse(UserRoadmapSteps roadmap) {
        Map<String, Object> response = new LinkedHashMap<>();

        // current_step_index
        response.put("current_step_index", roadmap.getCurrentStepIndex());

        // target_job_id 和 target_job_name（从 user_career_data 获取）
        UserCareerData careerData = userCareerDataMapper.selectByUserId(roadmap.getUserId());
        if (careerData != null) {
            response.put("target_job_id", careerData.getTargetJobId());
            response.put("target_job_name", careerData.getTargetJob());
        }

        // steps 列表
        try {
            if (roadmap.getSteps() != null) {
                List<Map<String, Object>> stepsList = objectMapper.readValue(roadmap.getSteps(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                response.put("steps", stepsList);
            }
        } catch (JsonProcessingException e) {
            log.error("解析 roadmap steps JSON 失败", e);
            response.put("steps", Collections.emptyList());
        }

        return response;
    }

    /**
     * 重新计算 steps 的 active 和 status 状态
     */
    private List<Map<String, Object>> recalculateStepsActive(List<Map<String, Object>> steps, int currentIndex) {
        List<String> levelOrder = List.of("INTERNSHIP", "JUNIOR", "MID", "SENIOR");
        List<String> icons = List.of("check", "star", "diamond", "flag");
        List<String> statuses = List.of("已完成", "当前阶段", "待解锁", "待解锁");

        List<Map<String, Object>> updatedSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Map<String, Object> step = new LinkedHashMap<>(steps.get(i));

            if (i < currentIndex) {
                step.put("active", false);
                step.put("status", "已完成");
                // 更新图标
                int levelIdx = levelOrder.indexOf((String) step.get("level"));
                if (levelIdx >= 0 && levelIdx < icons.size()) {
                    step.put("icon", "check");
                }
            } else if (i == currentIndex) {
                step.put("active", true);
                step.put("status", "当前阶段");
            } else {
                step.put("active", false);
                step.put("status", "待解锁");
            }

            updatedSteps.add(step);
        }
        return updatedSteps;
    }
}
