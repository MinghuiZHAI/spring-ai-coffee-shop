package com.zmh.atlantic.coffee.ai.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * role 四值映射（详细设计 §3.4）：USER/AI 即刻生效，AGENT/SYSTEM 值域预留零迁移。
 * 轮次键（决策 #66）：USER 行 metadata 携带、跨 saveAll 重建存活，AI 行由仓储推导。
 */
class MessageCodecTest {

    @Test
    @DisplayName("Spring AI Message → DB role：USER→USER、ASSISTANT→AI、SYSTEM→SYSTEM")
    void toDbRole() {
        assertThat(MessageCodec.toDbRole(new UserMessage("你好"))).isEqualTo("USER");
        assertThat(MessageCodec.toDbRole(new AssistantMessage("你好，请问点什么？"))).isEqualTo("AI");
        assertThat(MessageCodec.toDbRole(new SystemMessage("你是客服"))).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("DB role → Spring AI Message：AI 与预留 AGENT 均映射为 AssistantMessage")
    void toSpringMessage() {
        assertThat(MessageCodec.toSpringMessage("USER", "你好", null)).isInstanceOf(UserMessage.class);
        assertThat(MessageCodec.toSpringMessage("AI", "回答", null)).isInstanceOf(AssistantMessage.class);
        assertThat(MessageCodec.toSpringMessage("AGENT", "坐席接入（M3）", null)).isInstanceOf(AssistantMessage.class);
        assertThat(MessageCodec.toSpringMessage("SYSTEM", "系统提示", null)).isInstanceOf(SystemMessage.class);
    }

    @Test
    @DisplayName("StoredMessage 序列化形态包含 role 与 content")
    void toStored() {
        MessageCodec.StoredMessage stored = MessageCodec.toStored(new UserMessage("查订单"));
        assertThat(stored.role()).isEqualTo("USER");
        assertThat(stored.content()).isEqualTo("查订单");
        assertThat(stored.messageId()).isNull();
    }

    @Test
    @DisplayName("#66：USER 行轮次键经 toStored → toSpring 往返不丢（跨 saveAll 重建的存活载体）")
    void turnKeyRoundtripOnUserMessage() {
        UserMessage keyed = UserMessage.builder()
                .text("我的订单做好了吗")
                .metadata(Map.of(MessageCodec.MESSAGE_ID_KEY, 1790825366574L))
                .build();

        MessageCodec.StoredMessage stored = MessageCodec.toStored(keyed);
        assertThat(stored.messageId()).isEqualTo(1790825366574L);

        Message back = MessageCodec.toSpringMessage(stored.role(), stored.content(), stored.messageId());
        assertThat(back.getMetadata().get(MessageCodec.MESSAGE_ID_KEY)).isEqualTo(1790825366574L);
        // 再一轮 toStored：键仍存活（saveAll 每轮重建窗口时历史 USER 行不丢键）
        assertThat(MessageCodec.toStored(back).messageId()).isEqualTo(1790825366574L);
    }

    @Test
    @DisplayName("#66：无键历史消息（V6 前存量）往返后保持无键，不产生误关联")
    void turnKeyAbsentStaysAbsent() {
        MessageCodec.StoredMessage stored = MessageCodec.toStored(new UserMessage("存量消息"));
        Message back = MessageCodec.toSpringMessage(stored.role(), stored.content(), stored.messageId());
        assertThat(MessageCodec.toStored(back).messageId()).isNull();
    }

    @Test
    @DisplayName("#66 跨轮回归：连续两轮 saveAll 重建窗口，轮次键不丢、不串轮")
    void turnKeysSurviveCrossTurnRebuild() {
        // 轮 1：saveAll 组装 [USER(K1), AI] —— AI 行由紧邻 USER 行推导
        List<MessageCodec.StoredMessage> turn1 = List.of(
                stored("USER", "我的订单做好了吗", 1790825366574L),
                stored("AI", "我来帮您查一下订单进度", null));
        assertThat(MySqlRedisChatMemoryRepository.assignTurnKeys(turn1))
                .containsExactly(1790825366574L, 1790825366574L);

        // 轮 2 前的窗口回读：AI 行键未持久化（推导所得），USER 行键由 metadata 存活
        List<Message> window = turn1.stream()
                .map(s -> MessageCodec.toSpringMessage(s.role(), s.content(), s.messageId()))
                .toList();
        // 轮 2 before()：advisor 追加新 USER 后 saveAll —— 窗口重建，键分配为 [K1, K1, K2]
        List<MessageCodec.StoredMessage> turn2Window = MessageCodec.toStored(
                List.of(window.get(0), window.get(1),
                        UserMessage.builder()
                                .text("会员等级怎么升级")
                                .metadata(Map.of(MessageCodec.MESSAGE_ID_KEY, 1790825366599L))
                                .build()));
        assertThat(MySqlRedisChatMemoryRepository.assignTurnKeys(turn2Window))
                .containsExactly(1790825366574L, 1790825366574L, 1790825366599L);

        // 轮 2 after()：窗口再追加 AI 行再重建 —— [K1, K1, K2, K2]
        List<MessageCodec.StoredMessage> turn2After = new java.util.ArrayList<>(turn2Window);
        turn2After.add(stored("AI", "根据会员规则……", null));
        assertThat(MySqlRedisChatMemoryRepository.assignTurnKeys(turn2After))
                .containsExactly(1790825366574L, 1790825366574L, 1790825366599L, 1790825366599L);
    }

    @Test
    @DisplayName("#66 窗口切断防串轮：USER/AI 对被截断时 AI 行键为 NULL，宁缺勿串")
    void turnKeyNullWhenPairSplitByWindowEviction() {
        // 边界裁切掉 USER(K2)，窗口以 AI(K2 轮) 开头：前一行非 USER，键不得继承上一轮
        List<MessageCodec.StoredMessage> split = List.of(
                stored("USER", "第一轮", 1L),
                stored("AI", "回复一", null),
                stored("AI", "第二轮回复（其 USER 已被裁切）", null));
        assertThat(MySqlRedisChatMemoryRepository.assignTurnKeys(split))
                .containsExactly(1L, 1L, null);
    }

    private static MessageCodec.StoredMessage stored(String role, String content, Long messageId) {
        return new MessageCodec.StoredMessage(role, content, messageId);
    }
}
