package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.BaseException;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.client.PythonRoadmapRagClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.UserProfileMapper;
import com.itsheng.service.service.RoadmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final PythonRoadmapRagClient pythonRoadmapRagClient;
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;

    private static final String REDIS_KEY_PREFIX = "roadmap:user:current_job:";
    private static final String PERSONALIZED_RECOMMENDATIONS_CACHE = "roadmap:recommendations:personalized";
    private static final List<String> SENSITIVE_PATTERNS = List.of(
            "1[3-9]\\d{9}",
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}",
            "(?i)(api[_-]?key|secret|token)\\s*[:=]\\s*[A-Za-z0-9_\\-]{8,}",
            "(?i)\\bsk[-_][A-Za-z0-9_\\-]{8,}\\b"
    );

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
    @Cacheable(cacheNames = "roadmap:graph", key = "(#categoryCode == null || #categoryCode.isEmpty()) ? ('global:' + #mode) : (#categoryCode + ':' + #mode)")
    public RoadmapGraphVO getGraph(String categoryCode, String mode) {
        log.info("获取地图图谱: categoryCode={}, mode={}", categoryCode, mode);
        RoadmapGraphVO graph = new RoadmapGraphVO();
        graph.setMode(mode);

        // 如果没有指定类别，获取全局视图（所有类别）
        if (categoryCode == null || categoryCode.isEmpty()) {
            return getGlobalGraph(mode);
        }

        // 获取指定类别的聚焦视图
        return getFocusedGraph(categoryCode, mode);
    }

    /**
     * 获取全局职业地图（所有类别）
     */
    private RoadmapGraphVO getGlobalGraph(String mode) {
        log.info("获取全局职业地图");
        RoadmapGraphVO graph = new RoadmapGraphVO();
        graph.setMode(mode);
        graph.setViewType("global");

        // 获取所有岗位
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        log.info("查询到 {} 条岗位记录", allJobs.size());

        // 按类别分组
        Map<String, List<JobCategory>> jobsByCategory = allJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        List<RoadmapNodeVO> nodes = new ArrayList<>();
        List<RoadmapPathVO> paths = new ArrayList<>();

        // 为每个类别创建一个垂直路径列
        int categoryIndex = 0;
        int baseX = 80;
        int baseY = 80;
        int categorySpacingX = 200;
        int levelSpacingY = 120;

        for (Map.Entry<String, List<JobCategory>> entry : jobsByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            List<JobCategory> categoryJobs = entry.getValue();
            
            // 按级别排序
            categoryJobs.sort(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())));

            // 计算该列的基准位置
            int colX = baseX + categoryIndex * categorySpacingX;
            int colY = baseY;

            // 为每个级别创建节点
            for (int i = 0; i < categoryJobs.size(); i++) {
                JobCategory job = categoryJobs.get(i);

                RoadmapNodeVO node = new RoadmapNodeVO();
                node.setId(String.valueOf(job.getId()));
                node.setTitle(job.getJobCategoryName());
                node.setLabel(job.getJobCategoryName());
                node.setSubtitle(job.getJobLevelName());
                node.setSubLabel(job.getJobLevelName());
                node.setKind(i == 0 ? "core" : "secondary");
                node.setVariant(getVariantByLevel(job.getJobLevel()));
                node.setCategoryCode(categoryCode);
                node.setLevel(job.getJobLevel());
                
                // 计算位置 - 垂直排列
                node.setX(colX);
                node.setY(colY + i * levelSpacingY);

                nodes.add(node);

                // 创建与前一个节点的连线（垂直晋升路径 - 虚线）
                if (i > 0) {
                    RoadmapPathVO path = new RoadmapPathVO();
                    path.setFrom(String.valueOf(categoryJobs.get(i - 1).getId()));
                    path.setTo(String.valueOf(job.getId()));
                    path.setVariant("primary");
                    path.setEdgeType("vertical");
                    path.setLineStyle("dashed");  // 垂直路径用虚线
                    path.setDifficulty(2);
                    path.setAvgTimeMonths(24);
                    path.setSuccessRate(0.75);
                    paths.add(path);
                }
            }

            categoryIndex++;
        }

        // 添加横向换岗路径（基于技能相似度）
        addLateralPaths(nodes, paths);

        graph.setNodes(nodes);
        graph.setPaths(paths);
        return graph;
    }

    /**
     * 获取聚焦视图（指定类别）
     */
    private RoadmapGraphVO getFocusedGraph(String categoryCode, String mode) {
        log.info("获取聚焦视图: categoryCode={}", categoryCode);
        RoadmapGraphVO graph = new RoadmapGraphVO();
        graph.setMode(mode);
        graph.setViewType("focused");
        graph.setCenterCategoryCode(categoryCode);

        // 获取指定类别的垂直路径
        List<JobCategory> centerJobs = jobCategoryMapper.selectVerticalPathByCategoryCode(categoryCode);
        
        List<RoadmapNodeVO> nodes = new ArrayList<>();
        List<RoadmapPathVO> paths = new ArrayList<>();

        // 中心位置
        int centerX = 400;
        int centerY = 300;
        int levelSpacingY = 120;
        int lateralSpacingX = 250;

        // 添加中心类别的节点（垂直排列）
        int centerLevelCount = centerJobs.size();
        int startY = centerY - (centerLevelCount - 1) * levelSpacingY / 2;

        for (int i = 0; i < centerJobs.size(); i++) {
            JobCategory job = centerJobs.get(i);
            RoadmapNodeVO node = new RoadmapNodeVO();
            node.setId(String.valueOf(job.getId()));
            node.setTitle(job.getJobCategoryName());
            node.setLabel(job.getJobCategoryName());
            node.setSubtitle(job.getJobLevelName());
            node.setSubLabel(job.getJobLevelName());
            node.setKind(i == centerJobs.size() / 2 ? "core" : "secondary");
            node.setVariant("primary");
            node.setCategoryCode(categoryCode);
            node.setLevel(job.getJobLevel());
            node.setX(centerX);
            node.setY(startY + i * levelSpacingY);
            nodes.add(node);

            // 垂直路径（虚线）
            if (i > 0) {
                RoadmapPathVO path = new RoadmapPathVO();
                path.setFrom(String.valueOf(centerJobs.get(i - 1).getId()));
                path.setTo(String.valueOf(job.getId()));
                path.setVariant("primary");
                path.setEdgeType("vertical");
                path.setLineStyle("dashed");
                paths.add(path);
            }
        }

        // 添加相关的横向换岗路径
        addRelatedLateralPaths(nodes, paths, centerJobs, lateralSpacingX, levelSpacingY);

        graph.setNodes(nodes);
        graph.setPaths(paths);
        return graph;
    }

    /**
     * 添加横向换岗路径（全局视图）
     */
    private void addLateralPaths(List<RoadmapNodeVO> nodes, List<RoadmapPathVO> paths) {
        // 基于技能相似度创建横向连接
        Map<String, RoadmapNodeVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(RoadmapNodeVO::getId, n -> n));

        // 获取所有节点对应的岗位数据
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        Map<Long, JobCategory> jobMap = allJobs.stream()
                .collect(Collectors.toMap(JobCategory::getId, j -> j));

        // 为中级和高级岗位添加横向连接
        for (RoadmapNodeVO node : nodes) {
            if (!"MID".equals(node.getLevel()) && !"SENIOR".equals(node.getLevel())) {
                continue;
            }

            JobCategory job = jobMap.get(Long.parseLong(node.getId()));
            if (job == null) continue;

            List<String> jobSkills = parseJsonToList(job.getRequiredSkills());
            if (jobSkills.isEmpty()) continue;

            // 寻找技能相似的其他类别节点
            for (RoadmapNodeVO otherNode : nodes) {
                if (node.getId().equals(otherNode.getId())) continue;
                if (node.getCategoryCode().equals(otherNode.getCategoryCode())) continue; // 同一类别不连接
                if (!"MID".equals(otherNode.getLevel()) && !"SENIOR".equals(otherNode.getLevel())) continue;

                JobCategory otherJob = jobMap.get(Long.parseLong(otherNode.getId()));
                if (otherJob == null) continue;

                List<String> otherSkills = parseJsonToList(otherJob.getRequiredSkills());
                if (otherSkills.isEmpty()) continue;

                // 计算技能相似度
                long matchedSkills = jobSkills.stream()
                        .filter(s -> otherSkills.stream()
                                .anyMatch(os -> os.toLowerCase().contains(s.toLowerCase()) 
                                        || s.toLowerCase().contains(os.toLowerCase())))
                        .count();
                
                double similarity = (double) matchedSkills / Math.max(jobSkills.size(), otherSkills.size());
                
                // 相似度超过阈值则添加横向路径
                if (similarity > 0.3) {
                    RoadmapPathVO path = new RoadmapPathVO();
                    path.setFrom(node.getId());
                    path.setTo(otherNode.getId());
                    path.setVariant("secondary");
                    path.setEdgeType("lateral");
                    path.setLineStyle("solid");  // 横向路径用实线
                    path.setDifficulty((int) (5 - similarity * 5));  // 相似度越高难度越低
                    path.setSuccessRate(similarity);
                    path.setAvgTimeMonths((int) (12 + (1 - similarity) * 12));
                    
                    // 检查是否已存在
                    boolean exists = paths.stream().anyMatch(p -> 
                        (p.getFrom().equals(path.getFrom()) && p.getTo().equals(path.getTo())) ||
                        (p.getFrom().equals(path.getTo()) && p.getTo().equals(path.getFrom()))
                    );
                    
                    if (!exists) {
                        paths.add(path);
                    }
                }
            }
        }
    }

    /**
     * 添加相关的横向换岗路径（聚焦视图）
     */
    private void addRelatedLateralPaths(List<RoadmapNodeVO> nodes, List<RoadmapPathVO> paths, 
            List<JobCategory> centerJobs, int lateralSpacingX, int levelSpacingY) {
        
        // 获取所有其他类别的岗位
        List<JobCategory> allOtherJobs = jobCategoryMapper.selectAll().stream()
                .filter(j -> !extractBaseCategoryCode(j.getJobCategoryCode())
                        .equals(extractBaseCategoryCode(centerJobs.get(0).getJobCategoryCode())))
                .collect(Collectors.toList());

        if (allOtherJobs.isEmpty()) return;

        // 为中心岗位的每个级别找到最相似的横向岗位
        Map<String, List<JobCategory>> otherJobsByCategory = allOtherJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        int leftOffset = -1;
        int rightOffset = 1;

        for (JobCategory centerJob : centerJobs) {
            if (!"MID".equals(centerJob.getJobLevel()) && !"SENIOR".equals(centerJob.getJobLevel())) {
                continue;
            }

            List<String> centerSkills = parseJsonToList(centerJob.getRequiredSkills());
            if (centerSkills.isEmpty()) continue;

            // 找到最相似的类别
            String bestCategory = null;
            double bestSimilarity = 0;

            for (Map.Entry<String, List<JobCategory>> entry : otherJobsByCategory.entrySet()) {
                List<JobCategory> otherJobs = entry.getValue();
                
                // 计算平均相似度
                double totalSim = 0;
                int count = 0;
                for (JobCategory other : otherJobs) {
                    List<String> otherSkills = parseJsonToList(other.getRequiredSkills());
                    if (otherSkills.isEmpty()) continue;
                    
                    long matched = centerSkills.stream()
                            .filter(s -> otherSkills.stream()
                                    .anyMatch(os -> os.toLowerCase().contains(s.toLowerCase())
                                            || s.toLowerCase().contains(os.toLowerCase())))
                            .count();
                    
                    double sim = (double) matched / Math.max(centerSkills.size(), otherSkills.size());
                    totalSim += sim;
                    count++;
                }
                
                if (count > 0) {
                    double avgSim = totalSim / count;
                    if (avgSim > bestSimilarity && avgSim > 0.2) {
                        bestSimilarity = avgSim;
                        bestCategory = entry.getKey();
                    }
                }
            }

            if (bestCategory != null) {
                List<JobCategory> similarJobs = otherJobsByCategory.get(bestCategory);
                similarJobs.sort(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())));

                // 在左侧或右侧添加这些节点
                int offsetX = (leftOffset < 0 ? leftOffset : rightOffset) * lateralSpacingX;
                int baseY = nodes.stream()
                        .filter(n -> String.valueOf(centerJob.getId()).equals(n.getId()))
                        .findFirst()
                        .map(RoadmapNodeVO::getY)
                        .orElse(300);

                for (int i = 0; i < similarJobs.size(); i++) {
                    JobCategory job = similarJobs.get(i);
                    
                    RoadmapNodeVO node = new RoadmapNodeVO();
                    node.setId(String.valueOf(job.getId()));
                    node.setTitle(job.getJobCategoryName());
                    node.setLabel(job.getJobCategoryName());
                    node.setSubtitle(job.getJobLevelName());
                    node.setSubLabel(job.getJobLevelName());
                    node.setKind("secondary");
                    node.setVariant("neutral");
                    node.setCategoryCode(bestCategory);
                    node.setLevel(job.getJobLevel());
                    node.setX(400 + offsetX);
                    node.setY(baseY + (i - similarJobs.size() / 2) * levelSpacingY);
                    nodes.add(node);
                }

                // 更新偏移
                if (leftOffset < 0) {
                    leftOffset = -leftOffset;
                } else {
                    rightOffset++;
                }
                
                // 添加横向连接
                for (RoadmapNodeVO centerNode : nodes) {
                    if (!String.valueOf(centerJob.getId()).equals(centerNode.getId())) continue;
                    
                    for (RoadmapNodeVO lateralNode : nodes) {
                        if (!bestCategory.equals(lateralNode.getCategoryCode())) continue;
                        if (!centerNode.getLevel().equals(lateralNode.getLevel())) continue;
                        
                        RoadmapPathVO path = new RoadmapPathVO();
                        path.setFrom(centerNode.getId());
                        path.setTo(lateralNode.getId());
                        path.setVariant("secondary");
                        path.setEdgeType("lateral");
                        path.setLineStyle("solid");
                        path.setDifficulty((int) (5 - bestSimilarity * 5));
                        path.setSuccessRate(bestSimilarity);
                        paths.add(path);
                    }
                }
            }
        }
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
    @Cacheable(cacheNames = PERSONALIZED_RECOMMENDATIONS_CACHE, key = "T(com.itsheng.common.context.BaseContext).getUserId()")
    public CareerPathRecommendationVO getPersonalizedRecommendations() {
        Long userId = BaseContext.getUserId();
        log.info("获取用户 {} 的个性化职业路径推荐", userId);

        // 1. 获取用户简历分析结果
        ResumeParsedData resumeData = getUserResumeData(userId);
        List<String> userSkills = resumeData != null ? resumeData.getSkills() : new ArrayList<>();

        // 2. 优先使用 Redis 中保存的岗位，如果没有则使用简历中的岗位
        String effectiveCurrentJob = getUserCurrentJob();
        if (effectiveCurrentJob == null || effectiveCurrentJob.isEmpty()) {
            effectiveCurrentJob = resumeData != null ? resumeData.getCurrentRole() : "";
        }
        if (effectiveCurrentJob == null || effectiveCurrentJob.isEmpty()) {
            effectiveCurrentJob = "未设置当前岗位";
        }

        // 3. 获取所有岗位数据用于匹配
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();

        // 4. 找到与用户当前岗位最匹配的垂直晋升路径
        CareerPathRecommendationVO.VerticalPathRecommendationVO verticalPath =
                findBestMatchingVerticalPath(effectiveCurrentJob, userSkills, allJobs);

        // 5. 调用 Python Roadmap-RAG 生成横向换岗推荐，失败时使用本地相似度降级
        RoadmapRagResult ragResult =
                generateLateralPathRecommendationsRAG(effectiveCurrentJob, userSkills, resumeData, allJobs);

        return CareerPathRecommendationVO.builder()
                .currentJob(effectiveCurrentJob)
                .verticalPath(verticalPath)
                .lateralPaths(ragResult.lateralPaths())
                .ragDiagnostics(ragResult.diagnostics())
                .generatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
    }

    @Override
    public void clearPersonalizedRecommendationsCache() {
        evictPersonalizedRecommendationsCache(BaseContext.getUserId(), false);
    }

    @Override
    public void saveUserCurrentJob(String currentJob) {
        Long userId = BaseContext.getUserId();
        String redisKey = REDIS_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(redisKey, currentJob);
            evictPersonalizedRecommendationsCache(userId, true);
            log.info("Saved user {} current job to Redis, currentJobPresent={}, currentJobLength={}",
                    userId, currentJob != null && !currentJob.isBlank(), currentJobLength(currentJob));
        } catch (Exception e) {
            log.error("Failed to save user {} current job to Redis/cache, currentJobPresent={}, currentJobLength={}: {}",
                    userId, currentJob != null && !currentJob.isBlank(), currentJobLength(currentJob), e.getMessage());
            throw new BaseException("保存当前岗位失败，请稍后重试", e);
        }
    }

    private void evictPersonalizedRecommendationsCache(Long userId, boolean failOnError) {
        if (userId == null) {
            return;
        }
        try {
            Cache cache = cacheManager.getCache(PERSONALIZED_RECOMMENDATIONS_CACHE);
            if (cache != null) {
                cache.evict(userId);
                log.info("Evicted personalized recommendations cache for user: {}", userId);
            } else if (failOnError) {
                throw new IllegalStateException("Personalized recommendations cache is not configured");
            } else {
                log.warn("Personalized recommendations cache {} is not configured", PERSONALIZED_RECOMMENDATIONS_CACHE);
            }
        } catch (Exception e) {
            log.warn("Failed to evict personalized recommendations cache for user {}: {}", userId, e.getMessage());
            if (failOnError) {
                throw new IllegalStateException("Failed to evict personalized recommendations cache", e);
            }
        }
    }

    private int currentJobLength(String currentJob) {
        return currentJob == null ? 0 : currentJob.length();
    }

    @Override
    public String getUserCurrentJob() {
        Long userId = BaseContext.getUserId();
        String redisKey = REDIS_KEY_PREFIX + userId;
        try {
            String currentJob = redisTemplate.opsForValue().get(redisKey);
            log.info("Retrieved user {} current job from Redis, currentJobPresent={}, currentJobLength={}",
                    userId, currentJob != null && !currentJob.isBlank(), currentJobLength(currentJob));
            return currentJob;
        } catch (Exception e) {
            log.error("Failed to get user current job from Redis: {}", e.getMessage());
            return null;
        }
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
     * Find the best matching vertical career path
     */
    private CareerPathRecommendationVO.VerticalPathRecommendationVO findBestMatchingVerticalPath(
            String currentJob, List<String> userSkills, List<JobCategory> allJobs) {

        // Group by category code
        Map<String, List<JobCategory>> jobsByCategory = allJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        // Calculate similarity for each category
        Map<String, BigDecimal> similarityScores = new HashMap<>();
        Map<String, JobCategory> bestMatchJobByCategory = new HashMap<>();

        for (Map.Entry<String, List<JobCategory>> entry : jobsByCategory.entrySet()) {
            String categoryCode = entry.getKey();
            List<JobCategory> categoryJobs = entry.getValue();

            BigDecimal maxSimilarity = BigDecimal.ZERO;
            JobCategory bestMatch = null;

            for (JobCategory job : categoryJobs) {
                BigDecimal similarity = calculateJobSimilarity(currentJob, userSkills, job);
                log.info("岗位 {} 与当前岗位相似度: {}, currentJobPresent={}, currentJobLength={}",
                        job.getJobCategoryName(), similarity,
                        currentJob != null && !currentJob.isBlank(), currentJobLength(currentJob));
                if (similarity.compareTo(maxSimilarity) > 0) {
                    maxSimilarity = similarity;
                    bestMatch = job;
                }
            }

            similarityScores.put(categoryCode, maxSimilarity);
            bestMatchJobByCategory.put(categoryCode, bestMatch);
            log.info("类别 {} 的最高相似度: {}, 匹配岗位: {}", categoryCode, maxSimilarity, bestMatch != null ? bestMatch.getJobCategoryName() : "null");
        }

        // Find category with highest similarity - always use the best match, no random fallback
        String bestCategory = similarityScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");

        log.info("最终选择的类别: {}, 相似度: {}", bestCategory, similarityScores.get(bestCategory));

        if (bestCategory.isEmpty() && !jobsByCategory.isEmpty()) {
            // Only fallback if no categories at all (should not happen)
            bestCategory = jobsByCategory.keySet().iterator().next();
            if (!bestMatchJobByCategory.containsKey(bestCategory)) {
                List<JobCategory> categoryJobs = jobsByCategory.get(bestCategory);
                if (categoryJobs != null && !categoryJobs.isEmpty()) {
                    bestMatchJobByCategory.put(bestCategory, categoryJobs.get(0));
                }
            }
        }

        // Build vertical path nodes
        List<JobCategory> verticalJobs = jobsByCategory.get(bestCategory);
        if (verticalJobs == null || verticalJobs.isEmpty()) {
            bestCategory = jobsByCategory.keySet().iterator().next();
            verticalJobs = jobsByCategory.get(bestCategory);
            if (!bestMatchJobByCategory.containsKey(bestCategory) && verticalJobs != null && !verticalJobs.isEmpty()) {
                bestMatchJobByCategory.put(bestCategory, verticalJobs.get(0));
            }
        }

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

        // Safe access to bestMatchJobByCategory with null check
        JobCategory matchedJob = bestMatchJobByCategory.get(bestCategory);
        String matchedJobName = matchedJob != null ? matchedJob.getJobCategoryName() : "Unknown Position";
        BigDecimal similarityScore = similarityScores.get(bestCategory);
        if (similarityScore == null) {
            similarityScore = BigDecimal.ZERO;
        }

        return CareerPathRecommendationVO.VerticalPathRecommendationVO.builder()
                .categoryCode(bestCategory)
                .matchedJobName(matchedJobName)
                .similarityScore(similarityScore.setScale(2, RoundingMode.HALF_UP))
                .nodes(nodes)
                .currentLevelIndex(currentLevelIndex >= 0 ? currentLevelIndex : 0)
                .estimatedMonthsToNext(calculateEstimatedMonths(currentLevelIndex, verticalJobs.size()))
                .build();
    }

    /**
     * Calculate job similarity - improved algorithm with keyword matching
     */
    private BigDecimal calculateJobSimilarity(String currentJob, List<String> userSkills, JobCategory job) {
        double score = 0.0;

        // 1. Name matching (weight 0.7)
        String jobName = job.getJobCategoryName().toLowerCase();
        String current = currentJob.toLowerCase();

        // Exact or contains match
        if (jobName.equals(current) || jobName.contains(current) || current.contains(jobName)) {
            score += 0.7;
        } else {
            // Keyword matching - extract important keywords
            List<String> currentKeywords = extractJobKeywords(current);
            List<String> jobKeywords = extractJobKeywords(jobName);

            // Calculate keyword overlap - require at least one meaningful keyword match
            long matchedKeywords = currentKeywords.stream()
                    .filter(kw -> jobKeywords.stream()
                            .anyMatch(jkw -> jkw.equals(kw) || jkw.contains(kw) || kw.contains(jkw)))
                    .count();

            if (!currentKeywords.isEmpty()) {
                double keywordScore = (double) matchedKeywords / currentKeywords.size() * 0.6;
                score += keywordScore;
            }

            // 如果没有匹配到任何有意义的关键词，大幅降低分数
            if (matchedKeywords == 0) {
                score = score * 0.05; // 降权到5%
            }
        }

        // 2. Skill matching (weight 0.3)
        List<String> jobSkills = parseJsonToList(job.getRequiredSkills());
        if (userSkills != null && !userSkills.isEmpty() && !jobSkills.isEmpty()) {
            long matchedSkills = userSkills.stream()
                    .filter(userSkill -> jobSkills.stream()
                            .anyMatch(jobSkill -> jobSkill.toLowerCase().contains(userSkill.toLowerCase())
                                    || userSkill.toLowerCase().contains(jobSkill.toLowerCase())))
                    .count();
            double skillScore = (double) matchedSkills / jobSkills.size() * 0.3;
            score += skillScore;
        }

        return new BigDecimal(Math.min(score, 1.0)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Extract important keywords from job name for matching
     */
    private List<String> extractJobKeywords(String jobName) {
        Set<String> techKeywords = Set.of(
                "java", "python", "javascript", "js", "ts", "typescript", "c++", "c#", "go", "rust",
                "frontend", "backend", "fullstack", "full-stack", "full stack",
                "web", "mobile", "ios", "android", "flutter", "react", "vue", "angular",
                "spring", "django", "node", "express",
                "data", "ai", "ml", "machine learning", "algorithm",
                "devops", "cloud", "aws", "azure", "kubernetes",
                "semiconductor", "chip", "hardware", "embedded",
                "manager", "director", "lead", "senior", "junior", "intern",
                "analyst", "designer", "product", "operation", "marketing",
                "test", "qa", "security", "database", "dba",
                "前端", "后端", "全栈", "移动端", "安卓",
                "数据", "人工智能", "机器学习", "算法",
                "运维", "云", "半导体", "芯片", "硬件", "嵌入式",
                "经理", "总监", "主管", "高级", "初级", "实习生",
                "分析师", "设计师", "产品", "运营", "市场",
                "测试", "安全", "数据库"
        );

        String lowerName = jobName.toLowerCase();
        List<String> keywords = new ArrayList<>();

        for (String keyword : techKeywords) {
            if (lowerName.contains(keyword)) {
                keywords.add(keyword);
            }
        }

        String[] words = lowerName.split("[\\s_\\-/]+");
        for (String word : words) {
            if (word.length() >= 2 && !keywords.contains(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * Check if job matches current job
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
     * 调用 Python Roadmap-RAG 生成横向换岗推荐
     */
    private RoadmapRagResult generateLateralPathRecommendationsRAG(
            String currentJob, List<String> userSkills, ResumeParsedData resumeData, List<JobCategory> allJobs) {
        try {
            Map<String, Object> payload = buildRoadmapRagPayload(currentJob, userSkills, resumeData, allJobs);
            JsonNode rootNode = pythonRoadmapRagClient.generatePersonalizedRecommendations(payload);
            RoadmapRagResult result = parseRoadmapRagResult(rootNode, allJobs);
            if (result.lateralPaths().size() >= 2) {
                return result;
            }
            if (!result.lateralPaths().isEmpty()) {
                List<CareerPathRecommendationVO.LateralPathRecommendationVO> supplemented =
                        new ArrayList<>(result.lateralPaths());
                supplemented.addAll(generateFallbackLateralRecommendations(currentJob, userSkills, allJobs, supplemented));
                if (supplemented.size() >= 2) {
                    Map<String, Object> diagnostics = new LinkedHashMap<>(result.diagnostics());
                    diagnostics.put("supplementedBy", "local-similarity-fallback");
                    return new RoadmapRagResult(supplemented, diagnostics);
                }
            }
            log.warn("Python Roadmap-RAG 返回空推荐，使用本地相似度降级");
        } catch (RuntimeException e) {
            log.warn("Python Roadmap-RAG 调用失败，使用本地相似度降级: {}", e.getMessage());
        }
        return fallbackRoadmapRagResult(currentJob, userSkills, allJobs);
    }

    private Map<String, Object> buildRoadmapRagPayload(
            String currentJob, List<String> userSkills, ResumeParsedData resumeData, List<JobCategory> allJobs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", BaseContext.getUserId());
        payload.put("currentJob", currentJob);
        payload.put("userSkills", safeStringList(userSkills));
        payload.put("resumeData", buildSafeResumeSummary(resumeData));
        payload.put("jobs", allJobs.stream().map(this::buildJobCandidate).collect(Collectors.toList()));

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("excludeSameCategory", true);
        filters.put("documentTypes", List.of("job", "resume_summary", "jd_summary"));
        Map<String, Object> retrieval = new LinkedHashMap<>();
        retrieval.put("topK", 10);
        retrieval.put("filters", filters);
        payload.put("retrieval", retrieval);
        return payload;
    }

    private Map<String, Object> buildSafeResumeSummary(ResumeParsedData resumeData) {
        Map<String, Object> summary = new LinkedHashMap<>();
        if (resumeData == null) {
            return summary;
        }
        summary.put("target_role", resumeData.getTargetRole());
        summary.put("current_role", resumeData.getCurrentRole());
        summary.put("experience_years", resumeData.getExperienceYears());
        summary.put("skills", safeStringList(resumeData.getSkills()));
        summary.put("match_score", resumeData.getMatchScore());
        return summary;
    }

    private Map<String, Object> buildJobCandidate(JobCategory job) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", job.getId());
        candidate.put("categoryCode", job.getJobCategoryCode());
        candidate.put("baseCategoryCode", extractBaseCategoryCode(job.getJobCategoryCode()));
        candidate.put("name", job.getJobCategoryName());
        candidate.put("level", job.getJobLevel());
        candidate.put("levelName", job.getJobLevelName());
        candidate.put("requiredSkills", parseJsonToList(job.getRequiredSkills()));
        candidate.put("description", job.getJobDescription());
        candidate.put("profile", job.getJobProfile());
        candidate.put("salaryRange", formatSalaryRange(job));
        return candidate;
    }

    private RoadmapRagResult parseRoadmapRagResult(JsonNode rootNode, List<JobCategory> allJobs) {
        if (rootNode == null || !rootNode.isObject()) {
            throw new IllegalArgumentException("Python Roadmap-RAG response must be an object");
        }
        JsonNode lateralPathsNode = rootNode.get("lateralPaths");
        if (lateralPathsNode == null || !lateralPathsNode.isArray()) {
            throw new IllegalArgumentException("Python Roadmap-RAG response missing lateralPaths array");
        }

        List<CareerPathRecommendationVO.LateralPathRecommendationVO> recommendations = new ArrayList<>();
        for (JsonNode recNode : lateralPathsNode) {
            if (recNode == null || !recNode.isObject()) {
                continue;
            }
            recommendations.add(mapPythonRoadmapRecommendation(recNode, allJobs));
        }

        Map<String, Object> diagnostics = sanitizeDiagnostics(rootNode.get("diagnostics"));
        return new RoadmapRagResult(recommendations, diagnostics);
    }

    private CareerPathRecommendationVO.LateralPathRecommendationVO mapPythonRoadmapRecommendation(
            JsonNode recNode, List<JobCategory> allJobs) {
        String categoryCode = getOptionalTextValue(recNode, "targetCategoryCode", "");
        Long targetJobId = getLongValue(recNode, "targetJobId");
        JobCategory targetJob = findTargetJob(targetJobId, categoryCode, allJobs);
        if (targetJob == null) {
            throw new IllegalArgumentException("Python Roadmap-RAG returned unknown target job");
        }

        String pathCategoryCode = !categoryCode.isBlank() ? categoryCode : extractBaseCategoryCode(targetJob.getJobCategoryCode());
        List<CareerPathRecommendationVO.PathNodeVO> pathNodes = buildPathNodes(pathCategoryCode, allJobs);
        JsonNode evidenceNode = recNode.get("evidence");
        if (evidenceNode != null && !evidenceNode.isArray()) {
            throw new IllegalArgumentException("Python Roadmap-RAG evidence must be an array");
        }

        return CareerPathRecommendationVO.LateralPathRecommendationVO.builder()
                .targetJobId(targetJob.getId())
                .targetJobName(getOptionalTextValue(recNode, "targetJobName", targetJob.getJobCategoryName()))
                .targetCategoryCode(pathCategoryCode)
                .matchScore(BigDecimal.valueOf(getNumberValue(recNode, "matchScore", 0.7)))
                .transitionDifficulty(getIntValue(recNode, "transitionDifficulty", 3))
                .estimatedMonths(getIntValue(recNode, "estimatedMonths", 12))
                .requiredSkills(parseStrictStringArray(recNode.get("requiredSkills"), "requiredSkills"))
                .possessedSkills(parseStrictStringArray(recNode.get("possessedSkills"), "possessedSkills"))
                .aiRecommendationReason(getOptionalTextValue(recNode, "aiRecommendationReason", "基于 Roadmap-RAG 证据推荐"))
                .pathNodes(pathNodes)
                .evidence(sanitizeEvidenceArray(evidenceNode))
                .build();
    }

    private JobCategory findTargetJob(Long targetJobId, String categoryCode, List<JobCategory> allJobs) {
        if (targetJobId != null) {
            Optional<JobCategory> byId = allJobs.stream()
                    .filter(job -> targetJobId.equals(job.getId()))
                    .findFirst();
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        return allJobs.stream()
                .filter(job -> extractBaseCategoryCode(job.getJobCategoryCode()).equals(categoryCode))
                .findFirst()
                .orElse(null);
    }

    private List<CareerPathRecommendationVO.PathNodeVO> buildPathNodes(String categoryCode, List<JobCategory> allJobs) {
        return allJobs.stream()
                .filter(job -> extractBaseCategoryCode(job.getJobCategoryCode()).equals(categoryCode))
                .sorted(Comparator.comparingInt(job -> levelOrderIndex(job.getJobLevel())))
                .map(job -> CareerPathRecommendationVO.PathNodeVO.builder()
                        .id(String.valueOf(job.getId()))
                        .title(job.getJobCategoryName())
                        .levelName(job.getJobLevelName())
                        .salaryRange(formatSalaryRange(job))
                        .skills(parseJsonToList(job.getRequiredSkills()))
                        .isCurrentLevel(false)
                        .build())
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> sanitizeEvidenceArray(JsonNode evidenceNode) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        if (evidenceNode == null || evidenceNode.isNull()) {
            return evidence;
        }
        for (JsonNode item : evidenceNode) {
            if (item == null || !item.isObject()) {
                throw new IllegalArgumentException("Python Roadmap-RAG evidence item must be an object");
            }
            Map<String, Object> value = new LinkedHashMap<>();
            putOptionalText(value, item, "documentType");
            putOptionalLong(value, item, "jobId");
            putOptionalText(value, item, "chunkId");
            putOptionalNumber(value, item, "score");
            putOptionalText(value, item, "source");
            evidence.add(value);
        }
        return evidence;
    }

    private Map<String, Object> sanitizeDiagnostics(JsonNode diagnosticsNode) {
        if (diagnosticsNode == null || !diagnosticsNode.isObject()) {
            throw new IllegalArgumentException("Python Roadmap-RAG response diagnostics must be an object");
        }

        Map<String, Object> diagnostics = new LinkedHashMap<>();
        JsonNode queriesNode = diagnosticsNode.get("queries");
        if (queriesNode != null && !queriesNode.isNull()) {
            if (!queriesNode.isArray()) {
                throw new IllegalArgumentException("Python Roadmap-RAG diagnostics.queries must be an array");
            }
            List<String> queries = new ArrayList<>();
            for (JsonNode queryNode : queriesNode) {
                if (!queryNode.isTextual()) {
                    throw new IllegalArgumentException("Python Roadmap-RAG diagnostics.queries must contain strings");
                }
                queries.add(redactSensitive(queryNode.asText()));
            }
            diagnostics.put("queries", queries);
        }

        JsonNode filtersNode = diagnosticsNode.get("filters");
        if (filtersNode != null && !filtersNode.isNull()) {
            if (!filtersNode.isObject()) {
                throw new IllegalArgumentException("Python Roadmap-RAG diagnostics.filters must be an object");
            }
            Map<String, Object> filters = new LinkedHashMap<>();
            JsonNode excludeSameCategoryNode = filtersNode.get("excludeSameCategory");
            if (excludeSameCategoryNode != null && !excludeSameCategoryNode.isNull()) {
                if (!excludeSameCategoryNode.isBoolean()) {
                    throw new IllegalArgumentException("Python Roadmap-RAG diagnostics.filters.excludeSameCategory must be a boolean");
                }
                filters.put("excludeSameCategory", excludeSameCategoryNode.asBoolean());
            }
            JsonNode documentTypesNode = filtersNode.get("documentTypes");
            if (documentTypesNode != null && !documentTypesNode.isNull()) {
                filters.put("documentTypes", parseStrictStringArray(documentTypesNode, "diagnostics.filters.documentTypes"));
            }
            diagnostics.put("filters", filters);
        }

        copyOptionalDiagnosticText(diagnostics, diagnosticsNode, "fusion");
        copyOptionalDiagnosticText(diagnostics, diagnosticsNode, "reranker");
        JsonNode candidateCountNode = diagnosticsNode.get("candidateCount");
        if (candidateCountNode != null && !candidateCountNode.isNull()) {
            if (!candidateCountNode.isIntegralNumber() || !candidateCountNode.canConvertToInt()) {
                throw new IllegalArgumentException("Python Roadmap-RAG diagnostics.candidateCount must be an integer");
            }
            diagnostics.put("candidateCount", candidateCountNode.asInt());
        }
        return diagnostics;
    }

    private RoadmapRagResult fallbackRoadmapRagResult(String currentJob, List<String> userSkills, List<JobCategory> allJobs) {
        List<CareerPathRecommendationVO.LateralPathRecommendationVO> fallback =
                generateFallbackLateralRecommendations(currentJob, userSkills, allJobs, new ArrayList<>());
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("fusion", "local-similarity-fallback");
        diagnostics.put("reranker", "local-deterministic");
        diagnostics.put("candidateCount", allJobs != null ? allJobs.size() : 0);
        diagnostics.put("filters", Map.of("excludeSameCategory", true));
        diagnostics.put("queries", List.of(redactSensitive(currentJob != null ? currentJob : "")));
        return new RoadmapRagResult(fallback, diagnostics);
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
        String currentBaseCategory = inferCurrentBaseCategory(currentJob, allJobs);
        List<String> sanitizedUserSkills = safeStringList(userSkills);

        // 按类别分组，计算每个类别的平均相似度
        Map<String, List<JobCategory>> jobsByCategory = allJobs.stream()
                .collect(Collectors.groupingBy(j -> extractBaseCategoryCode(j.getJobCategoryCode())));

        List<Map.Entry<String, BigDecimal>> categoryScores = new ArrayList<>();
        for (Map.Entry<String, List<JobCategory>> entry : jobsByCategory.entrySet()) {
            if (existingCategories.contains(entry.getKey())) continue;
            if (!currentBaseCategory.isBlank() && currentBaseCategory.equals(entry.getKey())) continue;

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
                    .sorted(Comparator.comparingInt(j -> levelOrderIndex(j.getJobLevel())))
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
                    .possessedSkills(sanitizedUserSkills)
                    .aiRecommendationReason("基于您的技能背景，该岗位与您的经验有一定关联，转型难度适中。")
                    .pathNodes(pathNodes)
                    .build());
        }

        return fallback;
    }

    private String inferCurrentBaseCategory(String currentJob, List<JobCategory> allJobs) {
        if (currentJob == null || currentJob.isBlank() || allJobs == null || allJobs.isEmpty()) {
            return "";
        }
        return allJobs.stream()
                .filter(job -> isJobMatch(currentJob, job))
                .map(job -> extractBaseCategoryCode(job.getJobCategoryCode()))
                .filter(code -> code != null && !code.isBlank())
                .findFirst()
                .orElse("");
    }

    private int levelOrderIndex(String level) {
        int index = LEVEL_ORDER.indexOf(level);
        return index >= 0 ? index : LEVEL_ORDER.size();
    }

    private List<String> safeStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .map(this::redactSensitive)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return objectMapper.convertValue(node, Map.class);
    }

    private Long getLongValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        if (!fieldNode.isIntegralNumber() || !fieldNode.canConvertToLong()) {
            throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must be an integer");
        }
        return fieldNode.asLong();
    }

    private int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isIntegralNumber() || !fieldNode.canConvertToInt()) {
            throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must be an integer");
        }
        return fieldNode.asInt();
    }

    private double getNumberValue(JsonNode node, String fieldName, double defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isNumber()) {
            throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must be a number");
        }
        return fieldNode.asDouble();
    }

    private String getOptionalTextValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        if (!fieldNode.isTextual()) {
            throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must be a string");
        }
        return redactSensitive(fieldNode.asText(defaultValue));
    }

    private List<String> parseStrictStringArray(JsonNode node, String fieldName) {
        List<String> result = new ArrayList<>();
        if (node == null || node.isNull()) {
            return result;
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must be an array");
        }
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Python Roadmap-RAG " + fieldName + " must contain strings");
            }
            result.add(redactSensitive(item.asText()));
        }
        return result;
    }

    private void copyOptionalDiagnosticText(Map<String, Object> target, JsonNode source, String fieldName) {
        JsonNode fieldNode = source.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return;
        }
        if (!fieldNode.isTextual()) {
            throw new IllegalArgumentException("Python Roadmap-RAG diagnostics." + fieldName + " must be a string");
        }
        target.put(fieldName, redactSensitive(fieldNode.asText()));
    }

    private void putOptionalText(Map<String, Object> target, JsonNode source, String fieldName) {
        JsonNode fieldNode = source.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return;
        }
        if (!fieldNode.isTextual()) {
            throw new IllegalArgumentException("Python Roadmap-RAG evidence." + fieldName + " must be a string");
        }
        target.put(fieldName, redactSensitive(fieldNode.asText()));
    }

    private void putOptionalLong(Map<String, Object> target, JsonNode source, String fieldName) {
        JsonNode fieldNode = source.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return;
        }
        if (!fieldNode.isIntegralNumber() || !fieldNode.canConvertToLong()) {
            throw new IllegalArgumentException("Python Roadmap-RAG evidence." + fieldName + " must be an integer");
        }
        target.put(fieldName, fieldNode.asLong());
    }

    private void putOptionalNumber(Map<String, Object> target, JsonNode source, String fieldName) {
        JsonNode fieldNode = source.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return;
        }
        if (!fieldNode.isNumber()) {
            throw new IllegalArgumentException("Python Roadmap-RAG evidence." + fieldName + " must be a number");
        }
        target.put(fieldName, fieldNode.asDouble());
    }

    private String redactSensitive(String value) {
        String redacted = value == null ? "" : value;
        for (String pattern : SENSITIVE_PATTERNS) {
            redacted = redacted.replaceAll(pattern, "[REDACTED]");
        }
        return redacted;
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

    private record RoadmapRagResult(
            List<CareerPathRecommendationVO.LateralPathRecommendationVO> lateralPaths,
            Map<String, Object> diagnostics) {
    }

    // ==================== New methods for CareerMap frontend ====================

    @Override
    public List<JobSearchResultVO> searchJobs(String q, Integer limit) {
        log.info("Search jobs: q={}, limit={}", q, limit);
        List<JobCategory> jobs = jobCategoryMapper.searchByKeyword(q, limit != null ? limit : 10);

        return jobs.stream().map(job -> {
            String salaryRange = formatSalaryRange(job);
            return JobSearchResultVO.builder()
                    .id(job.getId())
                    .jobName(job.getJobCategoryName())
                    .industry(extractIndustryFromProfile(job.getJobProfile()))
                    .salaryRange(salaryRange)
                    .similarityScore(0.8)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public JobVerticalPathDetailVO getVerticalPathByJobName(String jobName) {
        return getVerticalPathByJobNameAndLevel(jobName, null);
    }

    @Override
    public JobVerticalPathDetailVO getVerticalPathByJobNameAndLevel(String jobName, String level) {
        log.info("Get vertical path by job name: {}, level: {}", jobName, level);

        // Normalize level parameter
        String normalizedLevel = normalizeLevel(level);
        log.info("Normalized level: raw={}, normalized={}", level, normalizedLevel);

        // Search with higher limit to find best match
        List<JobCategory> matchedJobs = jobCategoryMapper.searchByKeyword(jobName, 20);
        if (matchedJobs.isEmpty()) {
            log.warn("No job found for name: {}", jobName);
            return JobVerticalPathDetailVO.builder()
                    .jobId(null)
                    .jobName(jobName)
                    .paths(new ArrayList<>())
                    .build();
        }

        // Find best matching job - prioritize level parameter first!
        JobCategory centerJob = null;

        // 1. First try: exact name match + exact level match
        if (normalizedLevel != null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobCategoryName().equalsIgnoreCase(jobName) &&
                            j.getJobLevel().equalsIgnoreCase(normalizedLevel))
                    .findFirst()
                    .orElse(null);
        }

        // 2. Second try: any name match + exact level match
        if (centerJob == null && normalizedLevel != null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobLevel().equalsIgnoreCase(normalizedLevel))
                    .findFirst()
                    .orElse(null);
        }

        // 3. Third try: exact name match (any level)
        if (centerJob == null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobCategoryName().equalsIgnoreCase(jobName))
                    .findFirst()
                    .orElse(null);
        }

        // 4. Fallback: first result
        if (centerJob == null) {
            centerJob = matchedJobs.get(0);
        }

        log.info("Found center job: {} (level: {}, levelName: {}, code: {})",
                centerJob.getJobCategoryName(), centerJob.getJobLevel(), centerJob.getJobLevelName(), centerJob.getJobCategoryCode());

        String baseCategoryCode = extractBaseCategoryCode(centerJob.getJobCategoryCode());
        log.info("Base category code: {}", baseCategoryCode);

        // Get all jobs in same category (vertical path)
        List<JobCategory> verticalJobs = jobCategoryMapper.selectVerticalPathByCategoryCode(baseCategoryCode);
        if (verticalJobs == null || verticalJobs.isEmpty()) {
            // Fallback: filter from all jobs
            List<JobCategory> allJobs = jobCategoryMapper.selectAll();
            verticalJobs = allJobs.stream()
                    .filter(j -> extractBaseCategoryCode(j.getJobCategoryCode()).equals(baseCategoryCode))
                    .collect(Collectors.toList());
        }

        if (verticalJobs == null || verticalJobs.isEmpty()) {
            log.warn("No vertical jobs found for category: {}", baseCategoryCode);
            return JobVerticalPathDetailVO.builder()
                    .jobId(centerJob.getId())
                    .jobName(centerJob.getJobCategoryName())
                    .jobLevel(centerJob.getJobLevel())
                    .jobLevelName(centerJob.getJobLevelName())
                    .paths(new ArrayList<>())
                    .build();
        }

        log.info("Found {} jobs in vertical path", verticalJobs.size());

        // Sort by level order
        verticalJobs.sort(Comparator.comparingInt(j -> LEVEL_ORDER.indexOf(j.getJobLevel())));

        int centerLevelIndex = LEVEL_ORDER.indexOf(centerJob.getJobLevel());

        List<JobVerticalPathDetailVO.PathStepVO> steps = new ArrayList<>();
        for (int i = 0; i < verticalJobs.size(); i++) {
            JobCategory j = verticalJobs.get(i);
            int step = i - centerLevelIndex; // Negative = down (lower level), Positive = up (higher level)

            steps.add(JobVerticalPathDetailVO.PathStepVO.builder()
                    .step(step)
                    .jobName(j.getJobCategoryName())
                    .jobLevel(j.getJobLevel())
                    .jobLevelName(j.getJobLevelName())
                    .skills(parseJsonToList(j.getRequiredSkills()))
                    .avgTimeMonths(estimateMonthsForLevel(j.getJobLevel()))
                    .difficulty(estimateDifficultyForLevel(j.getJobLevel()))
                    .salaryRange(formatSalaryRange(j))
                    .build());
        }

        JobVerticalPathDetailVO.JobVerticalPathVO path = JobVerticalPathDetailVO.JobVerticalPathVO.builder()
                .id(centerJob.getId())
                .pathType("vertical")
                .targetJobName(verticalJobs.get(verticalJobs.size() - 1).getJobCategoryName())
                .totalSteps(verticalJobs.size())
                .estimatedTotalMonths(verticalJobs.size() * 24)
                .confidenceScore(BigDecimal.valueOf(0.85))
                .pathSteps(steps)
                .build();

        return JobVerticalPathDetailVO.builder()
                .jobId(centerJob.getId())
                .jobName(centerJob.getJobCategoryName())
                .jobLevel(centerJob.getJobLevel())
                .jobLevelName(centerJob.getJobLevelName())
                .paths(List.of(path))
                .build();
    }

    @Override
    public JobDetailVO getJobDetail(Long id) {
        log.info("Get job detail: id={}", id);

        JobCategory job = jobCategoryMapper.selectById(id);
        if (job == null) {
            return null;
        }

        List<String> skills = parseJsonToList(job.getRequiredSkills());

        // Build advancement skills advice
        List<JobDetailVO.SkillAdviceVO> advancementSkills = skills.stream()
                .limit(5)
                .map(skill -> JobDetailVO.SkillAdviceVO.builder()
                        .name(skill)
                        .priority("high")
                        .advice("Continue developing " + skill + " expertise")
                        .build())
                .collect(Collectors.toList());

        return JobDetailVO.builder()
                .id(job.getId())
                .jobName(job.getJobCategoryName())
                .jobLevel(job.getJobLevel())
                .jobLevelName(job.getJobLevelName())
                .description(job.getJobDescription() != null ? job.getJobDescription() : "")
                .industry(extractIndustryFromProfile(job.getJobProfile()))
                .salaryRange(formatSalaryRange(job))
                .requiredSkills(skills)
                .advancementSkills(advancementSkills)
                .build();
    }

    /**
     * Extract industry from job profile JSON
     */
    private String extractIndustryFromProfile(String jobProfile) {
        if (jobProfile == null || jobProfile.isEmpty()) {
            return "Technology";
        }
        try {
            JsonNode node = objectMapper.readTree(jobProfile);
            JsonNode segment = node.get("industrySegment");
            return segment != null ? segment.asText() : "Technology";
        } catch (Exception e) {
            return "Technology";
        }
    }

    /**
     * Estimate months for level transition
     */
    private int estimateMonthsForLevel(String level) {
        if (level == null) return 24;
        return switch (level) {
            case "INTERNSHIP" -> 6;
            case "JUNIOR" -> 12;
            case "MID" -> 24;
            case "SENIOR" -> 36;
            default -> 24;
        };
    }

    /**
     * Estimate difficulty for level (1-10)
     */
    private int estimateDifficultyForLevel(String level) {
        if (level == null) return 5;
        return switch (level) {
            case "INTERNSHIP" -> 2;
            case "JUNIOR" -> 4;
            case "MID" -> 6;
            case "SENIOR" -> 8;
            default -> 5;
        };
    }

    /**
     * Parse JSON string to list
     */
    private List<String> parseJsonToList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Parse JSON failed: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * Normalize level string to standard format
     * Handles both Chinese (intern/junior/mid/senior) and English level names
     */
    private String normalizeLevel(String level) {
        if (level == null || level.isEmpty()) {
            return null;
        }
        String trimmed = level.trim();

        // Handle Chinese level names (most common from frontend: 实习岗/初级岗/中级岗/高级岗)
        if (trimmed.contains("实习")) {
            return "INTERNSHIP";
        }
        if (trimmed.contains("初级")) {
            return "JUNIOR";
        }
        if (trimmed.contains("中级")) {
            return "MID";
        }
        if (trimmed.contains("高级")) {
            return "SENIOR";
        }

        // Handle Chinese level names from database (intern, junior, mid, senior)
        if (trimmed.contains("intern") || trimmed.equals("intern")) {
            return "INTERNSHIP";
        }
        if (trimmed.contains("junior") || trimmed.equals("junior")) {
            return "JUNIOR";
        }
        if (trimmed.contains("mid") || trimmed.equals("mid")) {
            return "MID";
        }
        if (trimmed.contains("senior") || trimmed.equals("senior")) {
            return "SENIOR";
        }

        // Handle English variations (case-insensitive)
        String lower = trimmed.toLowerCase();
        if (lower.contains("intern") || lower.contains("trainee") || lower.contains("student")) {
            return "INTERNSHIP";
        }
        if (lower.contains("junior") || lower.contains("entry") || lower.contains("associate") || lower.contains("primary")) {
            return "JUNIOR";
        }
        if (lower.contains("mid") || lower.contains("middle") || lower.contains("intermediate")) {
            return "MID";
        }
        if (lower.contains("senior") || lower.contains("lead") || lower.contains("principal") || lower.contains("staff") || lower.contains("chief")) {
            return "SENIOR";
        }

        // Return as-is if no match
        return trimmed.toUpperCase();
    }

    @Override
    public UserTransitionRecommendationVO recommendTransitionByJobName(String jobName) {
        return recommendTransitionByJobNameAndLevel(jobName, null);
    }

    @Override
    public UserTransitionRecommendationVO recommendTransitionByJobNameAndLevel(String jobName, String level) {
        log.info("Recommend transition by job name: {}, level: {}", jobName, level);

        String normalizedLevel = normalizeLevel(level);
        log.info("Normalized level: raw={}, normalized={}", level, normalizedLevel);

        // Search with higher limit to find best match
        List<JobCategory> matchedJobs = jobCategoryMapper.searchByKeyword(jobName, 20);
        if (matchedJobs.isEmpty()) {
            return UserTransitionRecommendationVO.builder()
                    .currentSkills(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .message("No job found for: " + jobName)
                    .build();
        }

        // Find best matching job - prioritize level parameter
        JobCategory centerJob = null;

        if (normalizedLevel != null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobCategoryName().equalsIgnoreCase(jobName) &&
                            j.getJobLevel().equalsIgnoreCase(normalizedLevel))
                    .findFirst()
                    .orElse(null);
        }

        if (centerJob == null && normalizedLevel != null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobLevel().equalsIgnoreCase(normalizedLevel))
                    .findFirst()
                    .orElse(null);
        }

        if (centerJob == null) {
            centerJob = matchedJobs.stream()
                    .filter(j -> j.getJobCategoryName().equalsIgnoreCase(jobName))
                    .findFirst()
                    .orElse(matchedJobs.get(0));
        }

        List<String> currentSkills = parseJsonToList(centerJob.getRequiredSkills());
        String centerBaseCode = extractBaseCategoryCode(centerJob.getJobCategoryCode());

        log.info("Center job: {}, level: {}, levelName: {}, skills: {}",
                centerJob.getJobCategoryName(), centerJob.getJobLevel(), centerJob.getJobLevelName(), currentSkills.size());

        List<JobCategory> allJobs = jobCategoryMapper.selectAll();

        // Calculate skill-based similarity and dedupe by base category.
        // Prefer same level as center (same behavior as roadmap focus lateral paths).
        String centerLevel = centerJob.getJobLevel();

        Map<String, TransitionCandidate> bestByCategory = new HashMap<>();
        for (JobCategory targetJob : allJobs) {
            if (targetJob == null || targetJob.getId() == null) continue;

            // Skip same job
            if (targetJob.getId().equals(centerJob.getId())) continue;

            // Skip same category
            String targetBaseCode = extractBaseCategoryCode(targetJob.getJobCategoryCode());
            if (targetBaseCode.equals(centerBaseCode)) continue;

            // Prefer same level first; if level missing, still allow
            if (centerLevel != null && targetJob.getJobLevel() != null && !centerLevel.equalsIgnoreCase(targetJob.getJobLevel())) {
                continue;
            }

            double score = calculateSkillSimilarity(currentSkills, targetJob);
            TransitionCandidate existing = bestByCategory.get(targetBaseCode);
            if (existing == null || score > existing.score) {
                bestByCategory.put(targetBaseCode, new TransitionCandidate(targetJob, score, targetBaseCode));
            }
        }

        List<TransitionCandidate> candidates = new ArrayList<>(bestByCategory.values());

        // Fallback: if too few candidates on same level, broaden to all levels
        if (candidates.size() < 4) {
            bestByCategory.clear();
            for (JobCategory targetJob : allJobs) {
                if (targetJob == null || targetJob.getId() == null) continue;
                if (targetJob.getId().equals(centerJob.getId())) continue;
                String targetBaseCode = extractBaseCategoryCode(targetJob.getJobCategoryCode());
                if (targetBaseCode.equals(centerBaseCode)) continue;

                double score = calculateSkillSimilarity(currentSkills, targetJob);
                TransitionCandidate existing = bestByCategory.get(targetBaseCode);
                if (existing == null || score > existing.score) {
                    bestByCategory.put(targetBaseCode, new TransitionCandidate(targetJob, score, targetBaseCode));
                }
            }
            candidates = new ArrayList<>(bestByCategory.values());
        }

        // Dynamic threshold: start at 0.3 (roadmap), if not enough then lower to 0.2
        List<TransitionCandidate> filtered = filterCandidatesByThreshold(candidates, 0.3);
        if (filtered.size() < 4) {
            filtered = filterCandidatesByThreshold(candidates, 0.2);
        }

        // Sort by score descending
        filtered.sort((a, b) -> Double.compare(b.score, a.score));

        // Take top candidates (return more so frontend can render more)
        List<UserTransitionRecommendationVO.TransitionRecommendationItemVO> recommendations = new ArrayList<>();
        int count = Math.min(filtered.size(), 8);

        for (int i = 0; i < count; i++) {
            TransitionCandidate candidate = filtered.get(i);
            JobCategory targetJob = candidate.job;

            List<String> targetSkills = parseJsonToList(targetJob.getRequiredSkills());
            List<JobTransitionPathDetailVO.SkillsGapVO> skillsGap = new ArrayList<>();

            for (String skill : targetSkills) {
                boolean possessed = currentSkills.stream()
                        .anyMatch(s -> {
                            String sLower = s.toLowerCase();
                            String skillLower = skill.toLowerCase();
                            return sLower.equals(skillLower) ||
                                   sLower.contains(skillLower) ||
                                   skillLower.contains(sLower);
                        });
                if (!possessed) {
                    skillsGap.add(JobTransitionPathDetailVO.SkillsGapVO.builder()
                            .skill(skill)
                            .level(0.7)
                            .priority(skillsGap.size() < 3 ? "high" : "medium")
                            .build());
                }
            }

            int difficulty = (int) Math.max(1, Math.min(5, 6 - candidate.score * 5));
            int avgMonths = (int) (6 + (1 - candidate.score) * 18);

            recommendations.add(UserTransitionRecommendationVO.TransitionRecommendationItemVO.builder()
                    .id(targetJob.getId())
                    .recommendationId(targetJob.getId())
                    .toJobId(targetJob.getId())
                    .toJobName(targetJob.getJobCategoryName())
                    .toJobLevel(targetJob.getJobLevel())
                    .toJobLevelName(targetJob.getJobLevelName())
                    .matchScore(candidate.score)
                    .transitionDifficulty(difficulty)
                    .avgTransitionTimeMonths(avgMonths)
                    .requiredSkillsGap(skillsGap)
                    .industry(extractIndustryFromProfile(targetJob.getJobProfile()))
                    .salaryRange(formatSalaryRange(targetJob))
                    .build());
        }

        return UserTransitionRecommendationVO.builder()
                .currentSkills(currentSkills)
                .recommendations(recommendations)
                .message("Found " + recommendations.size() + " transition recommendations based on skill similarity")
                .build();
    }

    /**
     * Calculate skill similarity only (for transition matching)
     * Uses same algorithm as roadmap lateral paths
     */
    private double calculateSkillSimilarity(List<String> currentSkills, JobCategory targetJob) {
        List<String> targetSkills = parseJsonToList(targetJob.getRequiredSkills());

        if (currentSkills.isEmpty() || targetSkills.isEmpty()) {
            return 0.0;
        }

        // Same matching logic as addLateralPaths method
        long matchedSkills = currentSkills.stream()
                .filter(s -> targetSkills.stream()
                        .anyMatch(ts -> ts.toLowerCase().contains(s.toLowerCase())
                                || s.toLowerCase().contains(ts.toLowerCase())))
                .count();

        // Same formula as roadmap lateral paths
        double similarity = (double) matchedSkills / Math.max(currentSkills.size(), targetSkills.size());

        return Math.min(similarity, 1.0);
    }

    private List<TransitionCandidate> filterCandidatesByThreshold(List<TransitionCandidate> candidates, double threshold) {
        final double t = threshold;
        return candidates.stream()
                .filter(c -> c != null && c.score > t)
                .collect(Collectors.toList());
    }

    /**
     * Helper class for transition candidates
     */
    private static class TransitionCandidate {
        JobCategory job;
        double score;
        String categoryCode;

        TransitionCandidate(JobCategory job, double score, String categoryCode) {
            this.job = job;
            this.score = score;
            this.categoryCode = categoryCode;
        }
    }
}
