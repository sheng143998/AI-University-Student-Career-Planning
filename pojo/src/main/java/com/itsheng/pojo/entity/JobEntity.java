package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 岗位原始数据实体
 * 对应数据库表：recruitment_data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 岗位名称
     */
    private String jobName;

    /**
     * 工作地址
     */
    private String address;

    /**
     * 薪资范围 (支持按天/按月/年薪等格式)
     */
    private String salaryRange;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 所属行业
     */
    private String industry;

    /**
     * 工作城市
     */
    private String city;

    /**
     * 公司规模
     */
    private String companySize;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 公司性质
     */
    private String companyNature;

    /**
     * 岗位编码
     */
    private String jobCode;

    /**
     * 岗位详情
     */
    private String jobDetail;

    /**
     * 岗位职责描述
     */
    private String jobDescription;

    /**
     * 任职要求
     */
    private String jobRequirements;

    /**
     * 公司详情
     */
    private String companyDetail;

    /**
     * 公司描述
     */
    private String companyDescription;

    /**
     * 更新日期
     */
    private String updateDate;

    /**
     * 岗位来源地址
     */
    private String sourceUrl;

    /**
     * 发布日期
     */
    private String publishDate;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
