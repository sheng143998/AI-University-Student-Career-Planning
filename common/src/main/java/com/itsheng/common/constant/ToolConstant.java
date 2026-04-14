package com.itsheng.common.constant;

import java.util.List;

/**
 * Tool Calling 相关常量
 */
public class ToolConstant {

    /**
     * 默认简历扫描记录数
     */
    public static final int DEFAULT_RESUME_SCAN_LIMIT = 50;

    /**
     * 默认岗位返回数量
     */
    public static final int DEFAULT_JOB_LIMIT = 5;

    /**
     * 职业路径等级顺序
     */
    public static final List<String> ROADMAP_LEVEL_ORDER = List.of("INTERNSHIP", "JUNIOR", "MID", "SENIOR");

    /**
     * 允许 AI 修改的简历字段
     */
    public static final List<String> MODIFIABLE_FIELDS = List.of(
            "target_role", "skills", "name", "location", "experience", "education"
    );

    /**
     * Tool 生成简历 PDF 的 OSS 文件后缀（同时作为系统生成文件的标识）
     */
    public static final String TOOL_RESUME_PDF_SUFFIX = "_tool.pdf";

    private ToolConstant() {
    }
}
