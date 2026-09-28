package com.zmh.atlantic.coffee.ai.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会话超时调度（总体设计 §3.2 第 10 步 / v1.3 修正 4，决策 #42 双保险之二）：
 * 每分钟按会话状态区分扫描——AI_SERVING 超 30 分钟未活跃置 ENDED，
 * HUMAN_SERVING 超 2 小时（M3 启用，MVP 无实际数据但规则先定）；ENDED 为终态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionTimeoutTask {

    private final ChatSessionMapper chatSessionMapper;

    @Value("${atlantic.chat.idle-minutes.ai-serving:30}")
    private int aiServingIdleMinutes;

    @Value("${atlantic.chat.idle-minutes.human-serving:120}")
    private int humanServingIdleMinutes;

    @Scheduled(fixedDelay = 60_000)
    public void markExpired() {
        int ai = chatSessionMapper.markExpired("AI_SERVING",
                LocalDateTime.now().minusMinutes(aiServingIdleMinutes));
        int human = chatSessionMapper.markExpired("HUMAN_SERVING",
                LocalDateTime.now().minusMinutes(humanServingIdleMinutes));
        if (ai + human > 0) {
            log.info("会话超时置 ENDED: aiServing={} humanServing={}", ai, human);
        }
    }
}
