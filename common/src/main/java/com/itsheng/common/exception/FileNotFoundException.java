package com.itsheng.common.exception;

// 文件找不到异常
public class FileNotFoundException extends BaseException {

    public FileNotFoundException() {
    }

    public FileNotFoundException(String msg) {
        super(msg);
    }

    public FileNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
