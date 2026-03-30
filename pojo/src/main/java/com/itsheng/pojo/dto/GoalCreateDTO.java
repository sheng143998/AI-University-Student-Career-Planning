package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建目标请求")
public class GoalCreateDTO {
    
    @NotBlank(message = "目标标题不能为空")
    @Schema(description = "目标标题", example = "成为高级前端工程师")
    private String title;
    
    @Schema(description = "目标描述", example = "通过工程化与性能体系建设拿到更高级别岗位")
    private String desc;
    
    @Schema(description = "状态：TODO/IN_PROGRESS/DONE", example = "TODO")
    private String status;
    
    @Schema(description = "进度 0-100", example = "0")
    private Integer progress;
    
    @Schema(description = "预计达成时间", example = "2025年12月")
    private String eta;
    
    @Schema(description = "是否为主目标", example = "false")
    private Boolean isPrimary;
}
