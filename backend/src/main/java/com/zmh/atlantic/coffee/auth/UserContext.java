package com.zmh.atlantic.coffee.auth;

import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;

/**
 * 当前请求的用户上下文：由 JwtAuthenticationFilter 在校验 access token 后写入，
 * 请求结束（finally）统一清理。注意：@Tool 方法在模型回调线程执行时 ThreadLocal 失效，
 * 工具身份必须走 ToolContext（详细设计 v1.2 §3.3 / 决策 #53）。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BizException(ResultCode.FORBIDDEN, "缺少用户上下文");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
