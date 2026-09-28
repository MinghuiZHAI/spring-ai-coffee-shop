package com.zmh.atlantic.coffee.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 通用知识 Agent（全库检索；意图降级与超时的兜底承接方）。调用级注入范式同 {@link OrderAgent}。 */
@Component
public class FaqAgent {

    private final ChatClient chatClient;

    public FaqAgent(@Qualifier("faqAgentClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatClient client() {
        return chatClient;
    }
}
