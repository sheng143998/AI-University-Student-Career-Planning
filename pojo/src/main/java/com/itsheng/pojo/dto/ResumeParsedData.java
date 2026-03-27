package com.itsheng.pojo.dto;

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
    private String name;

    /**
     * 求职意向/目标岗位
     */
    private String targetRole;

    /**
     * 技能列表
     */
    private List<String> skills;

    /**
     * 工作年限
     */
    private Integer experienceYears;

    /**
     * 教育经历
     */
    private List<Education> education;

    /**
     * 工作/项目经历
     */
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
        private String school;

        /**
         * 专业
         */
        private String major;

        /**
         * 学历：学士/硕士/博士
         */
        private String degree;

        /**
         * 时间：2018-2022
         */
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
        private String company;

        /**
         * 职位
         */
        private String position;

        /**
         * 时间
         */
        private String period;

        /**
         * 描述
         */
        private String description;
    }
}
