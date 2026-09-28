package com.zmh.atlantic.coffee.ai.log;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 意图分类日志落库（总体设计 §3.2 / 决策 #43）：成功与降级都写，
 * intent_classify_log 与 tool_call_log 分表、独立统计口径。
 */
@Service
@RequiredArgsConstructor
public class IntentClassifyLogService {

    private final IntentClassifyLogMapper mapper;

    public void write(Long messageId, String intent, double confidence,
                      boolean degraded, String degradeReason, long startMillis) {
        IntentClassifyLog row = new IntentClassifyLog();
        row.setMessageId(messageId);
        row.setIntent(intent);
        row.setConfidence(BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
        row.setIsDegraded(degraded ? 1 : 0);
        row.setDegradeReason(degradeReason);
        row.setDurationMs((int) (System.currentTimeMillis() - startMillis));
        mapper.insert(row);
    }
}
