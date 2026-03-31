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
@Schema(description = "里程碑")
public class GoalMilestoneVO {
    
    @Schema(description = "里程碑ID", example = "1")
    private Long id;
    
    @Schema(description = "所属目标ID", example = "1")
    private Long goalId;
    
    @Schema(description = "里程碑标题", example = "完成 React 高级模式课程")
    private String title;
    
    @Schema(description = "里程碑描述", example = "学习 Hooks 优化与组件复用逻辑")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "DONE")
    private String status;
    
    @Schema(description = "进度 0-100", example = "100")
    private Integer progress;
    
    @Schema(description = "排序顺序", example = "1")
    private Integer order;
}
