package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新里程碑请求")
public class GoalMilestoneUpdateDTO {
    
    @Schema(description = "里程碑标题", example = "完成 React 高级模式课程（含实战）")
    private String title;
    
    @Schema(description = "里程碑描述", example = "已完成")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "DONE")
    private String status;
    
    @Schema(description = "进度 0-100", example = "100")
    private Integer progress;
    
    @Schema(description = "排序顺序", example = "2")
    private Integer order;
}
