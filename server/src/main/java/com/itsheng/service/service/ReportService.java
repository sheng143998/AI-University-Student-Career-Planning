package com.itsheng.service.service;

import com.itsheng.pojo.dto.ReportGenerateDTO;
import com.itsheng.pojo.dto.ReportUpdateDTO;
import com.itsheng.pojo.vo.ReportDetailVO;
import com.itsheng.pojo.vo.ReportGenerateVO;
import com.itsheng.pojo.vo.ReportSummaryVO;

public interface ReportService {

    ReportGenerateVO generateReport(Long userId, ReportGenerateDTO dto);

    ReportSummaryVO getLatestReport(Long userId);

    ReportDetailVO getReportDetail(Long userId, Long reportId);

    boolean updateReport(Long userId, Long reportId, ReportUpdateDTO dto);

    boolean deleteReport(Long userId, Long reportId);

    byte[] downloadReportPdf(Long userId, Long reportId);
}
