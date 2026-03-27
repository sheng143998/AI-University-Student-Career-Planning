package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.constant.ResumeConstant;
import com.itsheng.common.constant.SystemConstants;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.dto.ResumeScores;
import com.itsheng.pojo.dto.ResumeSuggestion;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.pojo.vo.ResumeAnalysisResultVO;
import com.itsheng.service.mapper.ResumeAnalysisResultMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.ResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历分析 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private final ChatClient resumeAnalysisChatClient;
    private final ResumeAnalysisResultMapper resumeAnalysisResultMapper;
    private final UserVectorStoreMapper userVectorStoreMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void analyzeAndSave(String vectorStoreId, Long userId, String resumeContent,
                               String fileType, String originalFileName, String resumeFilePath) {
        try {
            log.info("开始 AI 解析简历，vectorStoreId: {}, userId: {}", vectorStoreId, userId);

            // 1. 调用 AI 进行简历解析
            String analysisJson = callAiForAnalysis(resumeContent);
            log.info("AI 返回的解析结果：{}", analysisJson);

            // 2. 解析 JSON 并提取各字段
            JsonAnalysisResult analysisResult = parseAnalysisJson(analysisJson);

            // 3. 构建分析结果实体
            ResumeAnalysisResult analysisRecord = ResumeAnalysisResult.builder()
                    .vectorStoreId(vectorStoreId)
                    .userId(userId)
                    .fileType(fileType)
                    .originalFileName(originalFileName)
                    .parsedData(analysisResult.parsedDataJson)
                    .scores(analysisResult.scoresJson)
                    .highlights(analysisResult.highlightsJson)
                    .suggestions(analysisResult.suggestionsJson)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // 4. 插入数据库
            resumeAnalysisResultMapper.insert(analysisRecord);
            log.info("简历分析结果已保存，analysisId: {}", analysisRecord.getId());

            // 5. 更新 user_vector_store 表的状态为 COMPLETED
            UserVectorStore userVectorStore = UserVectorStore.builder()
                    .id(vectorStoreId)
                    .resumeContent(resumeContent)
                    .build();
            userVectorStoreMapper.update(userVectorStore);
            log.info("简历解析完成，vectorStoreId: {}", vectorStoreId);

        } catch (Exception e) {
            log.error("简历 AI 解析失败：{}", e.getMessage(), e);
            // 可以在这里记录错误状态到数据库
            throw new RuntimeException("简历 AI 解析失败：" + e.getMessage(), e);
        }
    }

    @Override
    public ResumeAnalysisResultVO getAnalysisResult(String vectorStoreId) {
        // 1. 查询分析结果
        ResumeAnalysisResult result = resumeAnalysisResultMapper.selectByVectorStoreId(vectorStoreId);

        // 2. 如果尚未分析完成，返回 PROCESSING 状态
        if (result == null) {
            // 尝试从 user_vector_store 获取基本信息
            UserVectorStore store = userVectorStoreMapper.selectByVectorStoreId(vectorStoreId);
            if (store != null) {
                return ResumeAnalysisResultVO.builder()
                        .vectorStoreId(vectorStoreId)
                        .userId(store.getUserId())
                        .resumeFilePath(store.getResumeFilePath())
                        .status(ResumeConstant.PARSING_STATUS_PROCESSING)
                        .createdAt(store.getCreateTime() != null ? store.getCreateTime().toString() : null)
                        .build();
            }
            return ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .status(ResumeConstant.PARSING_STATUS_PROCESSING)
                    .build();
        }

        // 3. 构建并返回 VO
        return buildAnalysisResultVO(result, vectorStoreId);
    }

    @Override
    public List<ResumeAnalysisResultVO> getAnalysisList(Long userId, Long cursor, Integer limit) {
        List<ResumeAnalysisResult> results = resumeAnalysisResultMapper.selectByUserId(userId, cursor, limit);
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream()
                .map(r -> buildAnalysisResultVO(r, r.getVectorStoreId()))
                .collect(Collectors.toList());
    }

    /**
     * 调用 AI 进行简历解析
     */
    private String callAiForAnalysis(String resumeContent) {
        String userPrompt = """
            请分析以下简历内容：

            ===== 简历开始 =====
            %s
            ===== 简历结束 =====

            请按照要求的 JSON 格式返回分析结果，直接返回 JSON 对象，不要包含任何 markdown 标记或额外说明。
            """.formatted(resumeContent);

        ChatResponse response = resumeAnalysisChatClient.prompt()
                .system(SystemConstants.RESUME_ANALYSIS_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    /**
     * 解析 AI 返回的 JSON 结果
     * 处理可能的 markdown 代码块包装和字段缺失情况
     */
    private JsonAnalysisResult parseAnalysisJson(String json) throws JsonProcessingException {
        JsonAnalysisResult result = new JsonAnalysisResult();

        // 清理可能存在的 markdown 代码块标记
        String cleanedJson = cleanJsonResponse(json);
        log.debug("清理后的 JSON: {}", cleanedJson);

        // 使用 ObjectMapper 解析 JSON
        var rootNode = objectMapper.readTree(cleanedJson);

        // 提取 parsed_data 并转为 JSON 字符串（缺失时返回空对象）
        var parsedDataNode = rootNode.get("parsed_data");
        result.parsedDataJson = parsedDataNode != null && !parsedDataNode.isMissingNode()
                ? objectMapper.writeValueAsString(parsedDataNode)
                : "{}";

        // 提取 scores 并转为 JSON 字符串（缺失时返回默认评分）
        var scoresNode = rootNode.get("scores");
        if (scoresNode != null && !scoresNode.isMissingNode()) {
            result.scoresJson = objectMapper.writeValueAsString(scoresNode);
        } else {
            // 返回默认评分
            result.scoresJson = "{\"keyword_match\":0,\"layout\":0,\"skill_depth\":0,\"experience\":0}";
        }

        // 提取 highlights 并转为 JSON 字符串（缺失时返回空数组）
        var highlightsNode = rootNode.get("highlights");
        result.highlightsJson = highlightsNode != null && !highlightsNode.isMissingNode()
                ? objectMapper.writeValueAsString(highlightsNode)
                : "[]";

        // 提取 suggestions 并转为 JSON 字符串（缺失时返回空数组）
        var suggestionsNode = rootNode.get("suggestions");
        result.suggestionsJson = suggestionsNode != null && !suggestionsNode.isMissingNode()
                ? objectMapper.writeValueAsString(suggestionsNode)
                : "[]";

        return result;
    }

    /**
     * 清理 AI 返回的 JSON 响应，移除 markdown 代码块标记
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 移除开头的 ```json 或 ``` 标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // 移除结尾的 ``` 标记
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
     * 构建分析结果 VO
     */
    private ResumeAnalysisResultVO buildAnalysisResultVO(ResumeAnalysisResult result, String vectorStoreId) {
        try {
            ResumeAnalysisResultVO.ResumeAnalysisResultVOBuilder builder = ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .analysisId(result.getId())
                    .userId(result.getUserId())
                    .fileType(result.getFileType())
                    .originalFileName(result.getOriginalFileName())
                    .status("COMPLETED")
                    .createdAt(result.getCreateTime() != null ? result.getCreateTime().toString() : null)
                    .updatedAt(result.getUpdateTime() != null ? result.getUpdateTime().toString() : null);

            // 解析 parsed_data
            if (result.getParsedData() != null && !result.getParsedData().isEmpty()) {
                builder.parsedData(objectMapper.readValue(result.getParsedData(), ResumeParsedData.class));
            }

            // 解析 scores
            if (result.getScores() != null && !result.getScores().isEmpty()) {
                builder.scores(objectMapper.readValue(result.getScores(), ResumeScores.class));
            }

            // 解析 highlights
            if (result.getHighlights() != null && !result.getHighlights().isEmpty()) {
                builder.highlights(objectMapper.readValue(result.getHighlights(), List.class));
            }

            // 解析 suggestions
            if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
                var suggestions = objectMapper.readValue(
                        result.getSuggestions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ResumeSuggestion.class)
                );
                builder.suggestions((List<ResumeSuggestion>) suggestions);
            }

            return builder.build();

        } catch (JsonProcessingException e) {
            log.error("解析分析结果 JSON 失败：{}", e.getMessage());
            return ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .analysisId(result.getId())
                    .status("COMPLETED")
                    .build();
        }
    }

    /**
     * 内部类：用于临时存储解析后的 JSON 字符串
     */
    private static class JsonAnalysisResult {
        String parsedDataJson;
        String scoresJson;
        String highlightsJson;
        String suggestionsJson;
    }
}
