package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.service.client.PythonDashboardAiClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int MAX_DASHBOARD_PROFILE_SKILLS = 12;
    private static final Pattern DASHBOARD_PROFILE_EMAIL_PATTERN = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DASHBOARD_PROFILE_PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+?86[- ]?)?1[3-9]\\d{9}(?!\\d)"
    );
    private static final Pattern DASHBOARD_PROFILE_SECRET_PATTERN = Pattern.compile(
            "(?i)(token|secret|api[_-]?key)\\s*[:=]\\s*\\S+"
    );
    private static final Pattern DASHBOARD_PROFILE_TOKEN_LIKE_PATTERN = Pattern.compile(
            "(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9_-]{24,}"
    );

    private final UserRoadmapStepsMapper userRoadmapStepsMapper;
    private final UserCareerDataMapper userCareerDataMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final ResumeMapper resumeMapper;
    private final UserVectorStoreMapper userVectorStoreMapper;
    private final PythonDashboardAiClient pythonDashboardAiClient;
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
        UserCareerData careerData = userCareerDataMapper.selectByUserId(userId);
        JobCategory targetJob = resolveTargetJob(userId, careerData);
        if (targetJob == null) {
            log.warn("用户无可用目标岗位，且 RAG 未匹配到相似岗位，无法创建职业发展路径，userId: {}", userId);
            return null;
        }

        Long targetJobId = targetJob.getId();
        syncCareerTarget(userId, careerData, targetJob);

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
        if (careerData != null && careerData.getTargetJobId() != null) {
            response.put("target_job_id", careerData.getTargetJobId());
            response.put("target_job_name", careerData.getTargetJob());
        } else if (roadmap.getJobProfileId() != null) {
            JobCategory targetJob = jobCategoryMapper.selectById(roadmap.getJobProfileId());
            if (targetJob != null) {
                response.put("target_job_id", targetJob.getId());
                response.put("target_job_name", targetJob.getJobCategoryName());
            }
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

    private JobCategory resolveTargetJob(Long userId, UserCareerData careerData) {
        if (careerData != null && careerData.getTargetJobId() != null) {
            JobCategory targetJob = jobCategoryMapper.selectById(careerData.getTargetJobId());
            if (targetJob != null) {
                return targetJob;
            }
            log.warn("用户职业数据中的目标岗位不存在，尝试 RAG 重新匹配，userId: {}, jobId: {}",
                    userId, careerData.getTargetJobId());
        }

        List<ResumeAnalysisResult> latestResults = resumeMapper.selectByUserId(userId, null, 1);
        if (latestResults == null || latestResults.isEmpty()) {
            log.warn("用户暂无可用简历分析记录，无法执行 RAG 岗位匹配，userId: {}", userId);
            return null;
        }

        ResumeAnalysisResult latestResult = latestResults.get(0);
        if (latestResult.getVectorStoreId() == null || latestResult.getVectorStoreId().isBlank()) {
            log.warn("用户最新简历分析缺少向量存储 ID，无法执行 RAG 岗位匹配，userId: {}, analysisId: {}",
                    userId, latestResult.getId());
            return null;
        }

        var resumeVectorStore = userVectorStoreMapper.selectByVectorStoreIdAndUserId(
                latestResult.getVectorStoreId(), userId);
        if (resumeVectorStore != null && !Objects.equals(resumeVectorStore.getUserId(), userId)) {
            log.warn("用户简历向量归属不匹配，拒绝执行 Dashboard-AI 岗位匹配，userId: {}, vectorStoreId: {}",
                    userId, latestResult.getVectorStoreId());
            return null;
        }
        if (resumeVectorStore == null || resumeVectorStore.getResumeContent() == null
                || resumeVectorStore.getResumeContent().isBlank()) {
            log.warn("用户最新简历内容不存在，无法执行 RAG 岗位匹配，userId: {}, vectorStoreId: {}",
                    userId, latestResult.getVectorStoreId());
            return null;
        }

        Map<String, Object> payload = buildDashboardAiMatchPayload(userId, latestResult, resumeVectorStore.getResumeContent());
        PythonDashboardAiClient.TargetJobMatchResult matchResult = pythonDashboardAiClient.matchTargetJob(payload);
        JobCategory targetJob = jobCategoryMapper.selectById(matchResult.jobId());
        if (targetJob == null) {
            log.warn("Python Dashboard-AI 返回的岗位不存在，userId: {}, jobId: {}",
                    userId, matchResult.jobId());
            return null;
        }

        log.info("Python Dashboard-AI 为用户匹配目标岗位成功，userId: {}, jobId: {}, jobName: {}, score: {}",
                userId, targetJob.getId(), targetJob.getJobCategoryName(), matchResult.score());
        return targetJob;
    }

    private Map<String, Object> buildDashboardAiMatchPayload(Long userId, ResumeAnalysisResult latestResult,
                                                             String resumeContent) {
        List<JobCategory> jobCandidates = jobCategoryMapper.selectAll();
        if (jobCandidates == null || jobCandidates.isEmpty()) {
            log.warn("岗位候选快照为空，无法调用 Python Dashboard-AI，userId: {}", userId);
            throw new PythonDashboardAiClient.DashboardAiException("暂无可用岗位匹配结果");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("request_id", "dashboard-target-job-" + userId + "-" + latestResult.getId());
        payload.put("user_id", userId);
        payload.put("resume_analysis_id", latestResult.getId());
        payload.put("resume_vector_store_id", latestResult.getVectorStoreId());
        payload.put("resume_profile", parseResumeProfile(latestResult));
        payload.put("resume_content", truncateResumeContent(resumeContent));
        payload.put("job_candidates", buildJobCandidatePayload(jobCandidates));
        payload.put("filters", buildDashboardAiFilters());
        payload.put("top_k", 5);
        return payload;
    }

    private Map<String, Object> parseResumeProfile(ResumeAnalysisResult latestResult) {
        if (latestResult.getParsedData() == null || latestResult.getParsedData().isBlank()
                || "{}".equals(latestResult.getParsedData())) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> parsedProfile = objectMapper.readValue(latestResult.getParsedData(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            return whitelistDashboardResumeProfile(parsedProfile);
        } catch (JsonProcessingException e) {
            log.warn("解析简历画像 JSON 失败，analysisId: {}", latestResult.getId(), e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> whitelistDashboardResumeProfile(Map<String, Object> parsedProfile) {
        if (parsedProfile == null || parsedProfile.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        putTextIfPresent(profile, "target_role", parsedProfile.get("target_role"));

        List<String> skills = normalizeProfileSkills(parsedProfile.get("skills"));
        if (!skills.isEmpty()) {
            profile.put("skills", skills);
        }

        Object experienceYears = parsedProfile.get("experience_years");
        if (experienceYears instanceof Number) {
            profile.put("experience_years", experienceYears);
        }
        return profile;
    }

    private void putTextIfPresent(Map<String, Object> target, String key, Object value) {
        if (!isScalarProfileValue(value)) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty() && !containsDashboardProfileSensitiveValue(text)) {
            target.put(key, text);
        }
    }

    private List<String> normalizeProfileSkills(Object value) {
        List<?> rawSkills;
        if (value instanceof List<?> list) {
            rawSkills = list;
        } else if (value instanceof String text) {
            rawSkills = Arrays.asList(text.split("[,，、]"));
        } else {
            return Collections.emptyList();
        }

        Set<String> uniqueSkills = new LinkedHashSet<>();
        for (Object item : rawSkills) {
            if (item == null) {
                continue;
            }
            if (!isScalarProfileValue(item)) {
                continue;
            }
            String skill = String.valueOf(item).trim();
            if (skill.isEmpty() || containsDashboardProfileSensitiveValue(skill)) {
                continue;
            }
            uniqueSkills.add(skill);
            if (uniqueSkills.size() >= MAX_DASHBOARD_PROFILE_SKILLS) {
                break;
            }
        }
        return new ArrayList<>(uniqueSkills);
    }

    private boolean isScalarProfileValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character;
    }

    private boolean containsDashboardProfileSensitiveValue(String value) {
        return DASHBOARD_PROFILE_EMAIL_PATTERN.matcher(value).find()
                || DASHBOARD_PROFILE_PHONE_PATTERN.matcher(value).find()
                || DASHBOARD_PROFILE_SECRET_PATTERN.matcher(value).find()
                || DASHBOARD_PROFILE_TOKEN_LIKE_PATTERN.matcher(value).find();
    }

    private String truncateResumeContent(String resumeContent) {
        if (resumeContent == null) {
            return "";
        }
        String compact = resumeContent.trim();
        int maxChars = 4000;
        return compact.length() > maxChars ? compact.substring(0, maxChars) : compact;
    }

    private List<Map<String, Object>> buildJobCandidatePayload(List<JobCategory> jobs) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (JobCategory job : jobs) {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("job_id", job.getId());
            candidate.put("job_name", job.getJobCategoryName());
            candidate.put("job_category_code", job.getJobCategoryCode());
            candidate.put("job_level", job.getJobLevel());
            candidate.put("job_level_name", job.getJobLevelName());
            candidate.put("required_skills", parseJsonArray(job.getRequiredSkills()));
            candidate.put("job_description", job.getJobDescription());
            candidate.put("job_profile", parseJsonObject(job.getJobProfile()));
            candidates.add(candidate);
        }
        return candidates;
    }

    private Map<String, Object> buildDashboardAiFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("document_type", List.of("resume", "job", "jd"));
        filters.put("visibility_scope", "user_or_public");
        filters.put("language", "zh-CN");
        return filters;
    }

    private void syncCareerTarget(Long userId, UserCareerData existingCareerData, JobCategory targetJob) {
        boolean targetAlreadySynced = existingCareerData != null
                && Objects.equals(existingCareerData.getTargetJobId(), targetJob.getId())
                && Objects.equals(existingCareerData.getTargetJob(), targetJob.getJobCategoryName());
        if (targetAlreadySynced) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String jobProfileJson = buildTargetJobProfile(targetJob);

        if (existingCareerData != null) {
            UserCareerData updateData = UserCareerData.builder()
                    .userId(userId)
                    .targetJob(targetJob.getJobCategoryName())
                    .targetJobId(targetJob.getId())
                    .jobProfile(jobProfileJson)
                    .updateTime(now)
                    .build();
            userCareerDataMapper.update(updateData);
            log.info("已回填用户目标岗位到职业数据，userId: {}, jobId: {}", userId, targetJob.getId());
            return;
        }

        UserCareerData insertData = UserCareerData.builder()
                .userId(userId)
                .targetJob(targetJob.getJobCategoryName())
                .targetJobId(targetJob.getId())
                .jobProfile(jobProfileJson)
                .matchSummary("{}")
                .marketTrends("[]")
                .skillRadar("{}")
                .actions("[]")
                .createTime(now)
                .updateTime(now)
                .build();
        userCareerDataMapper.insert(insertData);
        log.info("已创建用户职业数据并写入 RAG 匹配岗位，userId: {}, jobId: {}", userId, targetJob.getId());
    }

    private String buildTargetJobProfile(JobCategory targetJob) {
        try {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", targetJob.getId());
            profile.put("name", targetJob.getJobCategoryName());
            profile.put("level", targetJob.getJobLevel());
            profile.put("level_name", targetJob.getJobLevelName());
            profile.put("required_skills", parseJsonArray(targetJob.getRequiredSkills()));
            profile.put("job_description", targetJob.getJobDescription());
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException e) {
            log.warn("构建目标岗位画像失败，jobId: {}", targetJob.getId(), e);
            return "{}";
        }
    }

    private List<Object> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class));
        } catch (JsonProcessingException e) {
            log.warn("解析 JSON 数组失败，content: {}", json, e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (JsonProcessingException e) {
            log.warn("解析岗位画像 JSON 对象失败", e);
            return Collections.emptyMap();
        }
    }
}
