package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 职业地图节点详情 VO
 */
@Data
public class RoadmapNodeDetailVO {

    /**
     * 节点 ID
     */
    private String id;

    /**
     * 岗位标题
     */
    private String title;

    /**
     * 岗位描述
     */
    private String summary;

    /**
     * 技能要求列表
     */
    private List<String> requirements;

    /**
     * 推荐技能列表
     */
    private List<String> recommendedSkills;

    /**
     * 薪资范围
     */
    private String salaryRange;

    /**
     * 经验要求
     */
    private String experienceYears;

    /**
     * 岗位级别
     */
    private String level;

    /**
     * 岗位级别名称
     */
    private String levelName;
}
