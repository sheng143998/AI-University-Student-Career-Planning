package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "目标详情")
public class GoalDetailVO {
    
    @Schema(description = "目标摘要")
    private GoalSummaryVO goal;
    
    @Schema(description = "里程碑列表")
    private List<GoalMilestoneVO> milestones;
    
    @Schema(description = "成功准则")
    private SuccessCriteriaVO successCriteria;
    
    @Schema(description = "长期愿景列表")
    private List<LongTermAspirationVO> longTermAspirations;
    
    @Schema(description = "AI建议")
    private AiAdviceVO aiAdvice;
}
