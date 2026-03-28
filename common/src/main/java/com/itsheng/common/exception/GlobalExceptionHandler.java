package com.itsheng.common.exception;

import com.itsheng.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e) {
        log.error("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理账号不存在异常
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public Result<Void> handleAccountNotFoundException(AccountNotFoundException e) {
        log.error("账号不存在：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理密码错误异常
     */
    @ExceptionHandler(PasswordErrorException.class)
    public Result<Void> handlePasswordErrorException(PasswordErrorException e) {
        log.error("密码错误：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理文件上传异常
     */
    @ExceptionHandler(FileUploadException.class)
    public Result<Void> handleFileUploadException(FileUploadException e) {
        log.error("文件上传异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理文件找不到异常
     */
    @ExceptionHandler(FileNotFoundException.class)
    public Result<Void> handleFileNotFoundException(FileNotFoundException e) {
        log.error("文件找不到：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理简历分析异常
     */
    @ExceptionHandler(ResumeAnalysisException.class)
    public Result<Void> handleResumeAnalysisException(ResumeAnalysisException e) {
        log.error("简历分析异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error("系统繁忙，请稍后再试");
    }
}
