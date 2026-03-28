package com.itsheng.common.exception;

// 简历分析异常
public class ResumeAnalysisException extends BaseException {

    public ResumeAnalysisException() {
    }

    public ResumeAnalysisException(String msg) {
        super(msg);
    }

    public ResumeAnalysisException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
