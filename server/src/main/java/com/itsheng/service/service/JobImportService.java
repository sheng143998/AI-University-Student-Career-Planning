package com.itsheng.service.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 岗位导入 Service 接口
 */
public interface JobImportService {

    /**
     * 从 Excel 文件导入岗位数据
     * @param file Excel 文件
     */
    void importFromExcel(MultipartFile file);

    /**
     * AI 智能分类导入岗位数据
     * 遍历 recruitment_data 表中的所有招聘数据（约 1 万条），调用 AI 进行语义分析，
     * 将岗位分为 51 种类型，并对每种类型的岗位进行级别细分（实习/初级/中级/高级），
     * 最终将分类结果聚合写入 job 表（约 100 多条记录）。
     */
    void classifyAndImportJobs();

    /**
     * 整合 job 表中 job_category_code 字段
     * 检测编码字段，若没有级别后缀（INTERNSHIP/JUNIOR/MID/SENIOR），
     * 通过 job_level 字段给它加上等级后缀。
     * 若更新后发现与已有记录重复，则合并数据（薪资范围、source_job_ids、skills）并删除原记录。
     */
    void consolidateJobCategoryCodes();
}
