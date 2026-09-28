package com.zmh.atlantic.coffee.common.web;

/**
 * 统一响应体（总体设计 §5.1）：code=0 成功，非 0 见 ResultCode 错误码表。
 * 业务错误统一走 HTTP 200 + 语义码；SSE 建立后的错误走 error 事件，不经过本结构。
 */
public record Result<T>(int code, String message, T data) {

    public static Result<Void> ok() {
        return new Result<>(ResultCode.OK.getCode(), ResultCode.OK.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.OK.getCode(), ResultCode.OK.getMessage(), data);
    }

    public static Result<Void> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static Result<Void> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
