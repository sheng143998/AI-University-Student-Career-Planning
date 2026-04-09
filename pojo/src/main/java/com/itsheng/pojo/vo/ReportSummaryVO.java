package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "报告概要信息")
public class ReportSummaryVO {

    @Schema(description = "报告 ID")
    private Long id;

    @Schema(description = "报告编号")
    private String reportNo;

    @Schema(description = "报告标题")
    private String title;

    @Schema(description = "状态：DRAFT/COMPLETED/ARCHIVED")
    private String status;

    @Schema(description = "人岗匹配总分")
    private Integer matchScore;

    @Schema(description = "目标岗位名称")
    private String targetJob;

    @Schema(description = "生成时间")
    private String generatedAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    @Schema(description = "是否可编辑")
    private Boolean editable;
}
