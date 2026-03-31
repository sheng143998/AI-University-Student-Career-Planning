package com.itsheng.pojo.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 岗位 Excel 行数据映射类
 * 用于 EasyExcel 解析 Excel 文件
 */
@Data
public class JobExcelRow {

    /**
     * 岗位名称
     */
    @ExcelProperty("岗位名称")
    private String jobName;

    /**
     * 工作地址
     */
    @ExcelProperty("地址")
    private String address;

    /**
     * 薪资范围
     */
    @ExcelProperty("薪资范围")
    private String salaryRange;

    /**
     * 公司名称
     */
    @ExcelProperty("公司名称")
    private String companyName;

    /**
     * 所属行业
     */
    @ExcelProperty("所属行业")
    private String industry;

    /**
     * 公司规模
     */
    @ExcelProperty("公司规模")
    private String companySize;

    /**
     * 公司类型
     */
    @ExcelProperty("公司类型")
    private String companyType;

    /**
     * 岗位编码
     */
    @ExcelProperty("岗位编码")
    private String jobCode;

    /**
     * 岗位详情
     */
    @ExcelProperty("岗位详情")
    private String jobDetail;

    /**
     * 更新日期
     */
    @ExcelProperty("更新日期")
    private String updateDate;

    /**
     * 公司详情
     */
    @ExcelProperty("公司详情")
    private String companyDetail;

    /**
     * 岗位来源地址
     */
    @ExcelProperty("岗位来源地址")
    private String sourceUrl;
}
