package com.zmh.atlantic.coffee.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 订单售后 Agent：持有专属 ChatClient（Prompt/工具/RAG 过滤差异在 AiClientConfig 装配）。
 * conversationId + toolContext 由 AiSessionHandler 调用级成对注入（决策 #55），Agent 不持有会话态。
 */
@Component
public class OrderAgent {

    private final ChatClient chatClient;

    public OrderAgent(@Qualifier("orderAgentClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatClient client() {
        return chatClient;
    }
}
