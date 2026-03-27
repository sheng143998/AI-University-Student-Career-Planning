package com.itsheng.pojo.vo;

import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.dto.ResumeScores;
import com.itsheng.pojo.dto.ResumeSuggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历分析结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResultVO {
    /**
     * 向量存储记录 ID
     */
    private String vectorStoreId;

    /**
     * 分析结果记录 ID
     */
    private Long analysisId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 简历原始内容
     */
    private String resumeContent;

    /**
     * OSS 文件路径
     */
    private String resumeFilePath;

    /**
     * 解析后的结构化数据
     */
    private ResumeParsedData parsedData;

    /**
     * 各维度评分
     */
    private ResumeScores scores;

    /**
     * 亮点列表
     */
    private List<String> highlights;

    /**
     * 优化建议
     */
    private List<ResumeSuggestion> suggestions;

    /**
     * 处理状态：PROCESSING / COMPLETED / FAILED
     */
    private String status;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
