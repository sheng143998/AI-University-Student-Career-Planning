package com.itsheng.service.controller;

import com.itsheng.common.result.Result;
import com.itsheng.service.service.JobImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 岗位导入 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/job")
@Tag(name = "岗位管理", description = "岗位数据导入与相关接口")
@RequiredArgsConstructor
public class JobImportController {

    private final JobImportService jobImportService;

    /**
     * 从 Excel 导入岗位数据
     * @param file Excel 文件，支持.xls/.xlsx 格式，最大 50MB
     * @return 导入结果
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "导入岗位数据",
            description = "上传 Excel 文件导入岗位数据，支持.xls/.xlsx 格式，最大 50MB。" +
                    "Excel 表头应包含：岗位名称、地址、薪资范围、公司名称、所属行业、公司规模、公司类型、" +
                    "岗位编码、岗位详情、更新日期、公司详情、岗位来源地址。" +
                    "后端接收后异步处理向量化，可立即返回。"
    )
    public Result<String> importFromExcel(@RequestPart("file") MultipartFile file) {
        log.info("开始导入岗位 Excel 数据，文件名：{}", file.getOriginalFilename());

        jobImportService.importFromExcel(file);

        return Result.success("岗位数据导入成功，正在异步处理向量化");
    }

    /**
     * AI 智能分类导入岗位数据
     * @return 导入结果
     */
    @PostMapping("/classify-and-import")
    @Operation(
            summary = "AI 智能分类导入岗位",
            description = "遍历 recruitment_data 表中的所有招聘数据（约 1 万条），调用 AI 模型进行语义分析，" +
                    "将岗位分为 51 种类型，并对每种类型的岗位进行级别细分（实习/初级/中级/高级），" +
                    "最终将分类结果聚合写入 job 表（约 100 多条记录）。" +
                    "处理时间较长，请耐心等待响应。"
    )
    public Result<String> classifyAndImportJobs() {
        log.info("开始 AI 智能分类导入岗位数据");

        jobImportService.classifyAndImportJobs();

        return Result.success("岗位 AI 分类导入完成");
    }

    /**
     * 整合岗位类别编码
     * @return 整合结果
     */
    @PostMapping("/consolidate-category-codes")
    @Operation(
            summary = "整合岗位类别编码",
            description = "检测 job 表中所有记录的 job_category_code 字段，若没有级别后缀（INTERNSHIP/JUNIOR/MID/SENIOR），" +
                    "则根据 job_level 字段添加相应的级别后缀。" +
                    "若更新后的编码与已有记录重复，则合并两条记录的数据（薪资范围取最优值、source_job_ids 和 skills 合并），" +
                    "然后删除原记录。"
    )
    public Result<String> consolidateCategoryCodes() {
        log.info("开始整合岗位类别编码");

        jobImportService.consolidateJobCategoryCodes();

        return Result.success("岗位类别编码整合完成");
    }
}
