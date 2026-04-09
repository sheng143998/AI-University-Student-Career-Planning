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
@Schema(description = "报告生成响应")
public class ReportGenerateVO {

    @Schema(description = "报告 ID")
    private Long reportId;

    @Schema(description = "报告编号")
    private String reportNo;

    @Schema(description = "状态：PROCESSING")
    private String status;

    @Schema(description = "预计生成时间（秒）")
    private Integer estimatedTime;
}
