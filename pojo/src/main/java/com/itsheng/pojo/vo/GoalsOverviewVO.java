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
@Schema(description = "目标总览")
public class GoalsOverviewVO {
    
    @Schema(description = "主目标")
    private GoalSummaryVO primaryGoal;
    
    @Schema(description = "里程碑列表")
    private List<GoalMilestoneVO> milestones;
    
    @Schema(description = "已完成里程碑数", example = "3")
    private Integer milestonesCompleted;
    
    @Schema(description = "里程碑总数", example = "6")
    private Integer milestonesTotal;
    
    @Schema(description = "成功准则")
    private SuccessCriteriaVO successCriteria;
    
    @Schema(description = "长期愿景列表")
    private List<LongTermAspirationVO> longTermAspirations;
    
    @Schema(description = "AI建议")
    private AiAdviceVO aiAdvice;
    
    @Schema(description = "并行目标列表")
    private List<GoalSummaryVO> parallelGoals;
}
