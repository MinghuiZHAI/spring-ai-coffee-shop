package com.zmh.atlantic.coffee.common.exception;

import com.zmh.atlantic.coffee.common.web.ResultCode;

/**
 * 业务异常：携带语义错误码，由 GlobalExceptionHandler 统一转为 Result 结构。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
