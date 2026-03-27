package com.itsheng.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历优化建议 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSuggestion {
    /**
     * 建议类型：CONTENT（内容）/ SKILL（技能）/ LAYOUT（排版）
     */
    private String type;

    /**
     * 建议内容
     */
    private String content;
}
