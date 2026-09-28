package com.zmh.atlantic.coffee.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可观测工具执行器（总体设计 §3.2 第 8 步的 SSE tool_start/tool_end 数据源）：
 * 装饰 DefaultToolCallingManager——工具执行环是真实 toolCallId 唯一可见的位置。
 * 通知器经 toolContext 调用级传入（仅聊天流式调用携带；IntentAgent 等无工具调用不受影响）。
 * 该 bean 存在后 spring-ai 自动配置的 DefaultToolCallingManager 退避，DashScopeChatModel 注入本 bean。
 */
@Slf4j
@Configuration
public class ObservableToolCallingManagerConfig {

    /** 工具通知器在 toolContext 中的键。 */
    public static final String NOTIFIER_KEY = "toolEventNotifier";

    @Bean
    public ToolCallingManager toolCallingManager() {
        ToolCallingManager delegate = DefaultToolCallingManager.builder().build();
        return new ObservableToolCallingManager(delegate);
    }

    @Slf4j
    static class ObservableToolCallingManager implements ToolCallingManager {

        private final ToolCallingManager delegate;

        ObservableToolCallingManager(ToolCallingManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions options) {
            return delegate.resolveToolDefinitions(options);
        }

        @Override
        public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse response) {
            ToolEventNotifier notifier = notifier(prompt);
            Map<String, Long> startTimes = new ConcurrentHashMap<>();
            if (notifier != null) {
                for (AssistantMessage.ToolCall call : toolCallsOf(response)) {
                    startTimes.put(call.id(), System.currentTimeMillis());
                    notifier.onStart(call.id(), call.name(), call.arguments());
                }
            }
            try {
                ToolExecutionResult result = delegate.executeToolCalls(prompt, response);
                if (notifier != null) {
                    long now = System.currentTimeMillis();
                    for (ToolResponseMessage.ToolResponse r : responsesOf(result)) {
                        long start = startTimes.getOrDefault(r.id(), now);
                        notifier.onEnd(r.id(), r.name(), true, (int) (now - start));
                    }
                }
                return result;
            } catch (RuntimeException e) {
                if (notifier != null) {
                    long now = System.currentTimeMillis();
                    startTimes.forEach((id, start) -> notifier.onEnd(id, "", false, (int) (now - start)));
                }
                throw e;
            }
        }

        // ===== 内部 =====

        private ToolEventNotifier notifier(Prompt prompt) {
            if (prompt.getOptions() instanceof ToolCallingChatOptions options
                    && options.getToolContext() != null) {
                Object notifier = options.getToolContext().get(NOTIFIER_KEY);
                return notifier instanceof ToolEventNotifier n ? n : null;
            }
            return null;
        }

        private List<AssistantMessage.ToolCall> toolCallsOf(ChatResponse response) {
            return response.getResults().stream()
                    .map(generation -> generation.getOutput())
                    .filter(output -> output != null && output.hasToolCalls())
                    .flatMap(output -> output.getToolCalls().stream())
                    .toList();
        }

        private List<ToolResponseMessage.ToolResponse> responsesOf(ToolExecutionResult result) {
            List<Message> history = result.conversationHistory();
            if (history.isEmpty() || !(history.get(history.size() - 1) instanceof ToolResponseMessage trm)) {
                return List.of();
            }
            return trm.getResponses();
        }
    }
}
