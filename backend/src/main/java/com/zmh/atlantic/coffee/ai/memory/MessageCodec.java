package com.zmh.atlantic.coffee.ai.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

/**
 * Spring AI Message 与持久化形态（JSON 字符串 / chat_message 行）的双向映射。
 *
 * <p>role 映射（详细设计 §3.4）：Spring AI UserMessage→USER、AssistantMessage→AI；
 * AGENT（M3 人工坐席）/SYSTEM 值域在 codec 中完整支持，M3 启用零迁移。</p>
 */
public final class MessageCodec {

    /** JSON 序列化形态（Redis List 元素与跨层传递共用）。 */
    public record StoredMessage(@JsonProperty("role") String role,
                                @JsonProperty("content") String content) {
    }

    private MessageCodec() {
    }

    // ===== Spring AI Message → 持久化形态 =====

    public static StoredMessage toStored(Message message) {
        return new StoredMessage(toDbRole(message), message.getText());
    }

    public static List<StoredMessage> toStored(List<Message> messages) {
        return messages.stream().map(MessageCodec::toStored).toList();
    }

    /** chat_message.role 值域：USER/AI/AGENT(预留)/SYSTEM。 */
    public static String toDbRole(Message message) {
        return switch (message.getMessageType()) {
            case USER -> "USER";
            case ASSISTANT -> "AI";
            case SYSTEM -> "SYSTEM";
            case TOOL -> throw new IllegalArgumentException("TOOL 消息不入会话记忆窗口");
        };
    }

    // ===== 持久化形态 → Spring AI Message（读出） =====

    public static Message toSpringMessage(String role, String content) {
        return switch (role) {
            case "USER" -> new UserMessage(content);
            case "AI", "AGENT" -> new AssistantMessage(content);
            case "SYSTEM" -> new SystemMessage(content);
            default -> throw new IllegalArgumentException("未知会话消息 role: " + role);
        };
    }
}
