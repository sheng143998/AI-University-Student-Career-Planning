package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.UserProfileMapper;
import com.itsheng.service.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 职业地图服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private final JobCategoryMapper jobCategoryMapper;
    private final ObjectMapper objectMapper;
    private final UserProfileMapper userProfileMapper;
    private final ResumeMapper resumeMapper;
    private final ChatClient chatClient;

    /**
     * 级别排序映射
     */
    private static final List<String> LEVEL_ORDER = List.of("INTERNSHIP", "JUNIOR", "MID", "SENIOR");

    @Override
    public RoadmapSearchResultVO searchNodes(String keyword, Integer limit) {
        List<JobCategory> jobs = jobCategoryMapper.searchByKeyword(keyword, limit);

        List<RoadmapSearchItemVO> items = new ArrayList<>();
        for (JobCategory job : jobs) {
            RoadmapSearchItemVO item = new RoadmapSearchItemVO();
            item.setId(String.valueOf(job.getId()));
            // 提取基础类别编码（去掉级别后缀）
            String baseCode = extractBaseCategoryCode(job.getJobCategoryCode());
            item.setCategoryCode(baseCode);
            item.setTitle(job.getJobCategoryName());
            item.setSubtitle(job.getJobLevelName() != null ? job.getJobLevelName() : "");
            item.setVariant("primary".equals(getVariantByLevel(job.getJobLevel())) ? "primary" : "neutral");
            item.setTags(List.of(job.getJobLevelName() != null ? job.getJobLevelName() : ""));
            item.setHasVerticalPaths(true);
            item.setHasLateralPaths(false);
            items.add(item);
        }

        RoadmapSearchResultVO result = new RoadmapSearchResultVO();
        result.setItems(items);
        return result;
    }

    /**
     * 提取基础类别编码（去掉级别后缀）
     */
    private String extractBaseCategoryCode(String code) {
        if (code == null) return "";
        return code.replaceAll("_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "");
    }

    @Override
    public RoadmapGraphVO getGraph(String categoryCode, String mode) {
        log.info("获取地图图谱: categoryCode={}, mode={}", categoryCode, mode);
        RoadmapGraphVO graph = new RoadmapGraphVO();
        graph.setMode(mode);

        if (categoryCode == null || categoryCode.isEmpty()) {
            categoryCode = jobCategoryMapper.selectRandomCategoryCode();
            log.info("随机选择的 categoryCode: {}", categoryCode);
            if (categoryCode == null || categoryCode.isEmpty()) {
                log.warn("无法获取有效的 categoryCode，返回空图谱");
                graph.setNodes(new ArrayList<>());
                graph.setPaths(new ArrayList<>());
                return graph;
            }
        }

        // 获取垂直晋升路径（同一类别编码的所有级别）
        List<JobCategory> jobs = jobCategoryMapper.selectVerticalPathByCategoryCode(categoryCode);
        log.info("查询到 {} 条垂直路径记录, categoryCode={}", jobs.size(), categoryCode);

        List<RoadmapNodeVO> nodes = new ArrayList<>();
        List<RoadmapPathVO> paths = new ArrayList<>();

        // 计算节点位置
        int baseX = 100;
        int baseY = 50;
        int stepX = 150;
        int stepY = 100;

        for (int i = 0; i < jobs.size(); i++) {
            JobCategory job = jobs.get(i);

            RoadmapNodeVO node = new RoadmapNodeVO();
            node.setId(String.valueOf(job.getId()));
            node.setTitle(job.getJobCategoryName());
            node.setLabel(job.getJobCategoryName());
            node.setSubtitle(job.getJobLevelName() != null ? job.getJobLevelName() : "");
            node.setSubLabel(job.getJobLevelName() != null ? job.getJobLevelName() : "");
            node.setKind(i == 0 ? "core" : "secondary");
            node.setVariant(getVariantByLevel(job.getJobLevel()));
            node.setTags(List.of(job.getJobLevelName() != null ? job.getJobLevelName() : ""));
            node.setX(baseX + i * stepX);
            node.setY(baseY + (i % 2) * stepY);

            nodes.add(node);

            // 创建与前一个节点的连线（垂直晋升路径）
            if (i > 0) {
                RoadmapPathVO path = new RoadmapPathVO();
                path.setFrom(String.valueOf(jobs.get(i - 1).getId()));
                path.setTo(String.valueOf(job.getId()));
                path.setVariant("primary");
                path.setEdgeType("vertical");
                path.setDifficulty(2);
                path.setAvgTimeMonths(24);
                path.setSuccessRate(0.75);
                paths.add(path);
            }
        }

        graph.setNodes(nodes);
        graph.setPaths(paths);
        return graph;
    }

    @Override
    public RoadmapNodeDetailVO getNodeDetail(Long id) {
        JobCategory job = jobCategoryMapper.selectById(id);
        if (job == null) {
            return null;
        }

        RoadmapNodeDetailVO detail = new RoadmapNodeDetailVO();
        detail.setId(String.valueOf(job.getId()));
        detail.setTitle(job.getJobCategoryName());
        detail.setSummary(job.getJobDescription());
        detail.setLevel(job.getJobLevel());
        detail.setLevelName(job.getJobLevelName());

        // 薪资范围
        if (job.getMinSalary() != null && job.getMaxSalary() != null) {
            detail.setSalaryRange(job.getMinSalary() + "-" + job.getMaxSalary() + " " +
                    (job.getSalaryUnit() != null ? job.getSalaryUnit() : "K"));
        }

        // 经验要求
        if (job.getRequiredExperienceYears() != null) {
            detail.setExperienceYears(job.getRequiredExperienceYears() + "年");
        }

        // 解析技能要求
        detail.setRequirements(parseJsonToList(job.getRequiredSkills()));
        detail.setRecommendedSkills(new ArrayList<>());

        return detail;
    }

    /**
     * 根据级别获取变体
     */
    private String getVariantByLevel(String level) {
        if (level == null) return "neutral";
        return "INTERNSHIP".equals(level) || "JUNIOR".equals(level) ? "primary" : "neutral";
    }

    /**
     * 获取个性化职业路径推荐
     */
    @Override
    public CareerPathRecommendationVO getPersonalizedRecommendations() {
        Long userId = BaseContext.getUserId();
        log.info("获取用户 {} 的个性化职业路径推荐", userId);

        // 1. 获取用户简历分析结果
        ResumeParsedData resumeData = getUserResumeData(userId);
        String currentJob = resumeData != null ? resumeData.getCurrentRole() : "";
        List<String> userSkills = resumeData != null ? resumeData.getSkills() : new ArrayList<>();

        if (currentJob == null || currentJob.isEmpty()) {
            currentJob = "未设置当前岗位";
        }

        // 2. 获取所有岗位数据用于匹配
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();

        // 3. 找到与用户当前岗位最匹配的垂直晋升路径
        CareerPathRecommendationVO.VerticalPathRecommendationVO verticalPath =
                findBestMatchingVerticalPath(currentJob, userSkills, allJobs);

        // 4. 调用大模型RAG生成横向换岗推荐（不少于2条）
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths =
                generateLateralPathRecommendationsRAG(currentJob, userSkills, resumeData, allJobs);

        return CareerPathRecommendationVO.builder()
                .currentJob(currentJob)
                .verticalPath(verticalPath)
                .lateralPaths(lateralPaths)
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }

    /**
     * 获取用户简历解析数据
     */
    private ResumeParsedData getUserResumeData(Long userId) {
        try {
            String parsedDataJson = userProfileMapper.selectLatestParsedDataByUserId(userId);
            if (parsedDataJson != null && !parsedDataJson.isEmpty() && !"{}".equals(parsedDataJson)) {
                return objectMapper.readValue(parsedDataJson, ResumeParsedData.class);
            }
        } catch (Exception e) {
            log.warn("获取用户 {} 简历数据失败: {}", userId, e.getMessage());
        }
        return null;
    }

    /**
     * 找到最匹配的垂直晋升路径
     */
    private CareerPathRecommendationVO.VerticalPathRecommendationVO findBestMatchingVerticalPath(
            String currentJob, List<String> userSkills, List<JobCategory> allJobs) {

        // 按类别编码分组
        Map<String, List<JobCategory>> jobsByCategory = allJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        // 计算每个类别与用户当前岗位的相似度
        Map<String, BigDecimal> similarityScores = new HashMap<>();
        Map<String, JobCategory> bestMatchJobByCategory = new HashMap<>();

        for (Map.Entry<String, List<JobCategory>> entry : jobsByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            List<JobCategory> categoryJobs = entry.getValue();

            BigDecimal maxSimilarity = BigDecimal.ZERO;
            JobCategory bestMatch = null;

            for (JobCategory job : categoryJobs) {
                BigDecimal similarity = calculateJobSimilarity(currentJob, userSkills, job);
                if (similarity.compareTo(maxSimilarity) > 0) {
                    maxSimilarity = similarity;
                    bestMatch = job;
                }
            }

            similarityScores.put(categoryCode, maxSimilarity);
            bestMatchJobByCategory.put(categoryCode, bestMatch);
        }

        // 找到相似度最高的类别
        String bestCategory = similarityScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        if (bestCategory.isEmpty() || similarityScores.get(bestCategory).compareTo(new BigDecimal("0.1")) < 0) {
            // 如果没有找到匹配的，返回随机一个
            bestCategory = jobsByCategory.keySet().iterator().next();
        }

        // 构建垂直路径节点
        List<JobCategory> verticalJobs = jobsByCategory.get(bestCategory);
        verticalJobs.sort(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())));

        List<CareerPathRecommendationVO.PathNodeVO> nodes = new ArrayList<>();
        int currentLevelIndex = -1;

        for (int i = 0; i < verticalJobs.size(); i++) {
            JobCategory job = verticalJobs.get(i);
            boolean isCurrentLevel = isJobMatch(currentJob, job);
            if (isCurrentLevel) {
                currentLevelIndex = i;
            }

            nodes.add(CareerPathRecommendationVO.PathNodeVO.builder()
                    .id(String.valueOf(job.getId()))
                    .title(job.getJobCategoryName())
                    .levelName(job.getJobLevelName())
                    .salaryRange(formatSalaryRange(job))
                    .skills(parseJsonToList(job.getRequiredSkills()))
                    .isCurrentLevel(isCurrentLevel)
                    .build());
        }

        return CareerPathRecommendationVO.VerticalPathRecommendationVO.builder()
                .categoryCode(bestCategory)
                .matchedJobName(bestMatchJobByCategory.get(bestCategory).getJobCategoryName())
                .similarityScore(similarityScores.get(bestCategory).setScale(2, RoundingMode.HALF_UP))
                .nodes(nodes)
                .currentLevelIndex(currentLevelIndex >= 0 ? currentLevelIndex : 0)
                .estimatedMonthsToNext(calculateEstimatedMonths(currentLevelIndex, verticalJobs.size()))
                .build();
    }

    /**
     * 计算岗位相似度
     */
    private BigDecimal calculateJobSimilarity(String currentJob, List<String> userSkills, JobCategory job) {
        double score = 0.0;

        // 1. 名称匹配（权重 0.6）
        String jobName = job.getJobCategoryName().toLowerCase();
        String current = currentJob.toLowerCase();

        if (jobName.contains(current) || current.contains(jobName)) {
            score += 0.6;
        } else {
            // 部分匹配
            String[] currentWords = current.split("\\s+|_");
            for (String word : currentWords) {
                if (word.length() > 2 && jobName.contains(word)) {
                    score += 0.15;
                }
            }
        }

        // 2. 技能匹配（权重 0.4）
        List<String> jobSkills = parseJsonToList(job.getRequiredSkills());
        if (!userSkills.isEmpty() && !jobSkills.isEmpty()) {
            long matchedSkills = userSkills.stream()
                    .filter(userSkill -> jobSkills.stream()
                            .anyMatch(jobSkill -> jobSkill.toLowerCase().contains(userSkill.toLowerCase())
                                    || userSkill.toLowerCase().contains(jobSkill.toLowerCase())))
                    .count();
            double skillScore = (double) matchedSkills / jobSkills.size() * 0.4;
            score += skillScore;
        }

        return new BigDecimal(Math.min(score, 1.0)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断是否为当前岗位
     */
    private boolean isJobMatch(String currentJob, JobCategory job) {
        String jobName = job.getJobCategoryName().toLowerCase();
        String current = currentJob.toLowerCase();
        return jobName.contains(current) || current.contains(jobName);
    }

    /**
     * 格式化薪资范围
     */
    private String formatSalaryRange(JobCategory job) {
        if (job.getMinSalary() != null && job.getMaxSalary() != null) {
            return job.getMinSalary() + "-" + job.getMaxSalary() + " " +
                    (job.getSalaryUnit() != null ? job.getSalaryUnit() : "K/月");
        }
        return "面议";
    }

    /**
     * 计算预计晋升月数
     */
    private Integer calculateEstimatedMonths(int currentLevelIndex, int totalLevels) {
        if (currentLevelIndex < 0 || currentLevelIndex >= totalLevels - 1) {
            return 0;
        }
        // 每级约需 12-24 个月
        return 12 + (totalLevels - currentLevelIndex - 1) * 6;
    }

    /**
     * 使用大模型RAG生成横向换岗推荐
     */
    private List<CareerPathRecommendationVO.LateralPathRecommendationVO> generateLateralPathRecommendationsRAG(
            String currentJob, List<String> userSkills, ResumeParsedData resumeData, List<JobCategory> allJobs) {

        List<CareerPathRecommendationVO.LateralPathRecommendationVO> recommendations = new ArrayList<>();

        try {
            // 构建RAG提示词
            StringBuilder jobDatabase = new StringBuilder();
            jobDatabase.append("# 职业数据库\n\n");

            // 按类别分组，每类只取一个代表性岗位
            Map<String, JobCategory> representativeJobs = new HashMap<>();
            for (JobCategory job : allJobs) {
                String baseCode = extractBaseCategoryCode(job.getJobCategoryCode());
                if (!representativeJobs.containsKey(baseCode)) {
                    representativeJobs.put(baseCode, job);
                }
            }

            representativeJobs.values().forEach(job -> {
                jobDatabase.append(String.format("- %s: %s\n  级别: %s\n  技能要求: %s\n\n",
                        extractBaseCategoryCode(job.getJobCategoryCode()),
                        job.getJobCategoryName(),
                        job.getJobLevelName(),
                        job.getRequiredSkills()));
            });

            // 构建用户画像
            String userProfile = String.format("""
                            # 用户画像
                            
                            当前岗位: %s
                            技能列表: %s
                            工作年限: %s
                            目标岗位: %s
                            """,
                    currentJob,
                    String.join(", ", userSkills != null ? userSkills : new ArrayList<>()),
                    resumeData != null && resumeData.getExperienceYears() != null ? resumeData.getExperienceYears() + "年" : "未知",
                    resumeData != null && resumeData.getTargetRole() != null ? resumeData.getTargetRole() : "未设置");

            String systemPrompt = """
                    你是一位资深的职业规划顾问。基于提供的职业数据库和用户画像，为用户推荐2-3条可行的横向换岗（转型）路径。
                    
                    要求：
                    1. 推荐的路径必须来自提供的职业数据库
                    2. 考虑用户当前技能与目标岗位的匹配度
                    3. 评估转型难度（1-5分，5分最难）
                    4. 列出需要补充的技能
                    5. 给出AI推荐理由
                    
                    请严格按以下JSON格式返回，不要包含任何markdown标记：
                    {
                        "recommendations": [
                            {
                                "targetCategoryCode": "目标类别编码",
                                "targetJobName": "目标岗位名称",
                                "matchScore": 0.85,
                                "transitionDifficulty": 3,
                                "estimatedMonths": 12,
                                "requiredSkills": ["需要补充的技能1", "技能2"],
                                "possessedSkills": ["用户已有的技能1", "技能2"],
                                "aiRecommendationReason": "推荐理由"
                            }
                        ]
                    }
                    """;

            String userPrompt = jobDatabase.toString() + "\n" + userProfile + "\n\n请基于以上信息推荐横向换岗路径。";

            // 调用大模型
            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .chatResponse();

            String aiResponse = response.getResult().getOutput().getText();
            log.info("AI换岗推荐响应: {}", aiResponse);

            // 解析AI响应
            JsonNode rootNode = objectMapper.readTree(cleanJsonResponse(aiResponse));
            JsonNode recommendationsNode = rootNode.get("recommendations");

            if (recommendationsNode != null && recommendationsNode.isArray()) {
                for (JsonNode recNode : recommendationsNode) {
                    String categoryCode = getStringValue(recNode, "targetCategoryCode", "");
                    String jobName = getStringValue(recNode, "targetJobName", "");

                    // 查找对应岗位的详细信息
                    JobCategory targetJob = representativeJobs.get(categoryCode);
                    if (targetJob == null) {
                        // 从allJobs中查找
                        targetJob = allJobs.stream()
                                .filter(j -> extractBaseCategoryCode(j.getJobCategoryCode()).equals(categoryCode))
                                .findFirst()
                                .orElse(null);
                    }

                    if (targetJob != null) {
                        // 获取该类别下的所有级别作为路径节点
                        String finalCategoryCode = categoryCode;
                        List<JobCategory> pathJobs = allJobs.stream()
                                .filter(j -> extractBaseCategoryCode(j.getJobCategoryCode()).equals(finalCategoryCode))
                                .sorted(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())))
                                .collect(Collectors.toList());

                        List<CareerPathRecommendationVO.PathNodeVO> pathNodes = pathJobs.stream()
                                .map(j -> CareerPathRecommendationVO.PathNodeVO.builder()
                                        .id(String.valueOf(j.getId()))
                                        .title(j.getJobCategoryName())
                                        .levelName(j.getJobLevelName())
                                        .salaryRange(formatSalaryRange(j))
                                        .skills(parseJsonToList(j.getRequiredSkills()))
                                        .isCurrentLevel(false)
                                        .build())
                                .collect(Collectors.toList());

                        recommendations.add(CareerPathRecommendationVO.LateralPathRecommendationVO.builder()
                                .targetJobId(targetJob.getId())
                                .targetJobName(jobName)
                                .targetCategoryCode(categoryCode)
                                .matchScore(new BigDecimal(getStringValue(recNode, "matchScore", "0.7")))
                                .transitionDifficulty(recNode.get("transitionDifficulty").asInt(3))
                                .estimatedMonths(recNode.get("estimatedMonths").asInt(12))
                                .requiredSkills(parseJsonArrayNode(recNode.get("requiredSkills")))
                                .possessedSkills(parseJsonArrayNode(recNode.get("possessedSkills")))
                                .aiRecommendationReason(getStringValue(recNode, "aiRecommendationReason", "基于您的技能背景推荐"))
                                .pathNodes(pathNodes)
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("生成横向换岗推荐失败: {}", e.getMessage(), e);
        }

        // 如果AI推荐不足2条，补充基于相似度的推荐
        if (recommendations.size() < 2) {
            recommendations.addAll(generateFallbackLateralRecommendations(currentJob, userSkills, allJobs, recommendations));
        }

        return recommendations;
    }

    /**
     * 生成备选横向推荐（当AI推荐不足时）
     */
    private List<CareerPathRecommendationVO.LateralPathRecommendationVO> generateFallbackLateralRecommendations(
            String currentJob, List<String> userSkills, List<JobCategory> allJobs,
            List<CareerPathRecommendationVO.LateralPathRecommendationVO> existing) {

        List<CareerPathRecommendationVO.LateralPathRecommendationVO> fallback = new ArrayList<>();
        Set<String> existingCategories = existing.stream()
                .map(CareerPathRecommendationVO.LateralPathRecommendationVO::getTargetCategoryCode)
                .collect(Collectors.toSet());

        // 按类别分组，计算每个类别的平均相似度
        Map<String, List<JobCategory>> jobsByCategory = allJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        List<Map.Entry<String, BigDecimal>> categoryScores = new ArrayList<>();
        for (Map.Entry<String, List<JobCategory>> entry : jobsByCategory.entrySet()) {
            if (existingCategories.contains(entry.getKey())) continue;

            BigDecimal avgScore = entry.getValue().stream()
                    .map(j -> calculateJobSimilarity(currentJob, userSkills, j))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(entry.getValue().size()), 2, RoundingMode.HALF_UP);

            categoryScores.add(new AbstractMap.SimpleEntry<>(entry.getKey(), avgScore));
        }

        // 选择相似度中等的类别（排除已选）
        categoryScores.sort(Map.Entry.comparingByValue());
        int startIdx = Math.max(0, categoryScores.size() / 2 - 1);

        for (int i = startIdx; i < Math.min(startIdx + (2 - existing.size()), categoryScores.size()); i++) {
            String categoryCode = categoryScores.get(i).getKey();
            List<JobCategory> categoryJobs = jobsByCategory.get(categoryCode);
            if (categoryJobs.isEmpty()) continue;

            JobCategory representative = categoryJobs.get(0);
            List<CareerPathRecommendationVO.PathNodeVO> pathNodes = categoryJobs.stream()
                    .sorted(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())))
                    .map(j -> CareerPathRecommendationVO.PathNodeVO.builder()
                            .id(String.valueOf(j.getId()))
                            .title(j.getJobCategoryName())
                            .levelName(j.getJobLevelName())
                            .salaryRange(formatSalaryRange(j))
                            .skills(parseJsonToList(j.getRequiredSkills()))
                            .isCurrentLevel(false)
                            .build())
                    .collect(Collectors.toList());

            fallback.add(CareerPathRecommendationVO.LateralPathRecommendationVO.builder()
                    .targetJobId(representative.getId())
                    .targetJobName(representative.getJobCategoryName())
                    .targetCategoryCode(categoryCode)
                    .matchScore(categoryScores.get(i).getValue())
                    .transitionDifficulty(3)
                    .estimatedMonths(12)
                    .requiredSkills(parseJsonToList(representative.getRequiredSkills()))
                    .possessedSkills(userSkills != null ? userSkills : new ArrayList<>())
                    .aiRecommendationReason("基于您的技能背景，该岗位与您的经验有一定关联，转型难度适中。")
                    .pathNodes(pathNodes)
                    .build());
        }

        return fallback;
    }

    /**
     * 从JsonNode安全获取字符串值
     */
    private String getStringValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText(defaultValue);
        }
        return defaultValue;
    }

    /**
     * 解析JSON数组节点
     */
    private List<String> parseJsonArrayNode(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> result.add(n.asText()));
        }
        return result;
    }

    /**
     * 清理AI返回的JSON字符串
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 移除markdown代码块标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // 找到第一个 { 和最后一个 } 之间的内容
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');

        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }

        return cleaned.trim();
    }

    /**
     * 解析 JSON 字符串为列表
     */
    private List<String> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 失败: {}", json, e);
            return new ArrayList<>();
        }
    }
}