package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "生成职业报告请求")
public class ReportGenerateDTO {

    @Schema(description = "目标岗位画像 ID，不传则使用最匹配岗位")
    private Long targetJobProfileId;

    @Schema(description = "职业偏好")
    private CareerPreference careerPreference;

    @Data
    @Schema(description = "职业偏好")
    public static class CareerPreference {
        @Schema(description = "偏好城市", example = "深圳")
        private String preferredCity;

        @Schema(description = "期望薪资", example = "15-25k")
        private String expectedSalary;

        @Schema(description = "职业方向偏好", example = "技术路线")
        private String careerDirection;
    }
}
