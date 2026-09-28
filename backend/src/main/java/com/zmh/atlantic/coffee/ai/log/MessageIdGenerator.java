package com.zmh.atlantic.coffee.ai.log;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 本轮消息"轮次相关键"生成器：intent_classify_log / tool_call_log.message_id 的取值源。
 *
 * <p>为什么不用 chat_message 自增 id（04 §3.2 注释）：决策 #52 的 saveAll 快照替换语义下，
 * chat_message 每轮先删后插、id 逐轮重建，任何跨轮引用必然悬挂——两份日志的统计口径
 * （意图准确率/工具成功率）只要求"本轮唯一、可按轮聚合"。这里用时间戳锚定的单调长整形
 * （毫秒起点 + 进程内自增），单实例部署下全局唯一且永不与 chat_message.id 冲突。</p>
 */
@Component
public class MessageIdGenerator {

    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis());

    public long next() {
        return counter.incrementAndGet();
    }
}
