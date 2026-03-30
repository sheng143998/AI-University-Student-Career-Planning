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
@Schema(description = "目标摘要")
public class GoalSummaryVO {
    
    @Schema(description = "目标ID", example = "1")
    private Long id;
    
    @Schema(description = "目标标题", example = "成为高级前端工程师")
    private String title;
    
    @Schema(description = "目标描述", example = "通过工程化与性能体系建设拿到更高级别岗位")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "IN_PROGRESS")
    private String status;
    
    @Schema(description = "进度 0-100", example = "65")
    private Integer progress;
    
    @Schema(description = "预计达成时间", example = "2025年12月")
    private String eta;
    
    @Schema(description = "是否为主目标", example = "true")
    private Boolean isPrimary;
}
