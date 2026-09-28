package com.zmh.atlantic.coffee.ai.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 发送包装：统一序列化（事件类型放 JSON 的 type 字段，总体设计 §5.3 写法 A——
 * fetch+ReadableStream 一次 JSON.parse 即可拿到全部信息）；客户端断开（send 抛异常）后
 * 置 closed 静默丢弃后续事件，避免流管线向已死连接写数据。
 */
@Slf4j
public final class SseSender {

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private volatile boolean closed;

    public SseSender(SseEmitter emitter, ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
    }

    public synchronized void send(Object payload) {
        if (closed) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            closed = true;
            log.debug("SSE 发送失败（客户端可能已断开）: {}", String.valueOf(e.getMessage()));
        }
    }

    /** 正常结束流：SSE 建立后的错误也以 error 事件呈现并正常 complete（总体设计 §3.2 第 10 步）。 */
    public synchronized void complete() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            emitter.complete();
        } catch (Exception e) {
            log.debug("SSE complete 失败: {}", String.valueOf(e.getMessage()));
        }
    }
}
