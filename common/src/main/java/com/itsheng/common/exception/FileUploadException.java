package com.itsheng.common.exception;

// 文件上传异常
public class FileUploadException extends BaseException {

    public FileUploadException() {
    }

    public FileUploadException(String msg) {
        super(msg);
    }

    public FileUploadException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
