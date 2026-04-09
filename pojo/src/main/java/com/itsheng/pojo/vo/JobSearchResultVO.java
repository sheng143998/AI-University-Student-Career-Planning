package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 岗位搜索结果（对齐前端 website/src/api/roadmap.ts 的 JobSearchResult）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchResultVO {

    private Long id;
    private String jobName;
    private String industry;
    private String salaryRange;
    private Double similarityScore;
}
