package com.zmh.atlantic.coffee.ai.session;

import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.ai.route.SessionRouter;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.common.exception.BizException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天接口（总体设计 §5.2 聊天域 + §5.3 SSE 契约）。
 * SSE 超时三维度对齐（决策 #54）：SseEmitter(300_000L) ↔ Nginx 300s ↔ spring.mvc.async 300000。
 * SSE 建立前的鉴权/参数/状态错误走标准 HTTP 业务码（§5.5 边界），建立后走 error 事件。
 */
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService chatSessionService;
    private final SessionRouter sessionRouter;
    private final ChatRequestGate chatRequestGate;

    @PostMapping("/sessions")
    public Result<ChatSessionService.SessionView> create() {
        return Result.ok(chatSessionService.create(UserContext.requireUserId()));
    }

    @GetMapping("/sessions")
    public Result<CursorPage<ChatSessionService.SessionView>> list(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(chatSessionService.list(UserContext.requireUserId(), cursor, limit));
    }

    @PostMapping("/sessions/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        chatSessionService.close(id, UserContext.requireUserId());
        return Result.ok();
    }

    @GetMapping("/sessions/{id}/messages")
    public Result<CursorPage<ChatSessionService.MessageView>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = UserContext.requireUserId();
        chatSessionService.getOwned(id, userId);
        return Result.ok(chatSessionService.messages(id, cursor, limit));
    }

    /** 发送消息，返回 text/event-stream（SSE 六事件：intent/tool_start/tool_end/delta/done/error）。 */
    @PostMapping(value = "/sessions/{id}/messages", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter send(@PathVariable Long id, @Valid @RequestBody ChatDtos.SendMessageRequest request) {
        Long userId = UserContext.requireUserId();
        ChatSession session = chatSessionService.getOwned(id, userId);
        if ("ENDED".equals(session.getStatus())) {
            throw new BizException(ResultCode.SESSION_ENDED);
        }
        chatRequestGate.acquire(userId);                       // 42901：单用户并发 1 路（SSE 建立前）
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onCompletion(() -> chatRequestGate.release(userId));
        emitter.onTimeout(() -> chatRequestGate.release(userId));
        emitter.onError(e -> chatRequestGate.release(userId));
        sessionRouter.route(session, request.content(), emitter);
        return emitter;
    }
}
