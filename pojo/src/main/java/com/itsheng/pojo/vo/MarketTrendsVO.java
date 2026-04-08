package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketTrendsVO {
    private Long jobProfileId;
    private String jobName;
    private String city;
    private String timeRange;
    private MarketSalaryTrendVO salary;
    private MarketDemandTrendVO demand;
    private List<MarketHotSkillVO> hotSkills;
    private String updatedAt;
}
