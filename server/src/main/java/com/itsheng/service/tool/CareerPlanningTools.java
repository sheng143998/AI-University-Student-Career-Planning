package com.itsheng.service.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itsheng.common.constant.ToolConstant;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.utils.AliOssUtil;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.Goal;
import com.itsheng.pojo.entity.GoalMilestone;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.StudentCapabilityProfile;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.service.mapper.GoalMapper;
import com.itsheng.service.mapper.GoalMilestoneMapper;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.StudentCapabilityProfileMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.JobVectorSearchService;
import com.itsheng.service.service.MarketService;
import com.itsheng.service.service.ResumeFileEditService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareerPlanningTools {

    private final ResumeMapper resumeMapper;
    private final StudentCapabilityProfileMapper capabilityProfileMapper;
    private final UserRoadmapStepsMapper userRoadmapStepsMapper;
    private final UserVectorStoreMapper userVectorStoreMapper;
    private final GoalMapper goalMapper;
    private final GoalMilestoneMapper goalMilestoneMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final JobVectorSearchService jobVectorSearchService;
    private final UserCareerDataMapper userCareerDataMapper;
    private final MarketService marketService;
    private final AliOssUtil aliOssUtil;
    private final OpenAiEmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final VectorStore pgVectorStore;
    private final ResumeFileEditService resumeFileEditService;
    private final Clock clock;

    /**
     * 查询用户简历解析结果和能力画像。
     * @param resumeId 简历分析记录 ID，可为空；为空时默认查询当前用户最新简历
     * @return 包含 parsed_data、scores、highlights、suggestions 和 capability_profile 的 JSON 字符串
     */
    @Tool(description = "查询当前用户的简历解析结果、评分和能力画像。适用于分析简历、评估技能短板或回顾个人能力情况。当需要修改简历时，应先调用此工具，查看 skills_section_raw 字段了解原有技能区块的排版风格，再生成风格一致的描述。")
    public String getUserResumeData(
            @ToolParam(description = "简历分析记录 ID，可选；不传时默认读取当前用户最新一份简历")
            @Nullable Long resumeId,
            @Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            // 优先级：AI 模型传入的有效 resumeId > toolContext 传入 > 用户最新简历
            Long toolContextResumeId = resolveResumeIdFromContext(toolContext);
            Long effectiveResumeId = (resumeId != null && resumeId > 0) ? resumeId : toolContextResumeId;
            log.info("getUserResumeData: userId={}, aiParam={}, toolContext={}, effective={}",
                    userId, resumeId, toolContextResumeId, effectiveResumeId);
            ResumeAnalysisResult resume = getResumeByUser(userId, effectiveResumeId);
            if (resume == null) {
                log.warn("getUserResumeData 未命中简历: userId={}, effectiveResumeId={}", userId, effectiveResumeId);
                return failure("未找到可用的简历分析结果");
            }

            StudentCapabilityProfile profile = capabilityProfileMapper.selectByUserId(userId);

            // 读取简历文件原文（仅 txt/md），作为 AI 修改简历时的唯一参考依据
            String resumeFileRaw = null;
            String fileUrl = resume.getResumeFilePath();
            if (fileUrl != null && !fileUrl.isBlank()) {
                try {
                    resumeFileRaw = resumeFileEditService.readResumeFileRaw(fileUrl);
                } catch (Exception ignored) {}
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("resume_id", resume.getId());
            payload.put("vector_store_id", resume.getVectorStoreId());
            payload.put("status", resume.getStatus());
            payload.put("resume_file_path", resume.getResumeFilePath());

            // 禁令声明放在最前，确保 AI 优先读到
            if (resumeFileRaw != null) {
                payload.put("resume_file_raw", resumeFileRaw);
                payload.put("EDITING_RULE",
                        "【强制规则】执行任何简历修改任务时：" +
                        "①必须以 resume_file_raw 中的原文内容为唯一依据；" +
                        "②禁止参考 parsed_data 中的结构化数据（该数据可能与原文不符）；" +
                        "③新增技能时，须学习 resume_file_raw 中已有技能行的句式、用词、行尾标点，生成风格一致的描述句；" +
                        "④传给 modifyResume 的 newValue 只包含本次要新增的描述句，不要重复传已有的技能。");
            } else {
                payload.put("EDITING_RULE",
                        "【强制规则】当前简历文件格式不支持读取原文（非 txt/md），" +
                        "修改时禁止依赖 parsed_data，应告知用户将简历另存为 txt 或 md 格式后重新上传。");
            }

            payload.put("parsed_data_for_display_only", parseJsonObject(resume.getParsedData()));
            payload.put("scores", parseJsonObject(resume.getScores()));
            payload.put("highlights", parseJsonArray(resume.getHighlights()));
            payload.put("suggestions", parseJsonArray(resume.getSuggestions()));
            payload.put("capability_profile", buildCapabilityProfile(profile));
            return toJson(payload);
        } catch (Exception e) {
            log.error("getUserResumeData 执行失败", e);
            return failure("查询用户简历数据失败: " + e.getMessage());
        }
    }

    /**
     * 按技能关键词搜索匹配岗位。
     * @param skill 岗位技能关键词，例如 Java、前端 Vue、Spring Boot
     * @param level 岗位级别，可为空；支持 INTERNSHIP/JUNIOR/MID/SENIOR
     * @param limit 返回岗位数量，可为空
     * @return 包含岗位列表、薪资、技能要求和匹配度的 JSON 字符串
     */
    @Tool(description = "根据技能关键词搜索匹配岗位，可按岗位级别过滤，返回岗位名称、薪资、技能要求和匹配度。")
    public String searchJobs(
            @ToolParam(description = "岗位技能关键词，例如 Java、前端 Vue、Spring Boot")
            String skill,
            @ToolParam(description = "岗位级别，可选值：INTERNSHIP/JUNIOR/MID/SENIOR")
            @Nullable String level,
            @ToolParam(description = "返回岗位数量，默认 5")
            @Nullable Integer limit) {
        try {
            int safeLimit = limit == null || limit <= 0 ? ToolConstant.DEFAULT_JOB_LIMIT : Math.min(limit, 20);
            String normalizedLevel = normalizeLevel(level);

            List<JobCategory> jobs = new ArrayList<>(jobCategoryMapper.searchByKeyword(skill, safeLimit * 3));
            if (jobs.isEmpty()) {
                jobs = new ArrayList<>(jobVectorSearchService.searchSimilarJobs(buildEmbeddingVector(skill), safeLimit * 3));
            }

            List<JobCategory> filtered = jobs.stream()
                    .filter(Objects::nonNull)
                    .filter(job -> normalizedLevel == null || normalizedLevel.equalsIgnoreCase(job.getJobLevel()))
                    .distinct()
                    .limit(safeLimit)
                    .toList();

            List<Map<String, Object>> results = filtered.stream()
                    .map(job -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("job_id", job.getId());
                        item.put("job_name", job.getJobCategoryName());
                        item.put("job_level", job.getJobLevel());
                        item.put("job_level_name", job.getJobLevelName());
                        item.put("salary", formatSalary(job));
                        item.put("required_skills", parseJsonArray(job.getRequiredSkills()));
                        item.put("match_score", calculateSkillMatchScore(skill, job));
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("skill", skill);
            payload.put("level", normalizedLevel);
            payload.put("count", results.size());
            payload.put("jobs", results);
            return toJson(payload);
        } catch (Exception e) {
            log.error("searchJobs 执行失败", e);
            return failure("岗位搜索失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户职业发展路径。
     * @return 包含当前阶段索引和 steps 列表的 JSON 字符串
     */
    @Tool(description = "查询当前用户的职业发展路径和当前阶段进度，用于回答职业路线、成长阶段和下一步规划。")
    public String getUserRoadmap(@Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            UserRoadmapSteps roadmap = userRoadmapStepsMapper.selectByUserId(userId);
            if (roadmap == null) {
                return failure("当前用户尚未生成职业发展路径");
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("job_profile_id", roadmap.getJobProfileId());
            payload.put("current_step_index", roadmap.getCurrentStepIndex());
            payload.put("steps", parseJsonArray(roadmap.getSteps()));
            payload.put("updated_at", roadmap.getUpdateTime());
            return toJson(payload);
        } catch (Exception e) {
            log.error("getUserRoadmap 执行失败", e);
            return failure("查询职业路径失败: " + e.getMessage());
        }
    }

    /**
     * 根据结构化简历数据生成 PDF 并上传 OSS。
     * @param resumeData JSON 格式的简历结构化数据
     * @return 包含 PDF URL 和解析后简历数据的 JSON 字符串
     */
    @Tool(description = "根据传入的结构化简历 JSON 生成 PDF 文件并上传到 OSS，适用于独立生成或预览简历文件。")
    public String generateResumePdf(
            @ToolParam(description = "JSON 格式的简历结构化数据，支持 name、target_role、location、summary、skills、education、experience 字段")
            String resumeData,
            @Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            ObjectNode parsedDataNode = parseEditableJson(resumeData);
            String pdfUrl = generateResumePdfInternal(userId, parsedDataNode);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("user_id", userId);
            payload.put("file_url", pdfUrl);
            payload.put("resume_data", jsonNodeToJava(parsedDataNode));
            return toJson(payload);
        } catch (Exception e) {
            log.error("generateResumePdf 执行失败", e);
            return failure("生成简历 PDF 失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前准确日期和时间。
     * @return 包含日期、时间、时区和星期信息的 JSON 字符串
     */
    @Tool(description = "获取当前准确日期和时间。凡是涉及今天、明天、本周、截止日期、几个月后等时间推算时，必须先调用本工具，再基于返回结果进行判断。")
    public String getCurrentDateTime() {
        try {
            LocalDateTime now = currentDateTime();
            LocalDate today = now.toLocalDate();
            DayOfWeek dayOfWeek = now.getDayOfWeek();
            ZoneId zone = clock.getZone();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("date", today.toString());
            payload.put("datetime", now.toString());
            payload.put("timezone", zone.getId());
            payload.put("weekday", dayOfWeek.name());
            payload.put("timestamp", now.atZone(zone).toInstant().toEpochMilli());
            return toJson(payload);
        } catch (Exception e) {
            log.error("getCurrentDateTime 执行失败", e);
            return failure("获取当前日期时间失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户目标及其里程碑进度。
     * @param goalId 目标 ID，可为空；为空时查询当前用户全部目标
     * @param status 状态过滤，可选值：active/completed
     * @return 包含目标列表及里程碑信息的 JSON 字符串
     */
    @Tool(description = "查询当前用户的目标列表、状态、截止时间和里程碑进度。适用于回顾学习目标、查看完成情况或跟进阶段任务。" +
            "当用户使用今天、明天、本周、下个月等相对时间表达时，必须先调用 getCurrentDateTime 获取准确日期。")
    public String getUserGoals(
            @ToolParam(description = "目标 ID，可选；不传时查询当前用户全部目标")
            @Nullable Long goalId,
            @ToolParam(description = "状态过滤，可选值：active/completed")
            @Nullable String status,
            @Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            String normalizedStatus = normalizeGoalStatusFilter(status);

            List<Goal> goals = loadUserGoals(userId, goalId);
            if (normalizedStatus != null) {
                goals = goals.stream()
                        .filter(goal -> matchesGoalStatusFilter(goal.getStatus(), normalizedStatus))
                        .toList();
            }

            List<Map<String, Object>> goalItems = goals.stream()
                    .map(this::buildGoalPayload)
                    .toList();

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("goal_id", goalId);
            payload.put("status_filter", normalizedStatus);
            payload.put("count", goalItems.size());
            payload.put("goals", goalItems);
            return toJson(payload);
        } catch (Exception e) {
            log.error("getUserGoals 执行失败", e);
            return failure("查询用户目标失败: " + e.getMessage());
        }
    }

    /**
     * 创建、更新、完成或删除用户目标。
     * @param action 操作类型：create/update/complete/delete
     * @param goalId 目标 ID，update/complete/delete 必填
     * @param title 目标标题，create 时建议必填
     * @param isPrimary 是否为主目标，仅 create 时使用；必须先明确主目标还是副目标
     * @param deadline 目标截止日期，建议传 ISO 日期格式 yyyy-MM-dd
     * @param milestones 里程碑 JSON 数组，create/update 可选
     * @return 包含操作结果和目标信息的 JSON 字符串
     */
    @Transactional
    @Tool(description = "创建、更新、完成或删除当前用户的目标，并可批量写入里程碑。action 支持 create/update/complete/delete。" +
            "【创建规则】当 action=create 时，必须先和用户确认要创建的是主目标还是副目标，再调用本工具；" +
            "主目标需要设置 isPrimary=true，且可以创建 milestones；副目标需要设置 isPrimary=false，且不创建 milestones。" +
            "【日期规则】当用户说今天、明天、本周、几个月后、月底前等相对时间时，必须先调用 getCurrentDateTime，再传入精确的 yyyy-MM-dd 日期。")
    public String updateGoal(
            @ToolParam(description = "操作类型：create/update/complete/delete")
            String action,
            @ToolParam(description = "目标 ID，update/complete/delete 时必填")
            @Nullable Long goalId,
            @ToolParam(description = "目标标题，create 时必填，update 时可选")
            @Nullable String title,
            @ToolParam(description = "是否为主目标，仅 create 时使用。true 表示主目标，false 表示副目标；创建前必须先确认")
            @Nullable Boolean isPrimary,
            @ToolParam(description = "目标截止日期，可选，建议传 yyyy-MM-dd")
            @Nullable String deadline,
            @ToolParam(description = "里程碑 JSON 数组，可选。仅主目标创建或更新时使用；元素支持 title、desc、status、progress、order 字段")
            @Nullable String milestones,
            @Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            String normalizedAction = normalizeGoalAction(action);
            if (normalizedAction == null) {
                return failure("不支持的目标操作: " + action + "，可选值为 create/update/complete/delete");
            }

            List<Map<String, Object>> milestoneSpecs = parseMilestoneSpecs(milestones);
            Goal targetGoal;
            String message;

            switch (normalizedAction) {
                case "create" -> {
                    if (title == null || title.isBlank()) {
                        return failure("创建目标时 title 不能为空");
                    }
                    if (isPrimary == null) {
                        return failure("创建目标前必须先确认该目标是主目标还是副目标");
                    }
                    targetGoal = createGoalRecord(userId, title.trim(), deadline, isPrimary);
                    if (Boolean.TRUE.equals(isPrimary)) {
                        replaceMilestones(userId, targetGoal.getId(), milestoneSpecs);
                    }
                    message = "目标创建成功";
                }
                case "update" -> {
                    if (goalId == null || goalId <= 0) {
                        return failure("更新目标时 goalId 不能为空");
                    }
                    targetGoal = goalMapper.findByIdAndUserId(goalId, userId);
                    if (targetGoal == null) {
                        return failure("未找到对应目标");
                    }
                    boolean changed = false;
                    if (title != null && !title.isBlank()) {
                        targetGoal.setTitle(title.trim());
                        changed = true;
                    }
                    if (deadline != null && !deadline.isBlank()) {
                        targetGoal.setEta(parseGoalDeadline(deadline));
                        changed = true;
                    }
                    if (changed) {
                        goalMapper.update(targetGoal);
                    }
                    if (milestones != null && Boolean.TRUE.equals(targetGoal.getIsPrimary())) {
                        replaceMilestones(userId, targetGoal.getId(), milestoneSpecs);
                    }
                    message = "目标更新成功";
                }
                case "complete" -> {
                    if (goalId == null || goalId <= 0) {
                        return failure("完成目标时 goalId 不能为空");
                    }
                    targetGoal = goalMapper.findByIdAndUserId(goalId, userId);
                    if (targetGoal == null) {
                        return failure("未找到对应目标");
                    }
                    targetGoal.setStatus("DONE");
                    targetGoal.setProgress(100);
                    goalMapper.update(targetGoal);
                    markMilestonesDone(targetGoal.getId());
                    message = "目标已标记为完成";
                }
                case "delete" -> {
                    if (goalId == null || goalId <= 0) {
                        return failure("删除目标时 goalId 不能为空");
                    }
                    targetGoal = goalMapper.findByIdAndUserId(goalId, userId);
                    if (targetGoal == null) {
                        return failure("未找到对应目标");
                    }
                    goalMilestoneMapper.deleteByGoalId(goalId);
                    goalMapper.deleteByIdAndUserId(goalId, userId);

                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("success", true);
                    payload.put("action", normalizedAction);
                    payload.put("goal_id", goalId);
                    payload.put("message", "目标删除成功");
                    return toJson(payload);
                }
                default -> {
                    return failure("不支持的目标操作");
                }
            }

            Goal refreshedGoal = goalMapper.findByIdAndUserId(targetGoal.getId(), userId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("action", normalizedAction);
            payload.put("message", message);
            payload.put("goal", refreshedGoal == null ? Map.of() : buildGoalPayload(refreshedGoal));
            return toJson(payload);
        } catch (Exception e) {
            log.error("updateGoal 执行失败", e);
            return failure("更新目标失败: " + e.getMessage());
        }
    }

    /**
     * 获取岗位市场洞察、趋势和热门岗位。
     * @param industry 行业分类，可选
     * @param category 岗位类别或岗位关键词，可选
     * @return 包含洞察、趋势和热门岗位的 JSON 字符串
     */
    @Tool(description = "查询岗位市场洞察、薪资趋势和热门岗位。适用于回答某类岗位的市场需求、薪资变化和热门技能。")
    public String getMarketInsight(
            @ToolParam(description = "行业分类，可选，例如互联网、AI、数据分析")
            @Nullable String industry,
            @ToolParam(description = "岗位类别或岗位关键词，可选，例如前端、Java、产品经理")
            @Nullable String category) {
        try {
            JobCategory matchedJob = resolveMarketJobCategory(category);
            Long jobProfileId = matchedJob != null ? matchedJob.getId() : null;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("industry", industry);
            payload.put("category", category);
            payload.put("matched_job", matchedJob == null ? null : Map.of(
                    "job_id", matchedJob.getId(),
                    "job_name", matchedJob.getJobCategoryName(),
                    "job_level", matchedJob.getJobLevel(),
                    "job_level_name", matchedJob.getJobLevelName()
            ));
            payload.put("insight", jsonNodeToJava(objectMapper.valueToTree(marketService.getInsight(jobProfileId, null))));
            payload.put("trends", jsonNodeToJava(objectMapper.valueToTree(marketService.getTrends(jobProfileId, null, "quarter"))));
            payload.put("hot_jobs", jsonNodeToJava(objectMapper.valueToTree(marketService.getHotJobs(5, null, industry))));
            return toJson(payload);
        } catch (Exception e) {
            log.error("getMarketInsight 执行失败", e);
            return failure("查询市场洞察失败: " + e.getMessage());
        }
    }

    /**
     * 修改用户最新简历的结构化字段，并联动更新 PDF、向量和岗位匹配。
     * @param field 要修改的字段名
     * @param newValue 字段的新值，复杂字段传 JSON 字符串
     * @return 包含旧值、新值、新 PDF 地址和重新匹配岗位信息的 JSON 字符串
     */
    @Transactional
    @Tool(description = "修改当前用户最新简历的结构化字段，并自动刷新向量内容、重新匹配岗位。支持直接编辑 txt/md 格式的简历文件；docx/PDF 等格式无法自动编辑，将给出可复制的修改建议。" +
            "【强制规则】调用本工具前，必须先调用 getUserResumeData 读取 resume_file_raw，以文件原文为唯一参考，禁止使用 parsed_data_for_display_only。" +
            "修改 skills 时，newValue 数组中每个元素是一句完整描述（参考 resume_file_raw 中技能行的句式和风格），代码会自动添加 bullet 前缀，只传本次新增的描述句，不要重复传文件中已有的技能。")
    public String modifyResume(
            @ToolParam(description = "要修改的字段，仅支持 target_role、skills、name、location、experience、education")
            String field,
            @ToolParam(description = "字段的新值。字符串字段直接传文本；skills/experience/education 传 JSON 字符串")
            String newValue,
            @Nullable ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            String normalizedField = field == null ? "" : field.trim();
            if (!ToolConstant.MODIFIABLE_FIELDS.contains(normalizedField)) {
                return failure("不支持修改字段: " + normalizedField + "，可修改字段为 " + ToolConstant.MODIFIABLE_FIELDS);
            }

            ResumeAnalysisResult resume = getLatestResume(userId);
            if (resume == null) {
                return failure("未找到可修改的简历记录");
            }

            // 获取数据库中的旧值作为兜底参考
            ObjectNode dbParsedData = parseEditableJson(resume.getParsedData());
            JsonNode dbOldValueNode = dbParsedData.get(normalizedField);

            JsonNode newValueNode = convertFieldValue(normalizedField, newValue);

            // 更新数据库中的结构化数据
            dbParsedData.set(normalizedField, newValueNode);
            String updatedParsedData = toJson(dbParsedData);

            // 重新构建用于向量搜索的内容
            String resumeContent = buildResumeContent(dbParsedData);

            String currentFileUrl = resume.getResumeFilePath();
            String fileExt = resumeFileEditService.getExtension(currentFileUrl);
            boolean isSystemGeneratedPdf = currentFileUrl != null
                    && currentFileUrl.contains(ToolConstant.TOOL_RESUME_PDF_SUFFIX);

            String newFileUrl = currentFileUrl;
            boolean fileUpdated = false;
            String fileEditFailReason = null;

            if (ResumeFileEditService.EDITABLE_TYPES.contains(fileExt)) {
                // txt / md：直接编辑用户文件
                String editedUrl = resumeFileEditService.tryEditResumeFile(
                        currentFileUrl, normalizedField, dbOldValueNode, newValueNode, userId);
                if (editedUrl != null) {
                    newFileUrl = editedUrl;
                    fileUpdated = true;
                } else {
                    // 文件编辑失败：回滚数据库更新，保持与文件内容一致
                    newFileUrl = currentFileUrl;
                    fileEditFailReason = "文件内容定位失败：可能是技能区块标题（如《专业技能》）不在已知关键词列表中，或文件编码异常。";
                    log.warn("字段 [{}] 在 {} 文件中编辑失败，跳过数据库更新", normalizedField, fileExt);
                    // 回滚：不写入数据库，直接跳到返回
                    Map<String, Object> failPayload = new LinkedHashMap<>();
                    failPayload.put("success", false);
                    failPayload.put("file_updated", false);
                    failPayload.put("ALERT_TO_ASSISTANT",
                            "【文件修改失败】简历 " + fileExt.toUpperCase() + " 文件内容未做任何更改，数据库也未更新。"
                            + "你必须如实告知用户：本次修改未能成功写入简历文件，文件内容没有变化。"
                            + "原因：" + fileEditFailReason
                            + "建议：请让用户确认简历文件中是否存在《专业技能》标题行，或让用户提供简历原文片段以便调试。");
                    failPayload.put("current_file_download_url", currentFileUrl);
                    return toJson(failPayload);
                }
            } else if (isSystemGeneratedPdf || currentFileUrl == null || currentFileUrl.isBlank()) {
                // 系统生成的 PDF：重新生成
                try {
                    newFileUrl = generateResumePdfInternal(userId, dbParsedData);
                    fileUpdated = true;
                } catch (Exception e) {
                    log.error("PDF 重新生成失败", e);
                    newFileUrl = currentFileUrl != null ? currentFileUrl : "";
                }
            } else {
                newFileUrl = currentFileUrl;
            }

            // 更新记录
            ResumeAnalysisResult updatedResume = ResumeAnalysisResult.builder()
                    .vectorStoreId(resume.getVectorStoreId())
                    .parsedData(updatedParsedData)
                    .resumeFilePath(newFileUrl)
                    .updateTime(currentDateTime())
                    .build();
            resumeMapper.update(updatedResume);

            UserVectorStore vectorStore = UserVectorStore.builder()
                    .id(resume.getVectorStoreId())
                    .resumeContent(resumeContent)
                    .resumeFilePath(newFileUrl)
                    .updateTime(currentDateTime())
                    .build();
            userVectorStoreMapper.update(vectorStore);

            // 同步向量库
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("userId", userId);
            metadata.put("vectorStoreId", resume.getVectorStoreId());
            metadata.put("resumeFilePath", newFileUrl);
            org.springframework.ai.document.Document updatedDoc = new org.springframework.ai.document.Document(
                    resume.getVectorStoreId(), resumeContent, metadata);
            pgVectorStore.add(List.of(updatedDoc));

            JobCategory matchedJob = null;
            if ("target_role".equals(normalizedField)) {
                matchedJob = matchJobByKeyword(newValue);
            } else if ("skills".equals(normalizedField)) {
                matchedJob = matchJobByKeyword(extractTargetRole(dbParsedData));
            }

            if (matchedJob != null) {
                syncCareerData(userId, matchedJob);
                syncRoadmap(userId, matchedJob);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("resume_id", resume.getId());
            payload.put("field", normalizedField);
            payload.put("file_updated", fileUpdated);
            payload.put("file_url", newFileUrl);

            if (fileUpdated && newFileUrl != null && !newFileUrl.isBlank()) {
                payload.put("notice_to_assistant",
                        "【文件修改成功】简历文件已自动修改并重新上传。" +
                        "你必须在回复中告知用户修改成功，并以 Markdown 链接格式提供下载地址：[点击下载修改后的简历](" + newFileUrl + ")");
            }

            if (!fileUpdated && currentFileUrl != null && !currentFileUrl.isBlank()) {
                Map<String, Object> editInfo = new LinkedHashMap<>();
                editInfo.put("file_format", fileExt.toUpperCase());
                editInfo.put("can_auto_edit", false);
                editInfo.put("reason", "docx".equals(fileExt)
                        ? "docx 格式因内部 XML 结构复杂，暂不支持自动编辑。请下载文件后手动修改，或将简历另存为 txt/md 格式后重新上传。"
                        : "当前文件格式不支持自动编辑。");
                editInfo.put("current_file_download_url", currentFileUrl);

                Map<String, Object> suggestion = new LinkedHashMap<>();
                suggestion.put("field_label", fieldLabel(normalizedField));
                suggestion.put("manual_instruction", buildInstructionText(normalizedField, dbOldValueNode, newValueNode));

                payload.put("ALERT_TO_ASSISTANT",
                        "【文件未修改】简历文件内容没有任何更改。你必须明确告知用户：本次操作没有修改简历文件，" +
                        "请让用户下载原件后按照下方修改建议手动操作。禁止声称文件已被修改。");
                payload.put("file_edit_info", editInfo);
                payload.put("modification_suggestion", suggestion);
            }

            return toJson(payload);
        } catch (Exception e) {
            log.error("modifyResume 执行失败", e);
            return failure("修改简历失败: " + e.getMessage());
        }
    }

    private Long requireUserId(@Nullable ToolContext toolContext) {
        Long userId = BaseContext.getUserId();
        if (userId == null && toolContext != null && toolContext.getContext() != null) {
            Object contextUserId = toolContext.getContext().get("userId");
            if (contextUserId instanceof Number number) {
                userId = number.longValue();
            } else if (contextUserId instanceof String value && !value.isBlank()) {
                try {
                    userId = Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed context user id and keep null for unified exception handling.
                }
            }
        }
        if (userId == null) {
            throw new IllegalStateException("当前请求缺少用户上下文");
        }
        return userId;
    }

    private Long resolveResumeIdFromContext(@Nullable ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object contextResumeId = toolContext.getContext().get("resumeId");
        if (contextResumeId == null) {
            contextResumeId = toolContext.getContext().get("selectedResumeId");
        }
        if (contextResumeId instanceof Number number) {
            return number.longValue();
        }
        if (contextResumeId instanceof String value && !value.isBlank()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private ResumeAnalysisResult getResumeByUser(Long userId, @Nullable Long resumeId) {
        if (resumeId == null) {
            return getLatestResume(userId);
        }
        return resumeMapper.selectByIdAndUserId(resumeId, userId);
    }

    private ResumeAnalysisResult getLatestResume(Long userId) {
        return resumeMapper.selectByUserId(userId, null, 1).stream().findFirst().orElse(null);
    }

    private Map<String, Object> buildCapabilityProfile(@Nullable StudentCapabilityProfile profile) {
        if (profile == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resume_analysis_id", profile.getResumeAnalysisId());
        result.put("overall_score", profile.getOverallScore());
        result.put("completeness_score", profile.getCompletenessScore());
        result.put("competitiveness_score", profile.getCompetitivenessScore());
        result.put("capability_scores", parseJsonObject(profile.getCapabilityScores()));
        result.put("professional_skills", parseJsonArray(profile.getProfessionalSkills()));
        result.put("certificates", parseJsonArray(profile.getCertificates()));
        result.put("soft_skills", parseJsonArray(profile.getSoftSkills()));
        result.put("ai_evaluation", profile.getAiEvaluation());
        result.put("updated_at", profile.getUpdatedAt());
        return result;
    }

    private ObjectNode parseEditableJson(String json) throws Exception {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return objectMapper.createObjectNode();
        }
        JsonNode node = objectMapper.readTree(json);
        if (!node.isObject()) {
            throw new IllegalArgumentException("简历 parsed_data 不是合法对象");
        }
        return (ObjectNode) node.deepCopy();
    }

    private JsonNode convertFieldValue(String field, String newValue) throws Exception {
        if (List.of("skills", "experience", "education").contains(field)) {
            JsonNode node = objectMapper.readTree(newValue);
            if (!node.isArray()) {
                throw new IllegalArgumentException(field + " 必须传 JSON 数组字符串");
            }
            return node;
        }
        return objectMapper.getNodeFactory().textNode(newValue);
    }

    private String buildResumeContent(JsonNode parsedDataNode) {
        List<String> lines = new ArrayList<>();
        appendIfPresent(lines, "姓名", parsedDataNode.get("name"));
        appendIfPresent(lines, "目标岗位", parsedDataNode.get("target_role"));
        appendIfPresent(lines, "工作地点", parsedDataNode.get("location"));
        appendIfPresent(lines, "个人简介", parsedDataNode.get("summary"));
        appendArraySection(lines, "技能", parsedDataNode.get("skills"));
        appendComplexSection(lines, "教育经历", parsedDataNode.get("education"));
        appendComplexSection(lines, "项目/实习经历", parsedDataNode.get("experience"));
        return String.join("\n", lines);
    }

    private void appendIfPresent(List<String> lines, String label, @Nullable JsonNode node) {
        if (node != null && !node.isNull() && !node.asText().isBlank()) {
            lines.add(label + "：" + node.asText());
        }
    }

    private void appendArraySection(List<String> lines, String label, @Nullable JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return;
        }
        lines.add(label + "：");
        node.forEach(item -> lines.add("- " + item.asText("")));
    }

    private void appendComplexSection(List<String> lines, String label, @Nullable JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return;
        }
        lines.add(label + "：");
        node.forEach(item -> {
            if (item.isObject()) {
                List<String> parts = new ArrayList<>();
                item.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isBlank()) {
                        parts.add(entry.getKey() + "=" + entry.getValue().asText());
                    }
                });
                if (!parts.isEmpty()) {
                    lines.add("- " + String.join("，", parts));
                }
            } else {
                lines.add("- " + item.asText(""));
            }
        });
    }

    /**
     * 构建用户手动修改简历文件的提示内容。
     * 当简历文件为用户上传的原始文件（无法直接编辑 PDF）时，
     * 返回结构化的修改说明，告知用户具体需要在简历中做哪些改动。
     */
    private String fieldLabel(String fieldName) {
        return switch (fieldName) {
            case "name"        -> "姓名";
            case "target_role" -> "目标岗位/求职意向";
            case "location"    -> "期望工作地点";
            case "skills"      -> "技能列表";
            case "education"   -> "教育经历";
            case "experience"  -> "工作/项目经历";
            default            -> fieldName;
        };
    }

    private String buildInstructionText(String fieldName, JsonNode oldValueNode, JsonNode newValueNode) {
        return switch (fieldName) {
            case "name" -> buildNameInstruction(oldValueNode, newValueNode);
            case "target_role" -> buildTargetRoleInstruction(oldValueNode, newValueNode);
            case "location" -> buildLocationInstruction(oldValueNode, newValueNode);
            case "skills" -> buildSkillsInstruction(oldValueNode, newValueNode);
            case "education" -> buildEducationInstruction(oldValueNode, newValueNode);
            case "experience" -> buildExperienceInstruction(oldValueNode, newValueNode);
            default -> "请根据新的内容更新你的简历文件";
        };
    }

    private String buildNameInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        String oldName = oldValueNode != null ? oldValueNode.asText("") : "";
        String newName = newValueNode != null ? newValueNode.asText("") : "";
        return String.format("""
                📝 修改位置：简历顶部「姓名」栏

                修改前：%s
                修改后：%s

                操作步骤：
                1. 打开你的简历文件
                2. 找到简历最上面的姓名位置
                3. 将「%s」替换为「%s」
                4. 保存文件""",
                oldName.isEmpty() ? "（未填写）" : oldName,
                newName.isEmpty() ? "（未填写）" : newName,
                oldName.isEmpty() ? "原姓名" : oldName,
                newName);
    }

    private String buildTargetRoleInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        String oldRole = oldValueNode != null ? oldValueNode.asText("") : "";
        String newRole = newValueNode != null ? newValueNode.asText("") : "";
        return String.format("""
                📋 修改位置：「求职意向」或「目标岗位」栏

                修改前：%s
                修改后：%s

                操作步骤：
                1. 找到简历中「求职意向」、「目标岗位」或「Apply for」等栏目
                2. 将内容从「%s」更改为「%s」
                3. 这通常位于简历的个人信息部分
                4. 保存文件""",
                oldRole.isEmpty() ? "（未填写）" : oldRole,
                newRole.isEmpty() ? "（未填写）" : newRole,
                oldRole.isEmpty() ? "原内容" : oldRole,
                newRole);
    }

    private String buildLocationInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        String oldLocation = oldValueNode != null ? oldValueNode.asText("") : "";
        String newLocation = newValueNode != null ? newValueNode.asText("") : "";
        return String.format("""
                🌍 修改位置：「期望工作地点」或「城市」栏

                修改前：%s
                修改后：%s

                操作步骤：
                1. 打开简历文件
                2. 在个人信息区找到「期望工作地点」、「城市」或「Location」字段
                3. 将「%s」更新为「%s」
                4. 保存文件""",
                oldLocation.isEmpty() ? "（未填写）" : oldLocation,
                newLocation.isEmpty() ? "（未填写）" : newLocation,
                oldLocation.isEmpty() ? "原地点" : oldLocation,
                newLocation);
    }

    private String buildSkillsInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        List<String> oldSkills = extractArrayValues(oldValueNode);
        List<String> newSkills = extractArrayValues(newValueNode);

        List<String> added = newSkills.stream()
                .filter(s -> !oldSkills.contains(s))
                .toList();
        List<String> removed = oldSkills.stream()
                .filter(s -> !newSkills.contains(s))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("💡 修改建议：建议在「专业技能」栏目中，采用与简历一致的 bullet point 格式追加新技能。\n\n");

        if (!removed.isEmpty()) {
            sb.append("🗑️ 建议移除（如果不再适用）：\n");
            for (String skill : removed) {
                sb.append("  • ").append(skill).append("\n");
            }
            sb.append("\n");
        }

        if (!added.isEmpty()) {
            sb.append("✨ 推荐新增（请复制以下内容到简历）：\n");
            for (String skill : added) {
                // 这里模拟"人写的"风格，可以根据上下文微调
                sb.append("  • 熟练掌握 ").append(skill).append("，能够应用于实际项目开发中；\n");
            }
            sb.append("\n");
        }

        sb.append("📋 完整技能列表参考：\n");
        for (String skill : newSkills) {
            sb.append("  • ").append(skill).append("\n");
        }

        sb.append("\n操作步骤：\n");
        sb.append("1. 打开简历，定位到「专业技能」区块\n");
        if (!added.isEmpty()) {
            sb.append("2. 将新增的技能点按照简历现有的 bullet 格式（如 \"\\u2022 熟练掌握...\"）手动填入\n");
        }
        sb.append("3. 检查排版是否整齐美观\n");
        sb.append("4. 保存文件");

        return sb.toString();
    }

    private String buildEducationInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        List<Map<String, String>> oldEdu = extractEducationList(oldValueNode);
        List<Map<String, String>> newEdu = extractEducationList(newValueNode);

        StringBuilder sb = new StringBuilder();
        sb.append("🎓 修改位置：「教育经历」或「学历」栏\n\n");
        sb.append("修改后的教育经历内容：\n");

        for (int i = 0; i < newEdu.size(); i++) {
            Map<String, String> edu = newEdu.get(i);
            sb.append(String.format("  学校 %d：\n", i + 1));
            sb.append(String.format("    • 学校：%s\n", edu.getOrDefault("school", "未填写")));
            sb.append(String.format("    • 专业：%s\n", edu.getOrDefault("major", "未填写")));
            sb.append(String.format("    • 学位：%s\n", edu.getOrDefault("degree", "未填写")));
            sb.append(String.format("    • 时间：%s\n", edu.getOrDefault("period", "未填写")));
            sb.append("\n");
        }

        sb.append("操作步骤：\n");
        sb.append("1. 打开简历，找到「教育经历」部分\n");
        sb.append("2. 按照上面列出的内容，更新对应的学校、专业、学位等信息\n");
        sb.append("3. 确保时间格式保持一致\n");
        sb.append("4. 保存文件");

        return sb.toString();
    }

    private String buildExperienceInstruction(JsonNode oldValueNode, JsonNode newValueNode) {
        List<Map<String, String>> oldExp = extractExperienceList(oldValueNode);
        List<Map<String, String>> newExp = extractExperienceList(newValueNode);

        StringBuilder sb = new StringBuilder();
        sb.append("💼 修改位置：「工作经历」、「项目经验」或「经验」栏\n\n");
        sb.append("修改后的工作/项目经历内容：\n");

        for (int i = 0; i < newExp.size(); i++) {
            Map<String, String> exp = newExp.get(i);
            sb.append(String.format("  项目/公司 %d：\n", i + 1));
            sb.append(String.format("    • 公司/项目名：%s\n", exp.getOrDefault("company", "未填写")));
            sb.append(String.format("    • 职位/角色：%s\n", exp.getOrDefault("position", "未填写")));
            sb.append(String.format("    • 时间：%s\n", exp.getOrDefault("period", "未填写")));
            String desc = exp.getOrDefault("description", "");
            if (!desc.isEmpty()) {
                sb.append(String.format("    • 描述：%s\n", desc.length() > 100 ? desc.substring(0, 100) + "..." : desc));
            }
            sb.append("\n");
        }

        sb.append("操作步骤：\n");
        sb.append("1. 打开简历，找到「工作经历」或「项目经验」部分\n");
        sb.append("2. 按照上面列出的内容，更新公司/项目名、职位、时间和描述\n");
        sb.append("3. 保持时间格式一致\n");
        sb.append("4. 保存文件");

        return sb.toString();
    }

    /**
     * 从 JsonNode 数组中提取字符串值列表
     */
    private List<String> extractArrayValues(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        arrayNode.forEach(item -> values.add(item.asText("")));
        return values;
    }

    /**
     * 从 JsonNode 数组中提取教育经历对象列表
     */
    private List<Map<String, String>> extractEducationList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<Map<String, String>> list = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (item.isObject()) {
                Map<String, String> edu = new LinkedHashMap<>();
                edu.put("school", item.has("school") ? item.get("school").asText("") : "");
                edu.put("major", item.has("major") ? item.get("major").asText("") : "");
                edu.put("degree", item.has("degree") ? item.get("degree").asText("") : "");
                edu.put("period", item.has("period") ? item.get("period").asText("") : "");
                list.add(edu);
            }
        });
        return list;
    }

    /**
     * 从 JsonNode 数组中提取工作/项目经历对象列表
     */
    private List<Map<String, String>> extractExperienceList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<Map<String, String>> list = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (item.isObject()) {
                Map<String, String> exp = new LinkedHashMap<>();
                exp.put("company", item.has("company") ? item.get("company").asText("") : "");
                exp.put("position", item.has("position") ? item.get("position").asText("") : "");
                exp.put("period", item.has("period") ? item.get("period").asText("") : "");
                exp.put("description", item.has("description") ? item.get("description").asText("") : "");
                list.add(exp);
            }
        });
        return list;
    }

    private String generateResumePdfInternal(Long userId, JsonNode parsedDataNode) throws Exception {
        byte[] pdfBytes = renderResumePdf(parsedDataNode);
        String objectName = "resumes/" + userId + "/" + UUID.randomUUID() + ToolConstant.TOOL_RESUME_PDF_SUFFIX;
        return aliOssUtil.upload(pdfBytes, objectName);
    }

    private byte[] renderResumePdf(JsonNode parsedDataNode) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(new Rectangle(595, 842), 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont   = new Font(baseFont, 18, Font.BOLD);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD);
            Font bodyFont    = new Font(baseFont, 11, Font.NORMAL);
            Font metaFont    = new Font(baseFont, 10, Font.NORMAL);

            // ── 姓名（标题） ──
            String name = textValue(parsedDataNode, "name", "未命名简历");
            document.add(new Paragraph(name, titleFont));

            // ── 基本信息行 ──
            List<String> metaItems = new ArrayList<>();
            String targetRole = textValue(parsedDataNode, "target_role", null);
            String location   = textValue(parsedDataNode, "location", null);
            String currentRole = textValue(parsedDataNode, "current_role", null);
            JsonNode expYears = parsedDataNode.get("experience_years");
            if (targetRole   != null) metaItems.add("求职意向：" + targetRole);
            if (location     != null) metaItems.add("期望地点：" + location);
            if (currentRole  != null) metaItems.add("当前职位：" + currentRole);
            if (expYears != null && !expYears.isNull()) metaItems.add("工作年限：" + expYears.asText() + " 年");
            if (!metaItems.isEmpty()) {
                document.add(new Paragraph(String.join("   |   ", metaItems), metaFont));
            }
            document.add(new Paragraph(" "));

            // ── 个人简介 ──
            String summary = textValue(parsedDataNode, "summary", null);
            if (summary != null) {
                addPdfSection(document, "个人简介", summary, sectionFont, bodyFont);
            }

            // ── 技能 ──
            addPdfSection(document, "技能", joinJsonArray(parsedDataNode.get("skills")), sectionFont, bodyFont);

            // ── 教育经历（school / major / degree / period） ──
            addPdfSection(document, "教育经历", stringifyEducation(parsedDataNode.get("education")), sectionFont, bodyFont);

            // ── 工作/项目经历（company / position / period / description） ──
            addPdfSection(document, "工作/项目经历", stringifyExperience(parsedDataNode.get("experience")), sectionFont, bodyFont);

        } catch (DocumentException e) {
            throw new IllegalStateException("生成简历 PDF 失败", e);
        } finally {
            document.close();
        }
        return outputStream.toByteArray();
    }

    /** 渲染教育经历：每条一行，格式：school · major · degree（period） */
    private String stringifyEducation(@Nullable JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return "";
        List<String> lines = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (!item.isObject()) { lines.add(item.asText("")); return; }
            List<String> parts = new ArrayList<>();
            String school = item.has("school") ? item.get("school").asText("") : "";
            String major  = item.has("major")  ? item.get("major").asText("")  : "";
            String degree = item.has("degree") ? item.get("degree").asText("") : "";
            String period = item.has("period") ? item.get("period").asText("") : "";
            if (!school.isBlank()) parts.add(school);
            if (!major.isBlank())  parts.add(major);
            if (!degree.isBlank()) parts.add(degree);
            String line = String.join(" · ", parts);
            if (!period.isBlank()) line += "（" + period + "）";
            if (!line.isBlank()) lines.add(line);
        });
        return String.join("\n", lines);
    }

    /** 渲染工作/项目经历：每条包含公司、职位、时间、描述 */
    private String stringifyExperience(@Nullable JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return "";
        List<String> lines = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (!item.isObject()) { lines.add(item.asText("")); return; }
            String company     = item.has("company")     ? item.get("company").asText("")     : "";
            String position    = item.has("position")    ? item.get("position").asText("")    : "";
            String period      = item.has("period")      ? item.get("period").asText("")      : "";
            String description = item.has("description") ? item.get("description").asText("") : "";
            // 标题行：公司 · 职位（时间）
            List<String> header = new ArrayList<>();
            if (!company.isBlank())  header.add(company);
            if (!position.isBlank()) header.add(position);
            String headerLine = String.join(" · ", header);
            if (!period.isBlank()) headerLine += "（" + period + "）";
            if (!headerLine.isBlank()) lines.add(headerLine);
            if (!description.isBlank()) lines.add(description);
            lines.add(""); // 条目之间空一行
        });
        return String.join("\n", lines).stripTrailing();
    }

    private void addPdfSection(Document document, String title, String content, Font sectionFont, Font bodyFont) throws DocumentException {
        if (content == null || content.isBlank()) {
            return;
        }
        document.add(new Paragraph(title, sectionFont));
        document.add(new Paragraph(content, bodyFont));
        document.add(new Paragraph(" "));
    }

    private String stringifyComplexArray(@Nullable JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        arrayNode.forEach(item -> {
            if (item.isObject()) {
                List<String> parts = new ArrayList<>();
                item.fields().forEachRemaining(entry -> {
                    if (!entry.getValue().isNull() && !entry.getValue().asText().isBlank()) {
                        parts.add(entry.getKey() + "：" + entry.getValue().asText());
                    }
                });
                if (!parts.isEmpty()) {
                    lines.add(String.join("，", parts));
                }
            } else {
                lines.add(item.asText(""));
            }
        });
        return String.join("\n", lines);
    }

    private String buildEmbeddingVector(String text) {
        float[] vector = embeddingModel.embed(text == null ? "" : text);
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    private JobCategory matchJobByKeyword(@Nullable String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return jobCategoryMapper.searchByKeyword(keyword, 1).stream().findFirst().orElse(null);
    }

    private void syncCareerData(Long userId, JobCategory matchedJob) throws Exception {
        UserCareerData existing = userCareerDataMapper.selectByUserId(userId);
        Map<String, Object> jobProfile = new LinkedHashMap<>();
        jobProfile.put("id", matchedJob.getId());
        jobProfile.put("name", matchedJob.getJobCategoryName());
        jobProfile.put("level", matchedJob.getJobLevel());
        jobProfile.put("level_name", matchedJob.getJobLevelName());
        jobProfile.put("required_skills", parseJsonArray(matchedJob.getRequiredSkills()));
        jobProfile.put("salary", formatSalary(matchedJob));

        UserCareerData data = UserCareerData.builder()
                .userId(userId)
                .targetJob(matchedJob.getJobCategoryName())
                .targetJobId(matchedJob.getId())
                .jobProfile(toJson(jobProfile))
                .updateTime(currentDateTime())
                .build();

        if (existing != null) {
            data.setId(existing.getId());
            userCareerDataMapper.update(data);
        } else {
            data.setMatchSummary("{\"score\":0,\"description\":\"由 Tool 自动更新\"}");
            data.setMarketTrends("[]");
            data.setSkillRadar("{}");
            data.setActions("[]");
            data.setCreateTime(currentDateTime());
            userCareerDataMapper.insert(data);
        }
    }

    private void syncRoadmap(Long userId, JobCategory matchedJob) throws Exception {
        String baseCategoryCode = matchedJob.getJobCategoryCode()
                .replaceAll("_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "");
        List<JobCategory> verticalPath = jobCategoryMapper.selectVerticalPathByCategoryCode(baseCategoryCode);
        if (verticalPath == null || verticalPath.isEmpty()) {
            return;
        }

        verticalPath = verticalPath.stream()
                .sorted((a, b) -> Integer.compare(levelIndex(a.getJobLevel()), levelIndex(b.getJobLevel())))
                .toList();

        int currentIndex = 0;
        List<Map<String, Object>> steps = new ArrayList<>();
        for (int i = 0; i < verticalPath.size(); i++) {
            JobCategory job = verticalPath.get(i);
            if (Objects.equals(job.getId(), matchedJob.getId())) {
                currentIndex = i;
            }

            Map<String, Object> step = new LinkedHashMap<>();
            step.put("job_id", job.getId());
            step.put("title", job.getJobCategoryName());
            step.put("level", job.getJobLevel());
            step.put("level_name", job.getJobLevelName());
            step.put("active", Objects.equals(job.getId(), matchedJob.getId()));
            step.put("status", Objects.equals(job.getId(), matchedJob.getId()) ? "当前目标" : "待解锁");
            steps.add(step);
        }

        UserRoadmapSteps existing = userRoadmapStepsMapper.selectByUserId(userId);
        UserRoadmapSteps roadmap = UserRoadmapSteps.builder()
                .userId(userId)
                .jobProfileId(matchedJob.getId())
                .currentStepIndex(currentIndex)
                .steps(toJson(steps))
                .updateTime(currentDateTime())
                .build();

        if (existing != null) {
            roadmap.setId(existing.getId());
            userRoadmapStepsMapper.update(roadmap);
        } else {
            roadmap.setCreateTime(currentDateTime());
            userRoadmapStepsMapper.insert(roadmap);
        }
    }

    private int levelIndex(@Nullable String level) {
        if (level == null) {
            return Integer.MAX_VALUE;
        }
        int index = ToolConstant.ROADMAP_LEVEL_ORDER.indexOf(level.toUpperCase(Locale.ROOT));
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private String normalizeLevel(@Nullable String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        return switch (level.trim().toUpperCase(Locale.ROOT)) {
            case "INTERNSHIP", "JUNIOR", "MID", "SENIOR" -> level.trim().toUpperCase(Locale.ROOT);
            default -> null;
        };
    }

    private List<Goal> loadUserGoals(Long userId, @Nullable Long goalId) {
        if (goalId != null && goalId > 0) {
            Goal goal = goalMapper.findByIdAndUserId(goalId, userId);
            return goal == null ? List.of() : List.of(goal);
        }

        List<Goal> goals = new ArrayList<>();
        Goal primary = goalMapper.findPrimaryByUserId(userId);
        if (primary != null) {
            goals.add(primary);
        }
        goals.addAll(goalMapper.findParallelByUserId(userId));
        goals.sort(Comparator
                .comparing((Goal goal) -> !Boolean.TRUE.equals(goal.getIsPrimary()))
                .thenComparing(Goal::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return goals;
    }

    private Map<String, Object> buildGoalPayload(Goal goal) {
        List<GoalMilestone> milestones = goalMilestoneMapper.findByGoalId(goal.getId());
        int milestoneTotal = milestones.size();
        int milestoneCompleted = (int) milestones.stream()
                .filter(item -> "DONE".equalsIgnoreCase(item.getStatus()))
                .count();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("goal_id", goal.getId());
        item.put("title", goal.getTitle());
        item.put("description", goal.getGoalDesc());
        item.put("status", goal.getStatus());
        item.put("progress", goal.getProgress());
        item.put("deadline", goal.getEta());
        item.put("is_primary", goal.getIsPrimary());
        item.put("milestone_completed", milestoneCompleted);
        item.put("milestone_total", milestoneTotal);
        item.put("milestones", milestones.stream().map(this::buildMilestonePayload).toList());
        item.put("success_criteria", buildGoalSuccessCriteria(goal));
        item.put("long_term_aspirations", parseJsonArray(goal.getLongTermAspirations()));
        item.put("ai_advice", goal.getAiAdvice());
        item.put("created_at", goal.getCreateTime());
        item.put("updated_at", goal.getUpdateTime());
        return item;
    }

    private Map<String, Object> buildMilestonePayload(GoalMilestone milestone) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("milestone_id", milestone.getId());
        item.put("goal_id", milestone.getGoalId());
        item.put("title", milestone.getTitle());
        item.put("description", milestone.getMilestoneDesc());
        item.put("status", milestone.getStatus());
        item.put("progress", milestone.getProgress());
        item.put("sort_order", milestone.getSortOrder());
        item.put("created_at", milestone.getCreateTime());
        item.put("updated_at", milestone.getUpdateTime());
        return item;
    }

    private Map<String, Object> buildGoalSuccessCriteria(Goal goal) {
        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("salary", goal.getSuccessSalary());
        criteria.put("companies", parseJsonArray(goal.getSuccessCompanies()));
        criteria.put("cities", parseJsonArray(goal.getSuccessCities()));
        return criteria;
    }

    private String normalizeGoalStatusFilter(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "active", "completed" -> status.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private boolean matchesGoalStatusFilter(@Nullable String goalStatus, String filter) {
        String normalized = goalStatus == null ? "" : goalStatus.trim().toUpperCase(Locale.ROOT);
        return switch (filter) {
            case "active" -> !"DONE".equals(normalized);
            case "completed" -> "DONE".equals(normalized);
            default -> true;
        };
    }

    private String normalizeGoalAction(@Nullable String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return switch (action.trim().toLowerCase(Locale.ROOT)) {
            case "create", "update", "complete", "delete" -> action.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private Goal createGoalRecord(Long userId, String title, @Nullable String deadline, boolean isPrimary) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(title);
        goal.setGoalDesc("");
        goal.setStatus("TODO");
        goal.setProgress(0);
        goal.setEta(parseGoalDeadline(deadline));
        goal.setIsPrimary(isPrimary);
        goal.setSuccessSalary(null);
        goal.setSuccessCompanies("[]");
        goal.setSuccessCities("[]");
        goal.setLongTermAspirations("[]");
        goal.setAiAdvice(null);
        if (isPrimary) {
            goalMapper.clearPrimaryByUserId(userId);
        }
        goalMapper.insert(goal);
        return goal;
    }

    private String parseGoalDeadline(@Nullable String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(deadline.trim()).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("deadline 必须为 yyyy-MM-dd 格式");
        }
    }

    private List<Map<String, Object>> parseMilestoneSpecs(@Nullable String milestones) throws Exception {
        if (milestones == null || milestones.isBlank()) {
            return List.of();
        }
        JsonNode node = objectMapper.readTree(milestones);
        if (!node.isArray()) {
            throw new IllegalArgumentException("milestones 必须传 JSON 数组字符串");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isObject()) {
                continue;
            }
            Map<String, Object> spec = new LinkedHashMap<>();
            spec.put("title", textValue(item, "title", ""));
            spec.put("desc", textValue(item, "desc", ""));
            spec.put("status", textValue(item, "status", "TODO"));
            spec.put("progress", item.has("progress") && item.get("progress").canConvertToInt() ? item.get("progress").asInt() : 0);
            spec.put("order", item.has("order") && item.get("order").canConvertToInt() ? item.get("order").asInt() : result.size() + 1);
            result.add(spec);
        }
        return result;
    }

    private void replaceMilestones(Long userId, Long goalId, List<Map<String, Object>> milestoneSpecs) {
        goalMilestoneMapper.deleteByGoalId(goalId);
        for (int i = 0; i < milestoneSpecs.size(); i++) {
            Map<String, Object> spec = milestoneSpecs.get(i);
            String title = String.valueOf(spec.getOrDefault("title", "")).trim();
            if (title.isBlank()) {
                continue;
            }
            GoalMilestone milestone = new GoalMilestone();
            milestone.setGoalId(goalId);
            milestone.setUserId(userId);
            milestone.setTitle(title);
            milestone.setMilestoneDesc(String.valueOf(spec.getOrDefault("desc", "")));
            milestone.setStatus(normalizeMilestoneStatus(String.valueOf(spec.getOrDefault("status", "TODO"))));
            milestone.setProgress(clampProgress(asInt(spec.get("progress"), 0)));
            milestone.setSortOrder(asInt(spec.get("order"), i + 1));
            if ("DONE".equals(milestone.getStatus())) {
                milestone.setProgress(100);
            }
            goalMilestoneMapper.insert(milestone);
        }
    }

    private void markMilestonesDone(Long goalId) {
        List<GoalMilestone> milestones = goalMilestoneMapper.findByGoalId(goalId);
        for (GoalMilestone milestone : milestones) {
            milestone.setStatus("DONE");
            milestone.setProgress(100);
            goalMilestoneMapper.update(milestone);
        }
    }

    private String normalizeMilestoneStatus(@Nullable String status) {
        if (status == null || status.isBlank()) {
            return "TODO";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "TODO", "IN_PROGRESS", "DONE" -> status.trim().toUpperCase(Locale.ROOT);
            default -> "TODO";
        };
    }

    private int asInt(@Nullable Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private int clampProgress(int progress) {
        return Math.max(0, Math.min(100, progress));
    }

    private JobCategory resolveMarketJobCategory(@Nullable String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return jobCategoryMapper.searchByKeyword(category.trim(), 1)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(clock);
    }

    private BigDecimal calculateSkillMatchScore(String skill, JobCategory job) {
        List<String> requiredSkills = parseJsonArray(job.getRequiredSkills()).stream()
                .map(String::valueOf)
                .toList();
        if (requiredSkills.isEmpty() || skill == null || skill.isBlank()) {
            return BigDecimal.ZERO;
        }

        String lowerSkill = skill.toLowerCase(Locale.ROOT);
        long matched = requiredSkills.stream()
                .filter(item -> lowerSkill.contains(item.toLowerCase(Locale.ROOT)) || item.toLowerCase(Locale.ROOT).contains(lowerSkill))
                .count();
        return BigDecimal.valueOf((double) matched / requiredSkills.size()).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatSalary(JobCategory job) {
        if (job.getMinSalary() == null || job.getMaxSalary() == null) {
            return "面议";
        }
        String unit = job.getSalaryUnit() == null ? "" : "/" + job.getSalaryUnit();
        return job.getMinSalary().stripTrailingZeros().toPlainString() + "-" +
                job.getMaxSalary().stripTrailingZeros().toPlainString() + unit;
    }

    private List<Object> parseJsonArray(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> parseJsonObject(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Object jsonNodeToJava(@Nullable JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, Object.class);
        } catch (IllegalArgumentException e) {
            return node.asText();
        }
    }

    private String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String failure(String message) {
        try {
            return toJson(Map.of("success", false, "message", message));
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        }
    }

    private String textValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return defaultValue;
        }
        String value = field.asText();
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String extractTargetRole(JsonNode parsedDataNode) {
        return textValue(parsedDataNode, "target_role", "");
    }

    private String joinJsonArray(@Nullable JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText("")));
        return String.join(" ", values);
    }

}
