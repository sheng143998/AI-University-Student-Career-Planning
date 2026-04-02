package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位分类结果实体
 * 对应数据库表：ai_career_plan.job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCategory {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 岗位类别编码（如：JAVA_DEV, FRONTEND_DEV）
     */
    private String jobCategoryCode;

    /**
     * 岗位类别名称（如：Java 开发工程师、前端开发工程师）
     */
    private String jobCategoryName;

    /**
     * 岗位级别：INTERNSHIP/JUNIOR/MID/SENIOR
     */
    private String jobLevel;

    /**
     * 岗位级别名称：实习岗/初级岗/中级岗/高级岗
     */
    private String jobLevelName;

    /**
     * 最低薪资
     */
    private BigDecimal minSalary;

    /**
     * 最高薪资
     */
    private BigDecimal maxSalary;

    /**
     * 薪资单位：DAY/MONTH/YEAR
     */
    private String salaryUnit;

    /**
     * 关联的原始岗位 ID 列表（JSON 字符串格式）
     */
    private String sourceJobIds;

    /**
     * 关联的原始岗位数量
     */
    private Integer sourceJobCount;

    /**
     * 要求工作年限
     */
    private Integer requiredExperienceYears;

    /**
     *  Required skills list (JSON 字符串格式)
     */
    private String requiredSkills;

    /**
     * 岗位描述
     */
    private String jobDescription;

    /**
     * AI 置信度 (0.00-1.00)
     */
    private BigDecimal aiConfidenceScore;

    /**
     * 使用的 AI 提示词
     */
    private String analysisPromptUsed;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
