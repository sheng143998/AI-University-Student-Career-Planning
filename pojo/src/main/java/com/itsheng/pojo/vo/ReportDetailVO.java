package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "报告详情")
public class ReportDetailVO {

    @Schema(description = "报告 ID")
    private Long id;

    @Schema(description = "报告编号")
    private String reportNo;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "状态：DRAFT/COMPLETED/ARCHIVED")
    private String status;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "目标岗位")
    private TargetJobInfo targetJob;

    @Schema(description = "人岗匹配总分")
    private Integer matchScore;

    @Schema(description = "匹配详情")
    private Map<String, Object> matchDetails;

    @Schema(description = "报告章节")
    private List<ReportSectionVO> sections;

    @Schema(description = "AI 建议")
    private String aiSuggestions;

    @Schema(description = "是否可编辑")
    private Boolean editable;

    @Schema(description = "生成时间")
    private String generatedAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "目标岗位信息")
    public static class TargetJobInfo {
        @Schema(description = "岗位 ID")
        private Long id;

        @Schema(description = "岗位名称")
        private String name;

        @Schema(description = "行业")
        private String industry;

        @Schema(description = "城市")
        private String city;
    }
}
