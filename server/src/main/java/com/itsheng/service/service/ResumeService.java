package com.itsheng.service.service;

import com.itsheng.pojo.vo.ResumeUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    /**
     * 上传简历
     * @param file 简历文件（PDF/DOCX/PPTX/HTML/TXT），最大 10MB
     * @return 上传结果 VO
     */
    ResumeUploadVO upload(MultipartFile file);
}
