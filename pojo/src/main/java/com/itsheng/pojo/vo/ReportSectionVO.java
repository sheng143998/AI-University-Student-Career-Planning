package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "报告章节")
public class ReportSectionVO {

    @Schema(description = "章节 key：self_discovery/match_analysis/career_goal/development_path/action_plan")
    private String key;

    @Schema(description = "章节标题")
    private String title;

    @Schema(description = "章节内容")
    private Map<String, Object> content;
}
