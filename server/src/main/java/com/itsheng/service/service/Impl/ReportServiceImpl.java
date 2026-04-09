package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.ReportGenerateDTO;
import com.itsheng.pojo.dto.ReportUpdateDTO;
import com.itsheng.pojo.entity.*;
import com.itsheng.pojo.vo.*;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.*;
import com.itsheng.service.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final CareerReportMapper careerReportMapper;
    private final StudentCapabilityProfileMapper capabilityProfileMapper;
    private final UserCareerDataMapper userCareerDataMapper;
    private final GoalMapper goalMapper;
    private final ResumeMapper resumeMapper;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CommonController commonController;

    @Override
    @Transactional
    public ReportGenerateVO generateReport(Long userId, ReportGenerateDTO dto) {
        log.info("开始生成用户 {} 的职业报告", userId);

        // 获取学生能力画像 ID 和目标岗位 ID（用于校验前置条件）
        StudentCapabilityProfile capability = capabilityProfileMapper.selectByUserId(userId);
        UserCareerData careerData = userCareerDataMapper.selectByUserId(userId);
        Long capabilityId = capability != null ? capability.getId() : null;
        Long targetJobProfileId = careerData != null ? careerData.getTargetJobId() : null;

        // 生成报告编号
        String reportNo = generateReportNo();

        // 先创建一个 PROCESSING 状态的报告记录
        CareerReport report = CareerReport.builder()
                .reportNo(reportNo)
                .userId(userId)
                .studentCapabilityId(capabilityId)
                .targetJobProfileId(targetJobProfileId)
                .status("PROCESSING")
                .isEditable(true)
                .build();

        careerReportMapper.insert(report);

        // 异步生成报告内容
        asyncGenerateReportContent(report.getId(), userId);

        return ReportGenerateVO.builder()
                .reportId(report.getId())
                .reportNo(reportNo)
                .status("PROCESSING")
                .estimatedTime(60)
                .build();
    }

    @Async
    public void asyncGenerateReportContent(Long reportId, Long userId) {
        try {
            // 1. 获取学生能力画像
            StudentCapabilityProfile capability = capabilityProfileMapper.selectByUserId(userId);
            if (capability == null) {
                log.warn("用户 {} 暂无能力画像数据", userId);
                updateReportStatus(reportId, "FAILED");
                return;
            }

            // 2. 获取用户职业数据（目标岗位）
            UserCareerData latestCareerData = userCareerDataMapper.selectByUserId(userId);

            if (latestCareerData == null) {
                log.warn("用户 {} 暂无职业数据", userId);
                updateReportStatus(reportId, "FAILED");
                return;
            }

            // 3. 构建报告各模块数据
            CareerReport report = new CareerReport();
            report.setId(reportId);
            report.setStudentCapabilityId(capability.getId());

            // 自我认知模块
            report.setSelfDiscovery(buildSelfDiscovery(capability));

            // 目标岗位信息（VARCHAR 类型，直接从 user_career_data 获取）
            String targetJobName = extractTargetJobName(latestCareerData);
            report.setTargetJob(targetJobName);

            // 人岗匹配分析
            Map<String, Object> matchDetails = performMatchAnalysis(capability, latestCareerData, targetJobName);
            report.setMatchDetails(toJson(matchDetails));
            report.setMatchScore((Integer) matchDetails.getOrDefault("overall", 0));

            // 职业目标设定
            report.setActionPlan(buildActionPlan(userId));

            // 职业发展路径
            report.setDevelopmentPath(buildDevelopmentPath(targetJobName));

            // AI 建议（从简历分析结果获取）
            report.setAiSuggestions(fetchAiSuggestions(userId));

            report.setStatus("COMPLETED");
            report.setIsEditable(true);

            careerReportMapper.update(report);

            // 生成并上传 PDF
            generateAndUploadPdf(reportId);

            log.info("用户 {} 职业报告生成完成，报告ID: {}", userId, reportId);

        } catch (Exception e) {
            log.error("用户 {} 职业报告生成失败: {}", userId, e.getMessage(), e);
            updateReportStatus(reportId, "FAILED");
        }
    }

    @Override
    public ReportSummaryVO getLatestReport(Long userId) {
        CareerReport report = careerReportMapper.selectLatestByUserId(userId);
        if (report == null) {
            return null;
        }
        return buildReportSummary(report);
    }

    @Override
    public ReportDetailVO getReportDetail(Long userId, Long reportId) {
        CareerReport report = careerReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在: " + reportId);
        }
        if (!report.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该报告");
        }
        return buildReportDetail(report);
    }

    @Override
    @Transactional
    public boolean updateReport(Long userId, Long reportId, ReportUpdateDTO dto) {
        CareerReport report = careerReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在: " + reportId);
        }
        if (!report.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改该报告");
        }
        if (!report.getIsEditable()) {
            throw new RuntimeException("报告不可编辑");
        }

        CareerReport update = new CareerReport();
        update.setId(reportId);

        try {
            if (dto.getCareerGoal() != null) {
                // careerGoal 存储在 action_plan 的 career_goal 字段中
                Map<String, Object> actionPlan = fromJson(report.getActionPlan(), Map.class);
                if (actionPlan == null) {
                    actionPlan = new HashMap<>();
                }
                actionPlan.put("career_goal", dto.getCareerGoal());
                update.setActionPlan(toJson(actionPlan));
            }
            if (dto.getActionPlan() != null) {
                update.setActionPlan(toJson(dto.getActionPlan()));
            }
            if (dto.getDevelopmentPath() != null) {
                update.setDevelopmentPath(toJson(dto.getDevelopmentPath()));
            }
            if (dto.getTargetJob() != null) {
                update.setTargetJob(toJson(dto.getTargetJob()));
            }

            return careerReportMapper.update(update) > 0;
        } catch (JsonProcessingException e) {
            log.error("更新报告内容失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新报告内容失败");
        }
    }

    @Override
    @Transactional
    public boolean deleteReport(Long userId, Long reportId) {
        CareerReport report = careerReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在: " + reportId);
        }
        if (!report.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该报告");
        }
        return careerReportMapper.deleteById(reportId) > 0;
    }

    @Override
    public byte[] downloadReportPdf(Long userId, Long reportId) {
        CareerReport report = careerReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在: " + reportId);
        }
        if (!report.getUserId().equals(userId)) {
            throw new RuntimeException("无权下载该报告");
        }

        // 如果 PDF 尚未生成，尝试同步生成
        if (report.getPdfFilePath() == null) {
            // 确保报告状态为 COMPLETED
            if (!"COMPLETED".equals(report.getStatus())) {
                throw new RuntimeException("报告尚未生成完成，当前状态: " + report.getStatus());
            }
            log.info("报告 {} PDF 未生成，尝试同步生成", reportId);
            generateAndUploadPdf(reportId);

            // 重新查询获取 pdfFilePath
            report = careerReportMapper.selectById(reportId);
            if (report.getPdfFilePath() == null) {
                throw new RuntimeException("PDF 生成失败，请稍后重试");
            }
        }

        // 通过 CommonController 从 OSS 下载 PDF
        return commonController.download(report.getPdfFilePath());
    }

    // ==================== 私有方法 ====================

    private String generateReportNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
        return String.format("CR%s%06d", dateStr, Integer.parseInt(timestamp));
    }

    private void updateReportStatus(Long reportId, String status) {
        CareerReport update = new CareerReport();
        update.setId(reportId);
        update.setStatus(status);
        careerReportMapper.update(update);
    }

    private String buildSelfDiscovery(StudentCapabilityProfile capability) throws JsonProcessingException {
        Map<String, Object> selfDiscovery = new LinkedHashMap<>();
        selfDiscovery.put("title", "自我认知");

        // 能力摘要
        String summary = "你具备良好的综合素质和专业能力";
        if (capability.getAiEvaluation() != null && !capability.getAiEvaluation().isEmpty()) {
            summary = capability.getAiEvaluation();
        }
        selfDiscovery.put("capability_summary", summary);

        // 雷达图数据
        Map<String, Object> radarChart = new LinkedHashMap<>();
        if (capability.getCapabilityScores() != null && !capability.getCapabilityScores().isEmpty()) {
            radarChart = fromJson(capability.getCapabilityScores(), Map.class);
        }
        selfDiscovery.put("radar_chart", radarChart);

        // 优势和劣势
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (capability.getSoftSkills() != null && !capability.getSoftSkills().isEmpty()) {
            Map<String, Object> softSkills = fromJson(capability.getSoftSkills(), Map.class);
            if (softSkills != null) {
                // 根据能力得分判断强弱
                Map<String, Object> scores = fromJson(capability.getCapabilityScores(), Map.class);
                if (scores != null) {
                    scores.forEach((key, value) -> {
                        if (value instanceof Number) {
                            double score = ((Number) value).doubleValue();
                            if (score >= 80) {
                                strengths.add(key + "能力强");
                            } else if (score < 60) {
                                weaknesses.add(key + "需要提升");
                            }
                        }
                    });
                }
            }
        }

        if (strengths.isEmpty()) {
            strengths.add("综合素质良好");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("可进一步提升实战经验");
        }

        selfDiscovery.put("strengths", strengths);
        selfDiscovery.put("weaknesses", weaknesses);

        return toJson(selfDiscovery);
    }

    private String extractTargetJobName(UserCareerData careerData) {
        if (careerData.getTargetJob() != null && !careerData.getTargetJob().isEmpty()) {
            return careerData.getTargetJob();
        }
        // 兜底：从 jobProfile 中解析
        if (careerData.getJobProfile() != null) {
            try {
                Map<String, Object> jobProfile = fromJson(careerData.getJobProfile(), Map.class);
                if (jobProfile != null && jobProfile.containsKey("target_job")) {
                    return (String) jobProfile.get("target_job");
                }
            } catch (Exception e) {
                log.warn("解析岗位名称失败: {}", e.getMessage());
            }
        }
        return "目标岗位";
    }

    private Map<String, Object> performMatchAnalysis(StudentCapabilityProfile capability,
                                                      UserCareerData careerData,
                                                      String targetJobName) {
        Map<String, Object> matchDetails = new LinkedHashMap<>();

        // 基于能力得分计算各维度匹配度
        int overall = capability.getOverallScore() != null ? capability.getOverallScore() : 70;

        // 四个维度得分
        int basicReq = calculateBasicRequirements(capability);
        int profSkills = calculateProfessionalSkills(capability);
        int profQuality = calculateProfessionalQuality(capability);
        int devPotential = calculateDevelopmentPotential(capability);

        matchDetails.put("overall", overall);
        matchDetails.put("basic_requirements", basicReq);
        matchDetails.put("professional_skills", profSkills);
        matchDetails.put("professional_quality", profQuality);
        matchDetails.put("development_potential", devPotential);

        return matchDetails;
    }

    private int calculateBasicRequirements(StudentCapabilityProfile capability) {
        // 基础要求：学历、专业等
        return capability.getCompletenessScore() != null ? capability.getCompletenessScore() : 75;
    }

    private int calculateProfessionalSkills(StudentCapabilityProfile capability) {
        // 职业技能：专业技能得分
        if (capability.getCapabilityScores() != null) {
            try {
                Map<String, Object> scores = fromJson(capability.getCapabilityScores(), Map.class);
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.values().stream()
                            .filter(v -> v instanceof Number)
                            .mapToDouble(v -> ((Number) v).doubleValue())
                            .average().orElse(75);
                    return (int) Math.round(avg);
                }
            } catch (Exception e) {
                log.warn("计算专业技能得分失败: {}", e.getMessage());
            }
        }
        return 70;
    }

    private int calculateProfessionalQuality(StudentCapabilityProfile capability) {
        // 职业素养：软技能
        return capability.getCompetitivenessScore() != null ? capability.getCompetitivenessScore() : 75;
    }

    private int calculateDevelopmentPotential(StudentCapabilityProfile capability) {
        // 发展潜力：综合能力
        return capability.getOverallScore() != null ? capability.getOverallScore() : 70;
    }

    private String buildActionPlan(Long userId) throws JsonProcessingException {
        Map<String, Object> actionPlan = new LinkedHashMap<>();
        actionPlan.put("title", "行动计划");

        // 从 goal 表获取用户目标
        List<Goal> goals = goalMapper.findParallelByUserId(userId);

        // 短期计划
        Map<String, Object> shortTermPlan = new LinkedHashMap<>();
        shortTermPlan.put("period", "1-6 个月");
        List<Map<String, Object>> shortTermGoals = new ArrayList<>();

        if (goals != null) {
            for (int i = 0; i < Math.min(3, goals.size()); i++) {
                Goal goal = goals.get(i);
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("id", "g_" + goal.getId());
                g.put("title", goal.getTitle());
                g.put("type", "learning");
                g.put("priority", "high");
                g.put("deadline", goal.getEta());
                g.put("status", goal.getStatus() != null ? goal.getStatus() : "pending");
                shortTermGoals.add(g);
            }
        }

        if (shortTermGoals.isEmpty()) {
            Map<String, Object> defaultGoal = new LinkedHashMap<>();
            defaultGoal.put("id", "default_1");
            defaultGoal.put("title", "完成专业技能提升计划");
            defaultGoal.put("type", "learning");
            defaultGoal.put("priority", "high");
            defaultGoal.put("deadline", LocalDate.now().plusMonths(6).toString());
            defaultGoal.put("status", "pending");
            shortTermGoals.add(defaultGoal);
        }

        shortTermPlan.put("goals", shortTermGoals);
        actionPlan.put("short_term_plan", shortTermPlan);

        // 中期计划
        Map<String, Object> mediumTermPlan = new LinkedHashMap<>();
        mediumTermPlan.put("period", "6-12 个月");
        mediumTermPlan.put("goals", Collections.emptyList());
        actionPlan.put("medium_term_plan", mediumTermPlan);

        // 评估周期
        Map<String, Object> evalCycle = new LinkedHashMap<>();
        evalCycle.put("type", "quarterly");
        evalCycle.put("metrics", Arrays.asList("技能提升", "项目数量", "作品集质量"));
        actionPlan.put("evaluation_cycle", evalCycle);

        return toJson(actionPlan);
    }

    private String buildDevelopmentPath(String targetJobName) throws JsonProcessingException {
        Map<String, Object> devPath = new LinkedHashMap<>();
        devPath.put("title", "职业发展路径");
        devPath.put("path_type", "vertical");

        String[] prefixes = {"实习", "初级", "中级", "高级"};
        String[] periods = {"第 1 年", "第 1-2 年", "第 3-5 年", "第 5-10 年"};
        String[] statuses = {"current", "future", "future", "future"};

        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < prefixes.length; i++) {
            Map<String, Object> step = new LinkedHashMap<>();
            step.put("order", i + 1);
            step.put("title", prefixes[i] + targetJobName);
            step.put("period", periods[i]);
            step.put("match_rate", Math.max(50, 90 - i * 15));
            step.put("status", statuses[i]);
            step.put("requirements", getDefaultRequirements(prefixes[i]));
            steps.add(step);
        }

        devPath.put("steps", steps);
        return toJson(devPath);
    }

    private List<String> getDefaultRequirements(String prefix) {
        return switch (prefix) {
            case "实习" -> Arrays.asList("学习基础工具使用", "了解行业规范", "完成导师分配的任务");
            case "初级" -> Arrays.asList("独立完成基础任务", "掌握核心工作技能", "积累项目经验");
            case "中级" -> Arrays.asList("独立负责项目模块", "指导新人", "跨部门协作");
            case "高级" -> Arrays.asList("负责整体方案设计", "团队管理与协调", "行业影响力建设");
            default -> List.of("持续提升专业能力");
        };
    }

    private String fetchAiSuggestions(Long userId) {
        // 从简历分析结果获取建议
        List<ResumeAnalysisResult> results = resumeMapper.selectByUserId(userId, null, 1);
        if (results != null && !results.isEmpty()) {
            ResumeAnalysisResult result = results.get(0);
            if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
                return result.getSuggestions();
            }
        }

        // 如果没有简历分析结果，使用 AI 生成
        try {
            String prompt = "基于用户的职业发展情况，给出简洁的职业发展建议（100字以内）：";
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getText();
            }
        } catch (Exception e) {
            log.warn("AI 建议生成失败: {}", e.getMessage());
        }

        return "建议持续学习提升专业技能，多参与实践项目积累经验。";
    }

    private ReportSummaryVO buildReportSummary(CareerReport report) {
        ReportSummaryVO.ReportSummaryVOBuilder builder = ReportSummaryVO.builder()
                .id(report.getId())
                .reportNo(report.getReportNo())
                .title("职业生涯发展报告")
                .status(report.getStatus())
                .matchScore(report.getMatchScore())
                .editable(report.getIsEditable());

        if (report.getCreatedAt() != null) {
            builder.generatedAt(report.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (report.getUpdatedAt() != null) {
            builder.updatedAt(report.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // 提取目标岗位名称（VARCHAR 类型）
        if (report.getTargetJob() != null) {
            builder.targetJob(report.getTargetJob());
        }

        return builder.build();
    }

    private ReportDetailVO buildReportDetail(CareerReport report) {
        ReportDetailVO.ReportDetailVOBuilder builder = ReportDetailVO.builder()
                .id(report.getId())
                .reportNo(report.getReportNo())
                .title("职业生涯发展报告")
                .status(report.getStatus())
                .userId(report.getUserId())
                .matchScore(report.getMatchScore())
                .aiSuggestions(report.getAiSuggestions())
                .editable(report.getIsEditable());

        if (report.getCreatedAt() != null) {
            builder.generatedAt(report.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (report.getUpdatedAt() != null) {
            builder.updatedAt(report.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // 解析目标岗位（VARCHAR 类型）
        if (report.getTargetJob() != null) {
            ReportDetailVO.TargetJobInfo jobInfo = ReportDetailVO.TargetJobInfo.builder()
                    .name(report.getTargetJob())
                    .build();
            builder.targetJob(jobInfo);
        }

        // 解析匹配详情
        if (report.getMatchDetails() != null) {
            try {
                builder.matchDetails(fromJson(report.getMatchDetails(), Map.class));
            } catch (Exception e) {
                log.warn("解析匹配详情失败: {}", e.getMessage());
            }
        }

        // 构建章节
        List<ReportSectionVO> sections = new ArrayList<>();

        if (report.getSelfDiscovery() != null) {
            try {
                Map<String, Object> selfDiscovery = fromJson(report.getSelfDiscovery(), Map.class);
                sections.add(ReportSectionVO.builder()
                        .key("self_discovery")
                        .title("自我认知")
                        .content(selfDiscovery)
                        .build());
            } catch (Exception e) {
                log.warn("解析自我认知失败: {}", e.getMessage());
            }
        }

        if (report.getMatchDetails() != null) {
            try {
                Map<String, Object> matchDetails = fromJson(report.getMatchDetails(), Map.class);
                Map<String, Object> matchAnalysis = buildMatchAnalysisContent(matchDetails);
                sections.add(ReportSectionVO.builder()
                        .key("match_analysis")
                        .title("人岗匹配分析")
                        .content(matchAnalysis)
                        .build());
            } catch (Exception e) {
                log.warn("解析人岗匹配分析失败: {}", e.getMessage());
            }
        }

        if (report.getTargetJob() != null) {
            Map<String, Object> careerGoal = buildCareerGoalContent(report.getTargetJob());
            sections.add(ReportSectionVO.builder()
                    .key("career_goal")
                    .title("职业目标设定")
                    .content(careerGoal)
                    .build());
        }

        if (report.getDevelopmentPath() != null) {
            try {
                Map<String, Object> devPath = fromJson(report.getDevelopmentPath(), Map.class);
                sections.add(ReportSectionVO.builder()
                        .key("development_path")
                        .title("职业发展路径")
                        .content(devPath)
                        .build());
            } catch (Exception e) {
                log.warn("解析发展路径失败: {}", e.getMessage());
            }
        }

        if (report.getActionPlan() != null) {
            try {
                Map<String, Object> actionPlan = fromJson(report.getActionPlan(), Map.class);
                sections.add(ReportSectionVO.builder()
                        .key("action_plan")
                        .title("行动计划")
                        .content(actionPlan)
                        .build());
            } catch (Exception e) {
                log.warn("解析行动计划失败: {}", e.getMessage());
            }
        }

        builder.sections(sections);
        return builder.build();
    }

    private Map<String, Object> buildMatchAnalysisContent(Map<String, Object> matchDetails) {
        Map<String, Object> content = new LinkedHashMap<>();

        int overall = (int) matchDetails.getOrDefault("overall", 0);
        content.put("summary", String.format("你与目标岗位的匹配度为 %d%%，请结合以下各维度分析了解自身优势与不足。", overall));

        // 维度分析
        Map<String, Object> dimensionAnalysis = new LinkedHashMap<>();
        dimensionAnalysis.put("basic_requirements", Map.of(
                "score", matchDetails.getOrDefault("basic_requirements", 0),
                "description", "学历、专业等基础条件符合要求"
        ));
        dimensionAnalysis.put("professional_skills", Map.of(
                "score", matchDetails.getOrDefault("professional_skills", 0),
                "description", "掌握一定专业技能，建议深化核心能力"
        ));
        dimensionAnalysis.put("professional_quality", Map.of(
                "score", matchDetails.getOrDefault("professional_quality", 0),
                "description", "具备良好的职业素养和团队协作能力"
        ));
        dimensionAnalysis.put("development_potential", Map.of(
                "score", matchDetails.getOrDefault("development_potential", 0),
                "description", "学习能力强，发展潜力较大"
        ));
        content.put("dimension_analysis", dimensionAnalysis);

        // 差距分析
        List<Map<String, String>> gapAnalysis = new ArrayList<>();
        int skills = (int) matchDetails.getOrDefault("professional_skills", 0);
        if (skills < 80) {
            Map<String, String> gap = new LinkedHashMap<>();
            gap.put("dimension", "职业技能");
            gap.put("gap", "需要进一步提升专业技能");
            gap.put("suggestion", "建议参加相关培训和实践项目");
            gapAnalysis.add(gap);
        }

        content.put("gap_analysis", gapAnalysis);
        return content;
    }

    private Map<String, Object> buildCareerGoalContent(String jobName) {
        Map<String, Object> careerGoal = new LinkedHashMap<>();
        careerGoal.put("short_term", Map.of(
                "period", "1-2 年",
                "goal", "初级" + jobName,
                "description", "掌握核心工作技能，完成入门到熟练的转变"
        ));
        careerGoal.put("medium_term", Map.of(
                "period", "3-5 年",
                "goal", "高级" + jobName + "/团队组长",
                "description", "独立负责项目，带领小团队完成目标"
        ));
        careerGoal.put("long_term", Map.of(
                "period", "5-10 年",
                "goal", "部门经理/总监",
                "description", "负责整体业务战略，管理大型团队"
        ));

        return careerGoal;
    }

    /**
     * 生成报告 PDF 并上传到 OSS
     */
    private void generateAndUploadPdf(Long reportId) {
        try {
            CareerReport report = careerReportMapper.selectById(reportId);
            if (report == null) {
                return;
            }

            // 生成 PDF 内容到字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // 尝试使用中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font headingFont = new Font(bfChinese, 14, Font.BOLD);
            Font normalFont = new Font(bfChinese, 11, Font.NORMAL);

            // 标题
            Paragraph title = new Paragraph("职业生涯发展报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 报告编号
            document.add(new Paragraph("报告编号: " + report.getReportNo(), normalFont));
            document.add(new Paragraph("生成时间: " + report.getCreatedAt(), normalFont));
            document.add(Chunk.NEWLINE);

            // 自我认知
            if (report.getSelfDiscovery() != null) {
                document.add(new Paragraph("一、自我认知", headingFont));
                document.add(Chunk.NEWLINE);
                Map<String, Object> selfDiscovery = fromJson(report.getSelfDiscovery(), Map.class);
                if (selfDiscovery != null) {
                    String summary = (String) selfDiscovery.get("capability_summary");
                    if (summary != null) {
                        document.add(new Paragraph(summary, normalFont));
                    }
                    @SuppressWarnings("unchecked")
                    List<String> strengths = (List<String>) selfDiscovery.get("strengths");
                    if (strengths != null && !strengths.isEmpty()) {
                        document.add(new Paragraph("优势:", headingFont));
                        for (String s : strengths) {
                            document.add(new Paragraph("  - " + s, normalFont));
                        }
                    }
                    @SuppressWarnings("unchecked")
                    List<String> weaknesses = (List<String>) selfDiscovery.get("weaknesses");
                    if (weaknesses != null && !weaknesses.isEmpty()) {
                        document.add(new Paragraph("待提升:", headingFont));
                        for (String w : weaknesses) {
                            document.add(new Paragraph("  - " + w, normalFont));
                        }
                    }
                }
                document.add(Chunk.NEWLINE);
            }

            // 人岗匹配
            if (report.getMatchDetails() != null) {
                document.add(new Paragraph("二、人岗匹配分析", headingFont));
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("综合匹配得分: " + report.getMatchScore() + "/100", normalFont));
                Map<String, Object> matchDetails = fromJson(report.getMatchDetails(), Map.class);
                if (matchDetails != null) {
                    document.add(new Paragraph("  基础要求: " + matchDetails.get("basic_requirements"), normalFont));
                    document.add(new Paragraph("  职业技能: " + matchDetails.get("professional_skills"), normalFont));
                    document.add(new Paragraph("  职业素养: " + matchDetails.get("professional_quality"), normalFont));
                    document.add(new Paragraph("  发展潜力: " + matchDetails.get("development_potential"), normalFont));
                }
                document.add(Chunk.NEWLINE);
            }

            // 职业目标
            if (report.getTargetJob() != null) {
                document.add(new Paragraph("三、职业目标", headingFont));
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("目标岗位: " + report.getTargetJob(), normalFont));
                document.add(Chunk.NEWLINE);
            }

            // 发展路径
            if (report.getDevelopmentPath() != null) {
                document.add(new Paragraph("四、职业发展路径", headingFont));
                document.add(Chunk.NEWLINE);
                Map<String, Object> devPath = fromJson(report.getDevelopmentPath(), Map.class);
                if (devPath != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> steps = (List<Map<String, Object>>) devPath.get("steps");
                    if (steps != null) {
                        for (Map<String, Object> step : steps) {
                            document.add(new Paragraph(
                                    "  " + step.get("title") + " (" + step.get("period") + ")", normalFont));
                        }
                    }
                }
                document.add(Chunk.NEWLINE);
            }

            // 行动计划
            if (report.getActionPlan() != null) {
                document.add(new Paragraph("五、行动计划", headingFont));
                document.add(Chunk.NEWLINE);
                Map<String, Object> actionPlan = fromJson(report.getActionPlan(), Map.class);
                if (actionPlan != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> shortTerm = (Map<String, Object>) actionPlan.get("short_term_plan");
                    if (shortTerm != null) {
                        document.add(new Paragraph("短期计划 (" + shortTerm.get("period") + "):", headingFont));
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> goals = (List<Map<String, Object>>) shortTerm.get("goals");
                        if (goals != null) {
                            for (Map<String, Object> g : goals) {
                                document.add(new Paragraph("  - " + g.get("title"), normalFont));
                            }
                        }
                    }
                }
                document.add(Chunk.NEWLINE);
            }

            // AI 建议
            if (report.getAiSuggestions() != null) {
                document.add(new Paragraph("六、AI 建议", headingFont));
                document.add(Chunk.NEWLINE);
                String aiText = formatAiSuggestionsFromObject(report.getAiSuggestions());
                document.add(new Paragraph(aiText, normalFont));
            }

            document.close();

            // 调用 CommonController.upload 上传到 OSS
            final byte[] pdfBytes = baos.toByteArray();
            final String pdfFileName = report.getReportNo() + ".pdf";
            org.springframework.web.multipart.MultipartFile pdfFile =
                    new org.springframework.web.multipart.MultipartFile() {
                        @Override public String getName() { return "file"; }
                        @Override public String getOriginalFilename() { return pdfFileName; }
                        @Override public String getContentType() { return "application/pdf"; }
                        @Override public boolean isEmpty() { return pdfBytes.length == 0; }
                        @Override public long getSize() { return pdfBytes.length; }
                        @Override public byte[] getBytes() { return pdfBytes; }
                        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(pdfBytes); }
                        @Override public void transferTo(java.io.File dest) throws java.io.IOException { java.nio.file.Files.write(dest.toPath(), pdfBytes); }
                    };
            Result<String> uploadResult = commonController.upload(pdfFile);
            String pdfUrl = uploadResult.getData();

            // 更新报告的 PDF 路径
            CareerReport update = new CareerReport();
            update.setId(reportId);
            update.setPdfFilePath(pdfUrl);
            careerReportMapper.update(update);

            log.info("报告 {} PDF 已生成并上传: {}", reportId, pdfUrl);
        } catch (Exception e) {
            log.error("报告 {} PDF 生成失败: {}", reportId, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    private String toJson(Object obj) throws JsonProcessingException {
        if (obj == null) {
            return "{}";
        }
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 将 AI 建议从 Object（可能是 Map, List 或 String）格式化为可读字符串
     */
    private String formatAiSuggestionsFromObject(Object aiSuggestions) {
        if (aiSuggestions == null) {
            return "暂无 AI 建议";
        }
        if (aiSuggestions instanceof String) {
            String str = (String) aiSuggestions;
            if (str.isEmpty()) return "暂无 AI 建议";
            return formatAiSuggestionsFromObject(parseIfJson(str));
        }
        if (aiSuggestions instanceof List) {
            List<?> list = (List<?>) aiSuggestions;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) item;
                    Object content = map.get("content");
                    sb.append(i + 1).append(". ").append(content != null ? content : item).append("\n");
                } else {
                    sb.append(i + 1).append(". ").append(item).append("\n");
                }
            }
            return sb.toString();
        }
        if (aiSuggestions instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) aiSuggestions;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List) {
                    sb.append(entry.getKey()).append(":\n");
                    List<?> items = (List<?>) value;
                    for (Object item : items) {
                        if (item instanceof Map) {
                            Map<?, ?> itemMap = (Map<?, ?>) item;
                            Object content = itemMap.get("content");
                            sb.append("  - ").append(content != null ? content : item).append("\n");
                        } else {
                            sb.append("  - ").append(item).append("\n");
                        }
                    }
                } else {
                    sb.append(entry.getKey()).append(": ").append(value).append("\n");
                }
            }
            return sb.toString();
        }
        return aiSuggestions.toString();
    }

    private Object parseIfJson(String str) {
        try {
            return objectMapper.readValue(str, Object.class);
        } catch (JsonProcessingException e) {
            return str;
        }
    }
}
