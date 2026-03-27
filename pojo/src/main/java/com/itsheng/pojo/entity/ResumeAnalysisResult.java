package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历分析结果实体
 * 对应数据库表：resume_analysis_result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResult {
    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 关联 user_vector_store.id
     */
    private String vectorStoreId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 文件类型：pdf / docx / pptx / html / txt
     */
    private String fileType;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 解析后的结构化数据（JSON）
     */
    private String parsedData;

    /**
     * 各维度评分（JSON）
     */
    private String scores;

    /**
     * 亮点列表（JSON 数组）
     */
    private String highlights;

    /**
     * 优化建议（JSON 数组）
     */
    private String suggestions;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
