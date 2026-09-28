package com.zmh.atlantic.coffee.common.exception;

import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（总体设计 §5.1）。
 * 业务错误统一 HTTP 200 + 语义码；401 由 Security 过滤器层直接处理、不进本处理器；
 * SSE 建立后的错误走 error 事件（总体设计 §5.5 边界）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Result<Void> handleInvalidParam(Exception e) {
        return Result.fail(ResultCode.PARAM_INVALID);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
