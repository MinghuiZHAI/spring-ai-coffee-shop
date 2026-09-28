package com.zmh.atlantic.coffee.ai.session;

import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单用户聊天并发 1 路限流（总体设计 §5.5 42901）：SSE 建立前校验，
 * 超限直接以标准 HTTP 业务错误返回（§5.5 边界：SSE 建立前的错误走标准错误码）。
 * 释放由 emitter 生命周期回调（onCompletion/onError/onTimeout）触发，可重入。
 */
@Component
public class ChatRequestGate {

    private final Set<Long> activeUsers = ConcurrentHashMap.newKeySet();

    public void acquire(Long userId) {
        if (!activeUsers.add(userId)) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "上一条消息还在回复中，请稍候");
        }
    }

    public void release(Long userId) {
        activeUsers.remove(userId);
    }
}
