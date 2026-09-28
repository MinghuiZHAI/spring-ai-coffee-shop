package com.zmh.atlantic.coffee.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmh.atlantic.coffee.ai.log.IntentClassifyLogService;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * IntentAgent（详细设计 §3.2）：qwen-turbo 结构化输出意图分类 + 三级降级。
 *
 * <p>降级矩阵（总体设计 §3.2）：① JSON 解析失败 → 重试 1 次 → 仍失败降 KNOWLEDGE
 * （RETRY_EXHAUSTED）；② LLM 异常/超时 → 直走 FaqAgent（TIMEOUT）；③ intent 非法枚举 →
 * 降 KNOWLEDGE（INVALID_ENUM）。每次分类（成功或降级）都落 intent_classify_log。</p>
 *
 * <p>无记忆 Advisor（不需要 conversationId）、无工具（不需要 toolContext）——分类是最小调用。</p>
 */
@Slf4j
@Component
public class IntentAgent {

    private final ChatClient intentAgentClient;
    private final IntentClassifyLogService logService;
    private final ObjectMapper objectMapper;

    public IntentAgent(ChatClient intentAgentClient, IntentClassifyLogService logService,
                       ObjectMapper objectMapper) {
        this.intentAgentClient = intentAgentClient;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    public ClassifyOutcome classify(Long messageId, String content) {
        long start = System.currentTimeMillis();
        String usedIntent = Intent.KNOWLEDGE.name();
        double usedConfidence = 0d;
        boolean degraded = true;
        String reason = "TIMEOUT";                                    // 默认降级口径，成功/其他分支内覆写
        try {
            String json = invokeModel(content);                       // ① 解析失败 → 重试 1 次
            IntentResult r;
            try {
                r = parse(json);
            } catch (Exception firstParseError) {
                String retryJson = invokeModel(content);              // 重试时模型异常直接外抛 → TIMEOUT
                try {
                    r = parse(retryJson);
                } catch (Exception secondParseError) {
                    throw new ParseExhausted();                       // 重试后仍解析失败 → RETRY_EXHAUSTED
                }
            }
            try {
                Intent intent = Intent.valueOf(normalize(r.intent()));
                usedIntent = intent.name();
                usedConfidence = clamp(r.confidence());
                degraded = false;
                reason = null;
                return ClassifyOutcome.of(intent, usedConfidence);
            } catch (IllegalArgumentException badEnum) {              // ③ 非法枚举 → KNOWLEDGE
                reason = "INVALID_ENUM";
                return ClassifyOutcome.degraded(reason);
            }
        } catch (ParseExhausted exhausted) {                          // ① 重试后仍解析失败
            reason = "RETRY_EXHAUSTED";
            log.warn("意图分类降级: messageId={} reason={}", messageId, reason);
            return ClassifyOutcome.degraded(reason);
        } catch (Exception e) {                                       // ② LLM 异常/超时 → 直走 FaqAgent
            reason = "TIMEOUT";
            log.warn("意图分类降级: messageId={} reason={}", messageId, reason, e);
            return ClassifyOutcome.degraded(reason);
        } finally {
            logService.write(messageId, usedIntent, usedConfidence, degraded, reason, start);
        }
    }

    // ===== 内部 =====

    /** 模型调用单独成方法（qwen-turbo 调用级覆盖，决策 #48），测试可覆写模拟各失败分支。 */
    String invokeModel(String content) {
        return intentAgentClient.prompt()
                .user(content)
                .options(DashScopeChatOptions.builder().withModel("qwen-turbo").build())
                .call()
                .content();
    }

    private IntentResult parse(String raw) throws Exception {
        return objectMapper.readValue(stripCodeFence(raw), IntentResult.class);
    }

    /** JSON 解析重试耗尽（对应 04 §3.2 的 ParseRetryExhausted 语义分支）。 */
    private static final class ParseExhausted extends RuntimeException {
    }

    /** 模型可能用 ```json 围栏包裹，剥除后交给 Jackson。 */
    private String stripCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "");
        }
        return text.strip();
    }

    private String normalize(String intent) {
        return intent == null ? "" : intent.strip().toUpperCase();
    }

    private double clamp(Double confidence) {
        if (confidence == null || confidence.isNaN() || confidence < 0) {
            return 0d;
        }
        return Math.min(confidence, 1d);
    }

    /** 结构化输出载体：intent 用 String 承接，非法值走 INVALID_ENUM 降级而非解析失败。 */
    record IntentResult(String intent, Double confidence) {
    }
}
