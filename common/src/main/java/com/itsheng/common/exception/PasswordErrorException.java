package com.itsheng.common.exception;

//密码错误异常
public class PasswordErrorException extends BaseException {

    public PasswordErrorException() {
    }

    public PasswordErrorException(String msg) {
        super(msg);
    }

    public PasswordErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
