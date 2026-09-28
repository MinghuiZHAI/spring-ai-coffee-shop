package com.zmh.atlantic.coffee.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmh.atlantic.coffee.ai.log.IntentClassifyLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * IntentAgent 三级降级单测（04 文档必测项）：成功 / 解析重试 / 重试耗尽 / 超时 / 非法枚举。
 * 通过覆写 invokeModel 模拟模型侧各失败形态，不依赖真实 DashScope。
 */
class IntentAgentDegradeTest {

    private final IntentClassifyLogService logService = mock(IntentClassifyLogService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private IntentAgent agent;

    /** invokeModel 的返回队列/异常队列（按调用次序消耗）。 */
    private final List<Object> scripted = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // ChatClient 传 null：被测路径全部走覆写后的 invokeModel
        agent = new IntentAgent(null, logService, objectMapper) {
            @Override
            String invokeModel(String content) {
                Object next = scripted.isEmpty() ? null : scripted.remove(0);
                if (next instanceof RuntimeException e) {
                    throw e;
                }
                return (String) next;
            }
        };
    }

    private void given(Object... outputs) {
        scripted.addAll(List.of(outputs));
    }

    @Test
    @DisplayName("成功解析：返回意图与置信度，日志标记未降级")
    void classifySuccess() {
        given("{\"intent\":\"ORDER\",\"confidence\":0.93}");
        ClassifyOutcome outcome = agent.classify(1L, "我的订单做好了吗");
        assertThat(outcome.intent()).isEqualTo(Intent.ORDER);
        assertThat(outcome.confidence()).isEqualTo(0.93);
        assertThat(outcome.degraded()).isFalse();
        verifyLoggedOnce(1L, "ORDER", false, null);
    }

    @Test
    @DisplayName("带 markdown 围栏的输出也能解析")
    void classifyWithCodeFence() {
        given("```json\n{\"intent\":\"MEMBER\",\"confidence\":0.8}\n```");
        ClassifyOutcome outcome = agent.classify(2L, "我的积分还有多少");
        assertThat(outcome.intent()).isEqualTo(Intent.MEMBER);
        assertThat(outcome.degraded()).isFalse();
    }

    @Test
    @DisplayName("降级①：首次解析失败重试 1 次后成功")
    void parseRetryRecovers() {
        given("这不是 JSON", "{\"intent\":\"RECOMMEND\",\"confidence\":0.7}");
        ClassifyOutcome outcome = agent.classify(3L, "推荐一杯咖啡");
        assertThat(outcome.intent()).isEqualTo(Intent.RECOMMEND);
        assertThat(outcome.degraded()).isFalse();
    }

    @Test
    @DisplayName("降级①：重试后仍解析失败 → KNOWLEDGE + RETRY_EXHAUSTED")
    void parseRetryExhausted() {
        given("坏输出", "还是坏输出");
        ClassifyOutcome outcome = agent.classify(4L, "随便聊聊");
        assertThat(outcome.intent()).isEqualTo(Intent.KNOWLEDGE);
        assertThat(outcome.degraded()).isTrue();
        assertThat(outcome.degradeReason()).isEqualTo("RETRY_EXHAUSTED");
        verifyLoggedOnce(4L, "KNOWLEDGE", true, "RETRY_EXHAUSTED");
    }

    @Test
    @DisplayName("降级②：LLM 调用异常/超时 → KNOWLEDGE + TIMEOUT")
    void timeoutDegrades() {
        given(new RuntimeException("connection timed out"));
        ClassifyOutcome outcome = agent.classify(5L, "门店几点开门");
        assertThat(outcome.intent()).isEqualTo(Intent.KNOWLEDGE);
        assertThat(outcome.degradeReason()).isEqualTo("TIMEOUT");
        verifyLoggedOnce(5L, "KNOWLEDGE", true, "TIMEOUT");
    }

    @Test
    @DisplayName("降级③：intent 非法枚举 → KNOWLEDGE + INVALID_ENUM")
    void invalidEnumDegrades() {
        given("{\"intent\":\"WEATHER\",\"confidence\":0.6}");
        ClassifyOutcome outcome = agent.classify(6L, "今天天气如何");
        assertThat(outcome.intent()).isEqualTo(Intent.KNOWLEDGE);
        assertThat(outcome.degradeReason()).isEqualTo("INVALID_ENUM");
        verifyLoggedOnce(6L, "KNOWLEDGE", true, "INVALID_ENUM");
    }

    @Test
    @DisplayName("confidence 越界收敛到 [0,1]")
    void confidenceClamped() {
        given("{\"intent\":\"FALLBACK\",\"confidence\":2.5}");
        ClassifyOutcome outcome = agent.classify(7L, "转人工");
        assertThat(outcome.intent()).isEqualTo(Intent.FALLBACK);
        assertThat(outcome.confidence()).isEqualTo(1.0);
    }

    private void verifyLoggedOnce(long messageId, String intent, boolean degraded, String reason) {
        verify(logService, times(1)).write(eq(messageId), eq(intent),
                anyDouble(), eq(degraded), eq(reason), anyLong());
    }
}
