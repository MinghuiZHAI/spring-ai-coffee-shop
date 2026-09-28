package com.zmh.atlantic.coffee.ai.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 会员权益 Agent（积分/优惠券/等级）。调用级注入范式同 {@link OrderAgent}。 */
@Component
public class CouponAgent {

    private final ChatClient chatClient;

    public CouponAgent(@Qualifier("couponAgentClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatClient client() {
        return chatClient;
    }
}
