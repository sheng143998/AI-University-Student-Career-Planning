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
}
