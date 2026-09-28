package com.zmh.atlantic.coffee.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 饮品推荐 Agent。调用级注入范式同 {@link OrderAgent}。 */
@Component
public class RecommendAgent {

    private final ChatClient chatClient;

    public RecommendAgent(@Qualifier("recommendAgentClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatClient client() {
        return chatClient;
    }
}
