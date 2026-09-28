package com.zmh.atlantic.coffee.ai.route;

import com.zmh.atlantic.coffee.ai.session.ChatSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 会话级服务方路由（总体设计 §3.1 双层路由的外层）：按 chat_session.status 决定本条消息
 * 交给 AI 链路（AiSessionHandler）还是人工坐席链路（M3 预留，届时新增实现类不改主入口）。
 * 与 IntentAgent（AI 分支内部的意图路由）职责正交。
 */
public interface SessionRouter {

    void route(ChatSession session, String content, SseEmitter emitter);
}
