package com.itsheng.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历解析数据结构 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeParsedData {
    /**
     * 姓名
     */
    @JsonProperty("name")
    private String name;

    /**
     * 求职意向/目标岗位
     */
    @JsonProperty("target_role")
    private String targetRole;

    /**
     * 期望工作地点
     */
    @JsonProperty("location")
    private String location;

    /**
     * 当前职位/最近职位
     */
    @JsonProperty("current_role")
    private String currentRole;

    /**
     * 技能列表
     */
    @JsonProperty("skills")
    private List<String> skills;

    /**
     * 工作年限
     */
    @JsonProperty("experience_years")
    private Integer experienceYears;

    /**
     * 人岗匹配度（0-100）
     */
    @JsonProperty("match_score")
    private Integer matchScore;

    /**
     * 教育经历
     */
    @JsonProperty("education")
    private List<Education> education;

    /**
     * 工作/项目经历
     */
    @JsonProperty("experience")
    private List<Experience> experience;

    /**
     * 教育经历 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        /**
         * 学校名称
         */
        @JsonProperty("school")
        private String school;

        /**
         * 专业
         */
        @JsonProperty("major")
        private String major;

        /**
         * 学历：学士/硕士/博士
         */
        @JsonProperty("degree")
        private String degree;

        /**
         * 时间：2018-2022
         */
        @JsonProperty("period")
        private String period;
    }

    /**
     * 工作/项目经历 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        /**
         * 公司/项目名称
         */
        @JsonProperty("company")
        private String company;

        /**
         * 职位
         */
        @JsonProperty("position")
        private String position;

        /**
         * 时间
         */
        @JsonProperty("period")
        private String period;

        /**
         * 描述
         */
        @JsonProperty("description")
        private String description;
    }
}
