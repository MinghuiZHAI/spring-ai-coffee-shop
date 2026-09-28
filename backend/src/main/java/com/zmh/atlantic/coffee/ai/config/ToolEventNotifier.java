package com.zmh.atlantic.coffee.ai.config;

/**
 * 工具事件通知接口：ObservableToolCallingManager 在真实 toolCallId 可见的唯一位置
 * （工具执行环）发出 tool_start / tool_end，由 AiSessionHandler 的实现转发为 SSE 事件
 * 并汇总工具调用摘要（总体设计 §5.3：按 toolCallId 配对 start/end）。
 *
 * <p>为什么不在外层流观测：DashScopeChatModel 默认内联执行工具（internalToolExecutionEnabled），
 * 工具调用/响应块不会透传到 ChatClient 的外层 Flux——唯一同时能看到请求 tool_call id 与
 * 执行结果的拦截点是 ToolCallingManager。</p>
 */
public interface ToolEventNotifier {

    /** 模型发起工具调用（执行前）：toolCallId/参数来自模型返回的 tool_call 协议字段。 */
    void onStart(String toolCallId, String tool, String arguments);

    /** 工具执行结束（成功或失败）：toolCallId 与 start 同源，前端按 id 配对。 */
    void onEnd(String toolCallId, String tool, boolean success, int durationMs);
}
