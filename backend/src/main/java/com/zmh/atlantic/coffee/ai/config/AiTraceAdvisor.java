package com.zmh.atlantic.coffee.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * AI 链路日志 Advisor（详细设计 §3.1 ③，order=40 最后装配）：业务 Agent 专用，
 * 聚合流末尾记录 prompt 字符量、回复字符量与耗时，logger 名 ai.log（面试排障素材）。
 * 业务 Agent 仅走 stream()，故只实现 StreamAdvisor。
 */
@Slf4j(topic = "ai.log")
@Component
public class AiTraceAdvisor implements StreamAdvisor {

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public String getName() {
        return "AiTraceAdvisor";
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.currentTimeMillis();
        int promptChars = request.prompt().getInstructions().stream()
                .mapToInt(m -> m.getText() == null ? 0 : m.getText().length()).sum();
        return chain.nextStream(request)
                .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(flux, response -> {
                    int responseChars = response.chatResponse() == null || response.chatResponse().getResult() == null
                            ? 0 : String.valueOf(response.chatResponse().getResult().getOutput().getText()).length();
                    log.info("agent trace: promptChars={} responseChars={} durationMs={}",
                            promptChars, responseChars, System.currentTimeMillis() - start);
                }));
    }
}
