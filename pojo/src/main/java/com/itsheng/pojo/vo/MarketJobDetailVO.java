package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketJobDetailVO {
    private Long id;
    private String jobName;
    private String industrySegment;
    private String description;
    private List<String> cities;
    private MarketSalarySnapshotVO salaryRange;
    private MarketExperienceRangeVO experienceRange;
    private String educationRequirement;
    private List<String> requiredSkills;  // 来自job表的required_skills（字符串列表）
    private List<MarketSkillRequirementVO> coreSkills;
    private MarketCapabilityRequirementsVO capabilityRequirements;
    private List<SoftSkillItemVO> softSkills;  // 软技能详情（带描述和证据）
    private List<String> certificateRequirements;
    private List<String> companyBenefits;
    private MarketCareerPathVO careerPath;
    private MarketDemandAnalysisVO demandAnalysis;
    private String updatedAt;
}
