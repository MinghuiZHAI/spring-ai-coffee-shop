package com.zmh.atlantic.coffee.ai.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmh.atlantic.coffee.ai.agent.ClassifyOutcome;
import com.zmh.atlantic.coffee.ai.agent.CouponAgent;
import com.zmh.atlantic.coffee.ai.agent.FaqAgent;
import com.zmh.atlantic.coffee.ai.agent.Intent;
import com.zmh.atlantic.coffee.ai.agent.IntentAgent;
import com.zmh.atlantic.coffee.ai.agent.OrderAgent;
import com.zmh.atlantic.coffee.ai.agent.RecommendAgent;
import com.zmh.atlantic.coffee.ai.config.ObservableToolCallingManagerConfig;
import com.zmh.atlantic.coffee.ai.config.ToolEventNotifier;
import com.zmh.atlantic.coffee.ai.log.MessageIdGenerator;
import com.zmh.atlantic.coffee.ai.memory.MessageCodec;
import com.zmh.atlantic.coffee.ai.prompt.Prompts;
import com.zmh.atlantic.coffee.ai.session.ChatSession;
import com.zmh.atlantic.coffee.ai.session.ChatSessionService;
import com.zmh.atlantic.coffee.ai.session.ChatMessageMapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MVP 唯一 AI 分支（总体设计 §3.1/§3.2 十步时序的实现侧）：会话元数据维护 →
 * IntentAgent 意图分类（SSE intent 事件）→ 按意图路由业务 Agent 流式生成
 * （SSE tool_start/tool_end/delta 事件）→ 收尾落 done；FALLBACK 直出兜底；
 * 异常路径推 error 事件（retryable）并正常结束流。
 */
@Slf4j
@Component
public class AiSessionHandler implements SessionRouter {

    static final String AI_UPSTREAM_ERROR_MESSAGE = "服务暂时不可用，请稍后再试";

    private final IntentAgent intentAgent;
    private final OrderAgent orderAgent;
    private final CouponAgent couponAgent;
    private final RecommendAgent recommendAgent;
    private final FaqAgent faqAgent;
    private final ChatMemory chatMemory;
    private final ChatSessionService chatSessionService;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;
    private final MessageIdGenerator messageIdGenerator;

    public AiSessionHandler(IntentAgent intentAgent, OrderAgent orderAgent, CouponAgent couponAgent,
                            RecommendAgent recommendAgent, FaqAgent faqAgent, ChatMemory chatMemory,
                            ChatSessionService chatSessionService, ChatMessageMapper chatMessageMapper,
                            ObjectMapper objectMapper, MessageIdGenerator messageIdGenerator) {
        this.intentAgent = intentAgent;
        this.orderAgent = orderAgent;
        this.couponAgent = couponAgent;
        this.recommendAgent = recommendAgent;
        this.faqAgent = faqAgent;
        this.chatMemory = chatMemory;
        this.chatSessionService = chatSessionService;
        this.chatMessageMapper = chatMessageMapper;
        this.objectMapper = objectMapper;
        this.messageIdGenerator = messageIdGenerator;
    }

    @Override
    public void route(ChatSession session, String content, SseEmitter emitter) {
        if (!"AI_SERVING".equals(session.getStatus())) {
            // HUMAN_SERVING 分支 M3 启用；MVP 无任何路径能到达该状态
            throw new BizException(ResultCode.SESSION_ENDED, "会话状态不支持当前操作");
        }
        SseSender sse = new SseSender(emitter, objectMapper);
        long messageId = messageIdGenerator.next();
        Thread.startVirtualThread(() -> run(session, content, sse, messageId));
    }

    // ===== 主流程（虚拟线程内执行：意图分类为阻塞 LLM 调用） =====

    private void run(ChatSession session, String content, SseSender sse, long messageId) {
        try {
            chatSessionService.touch(session.getId());
            chatSessionService.initTitleIfBlank(session.getId(), session.getTitle(), content);
            ClassifyOutcome outcome = intentAgent.classify(messageId, content);
            sse.send(Map.of("type", "intent", "value", outcome.intent().name(),
                    "confidence", Math.round(outcome.confidence() * 100) / 100.0));
            if (outcome.intent() == Intent.FALLBACK) {
                fallback(session, content, messageId, sse);
            } else {
                stream(session, content, messageId, resolve(outcome.intent()), sse);
            }
        } catch (Exception e) {
            log.error("AI 会话处理失败: sessionId={}", session.getId(), e);
            sse.send(Map.of("type", "error", "message", AI_UPSTREAM_ERROR_MESSAGE, "retryable", true));
            sse.complete();
        }
    }

