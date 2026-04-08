package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketProfileItemVO {
    private Long id;
    private String jobName;
    private String industrySegment;
    private String city;
    private MarketSalaryRangeVO salaryRange;
    private MarketExperienceRangeVO experienceRange;
    private List<String> coreSkills;
    private List<String> certificateRequirements;
    private MarketCapabilityRequirementsVO capabilityRequirements;
    private String demandLevel;
    private String updatedAt;
}
