package com.zmh.atlantic.coffee.common.web;

/**
 * 错误码段位（总体设计 §5.5）：4xxxx 业务 / 5xxxx 系统。
 * 40901 状态机冲突、40902 会话已结束为高频语义码；50002 为百炼上游异常。
 */
public enum ResultCode {
    OK(0, "ok"),
    PARAM_INVALID(40001, "参数校验失败"),
    TOKEN_INVALID(40101, "token 缺失或过期"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    STATE_CONFLICT(40901, "状态机冲突"),
    SESSION_ENDED(40902, "会话已结束"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁"),
    SYSTEM_ERROR(50001, "系统异常"),
    AI_UPSTREAM_ERROR(50002, "AI 上游异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
