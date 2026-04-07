package com.itsheng.common.exception;

//用户名已存在异常
public class UsernameExistException extends BaseException {

    public UsernameExistException() {
    }

    public UsernameExistException(String msg) {
        super(msg);
    }

    public UsernameExistException(String msg, Throwable cause) {
        super(msg, cause);
    }

}