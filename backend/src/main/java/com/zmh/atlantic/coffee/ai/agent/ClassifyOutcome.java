package com.zmh.atlantic.coffee.ai.agent;

/**
 * 意图分类结果（IntentAgent 输出 + AiSessionHandler 消费）。
 * degradeReason：PARSE_FAILED/TIMEOUT/INVALID_ENUM/RETRY_EXHAUSTED。
 */
public record ClassifyOutcome(Intent intent, double confidence, boolean degraded, String degradeReason) {

    public static ClassifyOutcome of(Intent intent, double confidence) {
        return new ClassifyOutcome(intent, confidence, false, null);
    }

    public static ClassifyOutcome degraded(String reason) {
        return new ClassifyOutcome(Intent.KNOWLEDGE, 0d, true, reason);
    }
}
