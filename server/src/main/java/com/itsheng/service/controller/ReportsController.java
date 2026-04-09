package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.ReportGenerateDTO;
import com.itsheng.pojo.dto.ReportUpdateDTO;
import com.itsheng.pojo.vo.ReportDetailVO;
import com.itsheng.pojo.vo.ReportGenerateVO;
import com.itsheng.pojo.vo.ReportSummaryVO;
import com.itsheng.service.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "职业发展报告接口")
public class ReportsController {

    private final ReportService reportService;

    @PostMapping("/generate")
    @Operation(summary = "生成职业报告")
    public Result<ReportGenerateVO> generate(@RequestBody(required = false) ReportGenerateDTO dto) {
        Long userId = BaseContext.getUserId();
        log.info("生成职业报告，用户ID: {}", userId);
        ReportGenerateVO result = reportService.generateReport(userId, dto);
        return Result.success(result);
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新报告")
    public Result<ReportSummaryVO> latest() {
        Long userId = BaseContext.getUserId();
        log.info("获取最新报告，用户ID: {}", userId);
        ReportSummaryVO result = reportService.getLatestReport(userId);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取报告详情")
    public Result<ReportDetailVO> detail(@PathVariable Long id) {
        Long userId = BaseContext.getUserId();
        log.info("获取报告详情，报告ID: {}, 用户ID: {}", id, userId);
        ReportDetailVO result = reportService.getReportDetail(userId, id);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新报告内容")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ReportUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        log.info("更新报告内容，报告ID: {}, 用户ID: {}", id, userId);
        boolean result = reportService.updateReport(userId, id, dto);
        return Result.success(result);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载职业报告 PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Long userId = BaseContext.getUserId();
        log.info("下载报告 PDF，报告ID: {}, 用户ID: {}", id, userId);
        byte[] pdfData = reportService.downloadReportPdf(userId, id);
        String encodedFilename = URLEncoder.encode("职业生涯发展报告.pdf", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(pdfData);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public Result<Boolean> delete(@PathVariable Long id) {
        Long userId = BaseContext.getUserId();
        log.info("删除报告，报告ID: {}, 用户ID: {}", id, userId);
        boolean result = reportService.deleteReport(userId, id);
        return Result.success(result);
    }
}