    /** FALLBACK 直出兜底（决策 #40）：不跑业务 Agent，消息经同一仓储走快照替换落库。 */
    private void fallback(ChatSession session, String content, long messageId, SseSender sse) {
        chatMemory.add(String.valueOf(session.getId()),
                List.of(turnKeyedUserMessage(content, messageId),
                        new AssistantMessage(Prompts.FALLBACK_REPLY)));
        sse.send(Map.of("type", "delta", "content", Prompts.FALLBACK_REPLY));
        sse.send(Map.of("type", "done", "messageId", latestAiMessageId(session.getId())));
        sse.complete();
    }

    // ===== 业务 Agent 流式生成 =====

    private void stream(ChatSession session, String content, long messageId, ChatClient client, SseSender sse) {
        // 工具事件通知器：ObservableToolCallingManager 在工具执行环发 tool_start/tool_end（真实 toolCallId）。
        // 工具摘要的持久化不经此处（#66：tool_call_log 由 ToolCallLogger 同步写，历史按轮次键关联）。
        ToolEventNotifier notifier = new ToolEventNotifier() {
            @Override
            public void onStart(String toolCallId, String tool, String arguments) {
                sse.send(Map.of("type", "tool_start", "toolCallId", toolCallId,
                        "tool", tool, "args", arguments));
            }

            @Override
            public void onEnd(String toolCallId, String tool, boolean success, int durationMs) {
                sse.send(Map.of("type", "tool_end", "toolCallId", toolCallId,
                        "tool", tool, "success", success, "durationMs", durationMs));
            }
        };
        client.prompt()
                .messages(turnKeyedUserMessage(content, messageId))                      // 注入③：轮次键随消息进记忆（决策 #66）
                .toolContext(Map.of("userId", session.getUserId(),                      // 注入①：工具身份（决策 #53）
                        "messageId", messageId, "sessionId", session.getId(),
                        ObservableToolCallingManagerConfig.NOTIFIER_KEY, notifier))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(session.getId()))) // 注入②：记忆定位（决策 #55）
                .stream()
                .chatResponse()
                .doOnNext(chunk -> observe(chunk, sse))
                .doOnError(e -> {
                    log.error("业务 Agent 流式生成失败: sessionId={}", session.getId(), e);
                    sse.send(Map.of("type", "error", "message", AI_UPSTREAM_ERROR_MESSAGE, "retryable", true));
                    sse.complete();
                })
                .doOnComplete(() -> finish(session, sse))
                .subscribe();
    }

    /** 流块观测：文本块即打字机 delta（工具块被模型内联执行，不经外层流——事件走 ToolEventNotifier）。 */
    private void observe(ChatResponse chunk, SseSender sse) {
        if (chunk == null || chunk.getResults() == null) {
            return;
        }
        for (Generation generation : chunk.getResults()) {
            if (generation == null || generation.getOutput() == null) {
                continue;
            }
            String text = generation.getOutput().getText();
            if (text != null && !text.isEmpty()) {
                sse.send(Map.of("type", "delta", "content", text));
            }
        }
    }

    private void finish(ChatSession session, SseSender sse) {
        Long aiMessageId = latestAiMessageId(session.getId());
        // 工具摘要不再回填 chat_message（#66：快照替换下无法幸存，历史接口按轮次键查 tool_call_log）
        sse.send(Map.of("type", "done", "messageId", aiMessageId == null ? 0 : aiMessageId));
        sse.complete();
    }

    /** 本轮 USER 消息携带轮次键（#66）：MessageChatMemoryAdvisor.before() 原样入记忆，
     * 仓储组装行时 USER 行取自身 metadata、AI 行由紧邻 USER 行推导；tool_call_log.message_id 同源。 */
    private UserMessage turnKeyedUserMessage(String content, long messageId) {
        return UserMessage.builder()
                .text(content)
                .metadata(Map.of(MessageCodec.MESSAGE_ID_KEY, messageId))
                .build();
    }

    // ===== 内部 =====

    /** 意图 → 业务 Agent（总体设计 §3.2 第 6 步）。 */
    private ChatClient resolve(Intent intent) {
        return switch (intent) {
            case ORDER -> orderAgent.client();
            case MEMBER -> couponAgent.client();
            case RECOMMEND -> recommendAgent.client();
            case KNOWLEDGE -> faqAgent.client();
            case FALLBACK -> throw new IllegalStateException("FALLBACK 不应进入业务 Agent");
        };
    }

    private Long latestAiMessageId(Long sessionId) {
        return chatMessageMapper.findLatestAiId(sessionId);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
