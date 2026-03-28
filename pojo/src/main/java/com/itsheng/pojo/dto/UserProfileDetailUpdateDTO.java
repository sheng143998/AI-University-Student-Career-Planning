package com.itsheng.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户详细档案更新 DTO
 * 对应接口文档 2.4 更新详细档案的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDetailUpdateDTO {

    /**
     * 教育经历数组
     */
    private List<EducationItem> education;

    /**
     * 工作经历数组
     */
    private List<ExperienceItem> experience;

    /**
     * 技能列表
     */
    private List<String> skills;

    /**
     * 项目经历数组
     */
    private List<ProjectItem> projects;

    /**
     * 教育经历项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationItem {
        /**
         * 学校名称
         */
        private String school;

        /**
         * 专业
         */
        private String major;

        /**
         * 学历（学士/硕士/博士）
         */
        private String degree;

        /**
         * 时间段（如"2018-2022"）
         */
        private String period;
    }

    /**
     * 工作经历项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceItem {
        /**
         * 公司名称
         */
        private String company;

        /**
         * 职位
         */
        private String position;

        /**
         * 时间段
         */
        private String period;

        /**
         * 工作描述
         */
        private String description;
    }

    /**
     * 项目经历项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectItem {
        /**
         * 项目名称
         */
        private String name;

        /**
         * 项目链接
         */
        private String link;

        /**
         * 技术栈
         */
        private List<String> techStack;
    }
}
