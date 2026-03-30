package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建里程碑请求")
public class GoalMilestoneCreateDTO {
    
    @NotBlank(message = "里程碑标题不能为空")
    @Schema(description = "里程碑标题", example = "完成 React 高级模式课程")
    private String title;
    
    @Schema(description = "里程碑描述", example = "学习 Hooks 优化与组件复用逻辑")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "TODO")
    private String status;
    
    @Schema(description = "进度 0-100", example = "0")
    private Integer progress;
    
    @Schema(description = "排序顺序", example = "1")
    private Integer order;
}
