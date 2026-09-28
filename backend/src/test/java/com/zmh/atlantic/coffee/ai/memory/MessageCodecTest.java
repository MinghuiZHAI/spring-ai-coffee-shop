package com.zmh.atlantic.coffee.ai.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

/** role 四值映射（详细设计 §3.4）：USER/AI 即刻生效，AGENT/SYSTEM 值域预留零迁移。 */
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
        assertThat(MessageCodec.toSpringMessage("USER", "你好")).isInstanceOf(UserMessage.class);
        assertThat(MessageCodec.toSpringMessage("AI", "回答")).isInstanceOf(AssistantMessage.class);
        assertThat(MessageCodec.toSpringMessage("AGENT", "坐席接入（M3）")).isInstanceOf(AssistantMessage.class);
        assertThat(MessageCodec.toSpringMessage("SYSTEM", "系统提示")).isInstanceOf(SystemMessage.class);
    }

    @Test
    @DisplayName("StoredMessage 序列化形态包含 role 与 content")
    void toStored() {
        MessageCodec.StoredMessage stored = MessageCodec.toStored(new UserMessage("查订单"));
        assertThat(stored.role()).isEqualTo("USER");
        assertThat(stored.content()).isEqualTo("查订单");
    }
}
