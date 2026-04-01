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
}
