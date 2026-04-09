package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "更新报告内容请求")
public class ReportUpdateDTO {

    @Schema(description = "职业目标设定")
    private Map<String, Object> careerGoal;

    @Schema(description = "行动计划")
    private Map<String, Object> actionPlan;

    @Schema(description = "职业发展路径")
    private Map<String, Object> developmentPath;

    @Schema(description = "目标岗位")
    private Map<String, Object> targetJob;
}
