package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketHotJobItemVO {
    private Long id;
    private String jobName;
    private String industrySegment;
    private String city;
    private String tag;
    private MarketSalaryRangeVO salaryRange;
    private String demandLevel;
    private List<String> highlights;
    private List<String> coreSkills;
    private Double growthRate;
    private String icon;
}
