package com.zmh.atlantic.coffee.ai.memory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Message 与持久化形态（JSON 字符串 / chat_message 行）的双向映射。
 *
 * <p>role 映射（详细设计 §3.4）：Spring AI UserMessage→USER、AssistantMessage→AI；
 * AGENT（M3 人工坐席）/SYSTEM 值域在 codec 中完整支持，M3 启用零迁移。</p>
 *
 * <p>轮次相关键（决策 #66）：MessageIdGenerator 每轮生成一次，经 handler 注入本轮
 * UserMessage 的 metadata（1.1.2 的 MessageChatMemoryAdvisor.before() 原样复用请求中的
 * UserMessage 实例，metadata 随 saveAll 落库）。AI 行的键不落 metadata（1.1.2
 * AssistantMessage 无 metadata 构造通道），由仓储组装行时从紧邻 USER 行推导——键随
 * USER 行跨 saveAll 重建存活，AI 行每次重建重新推导。tool_call_log.message_id 与此同源。</p>
 */
public final class MessageCodec {

    /** Message.metadata 中轮次键的键名（chat_message.message_id / tool_call_log.message_id 同名同源）。 */
    public static final String MESSAGE_ID_KEY = "message_id";

    /** JSON 序列化形态（Redis List 元素与跨层传递共用）。 */
    public record StoredMessage(@JsonProperty("role") String role,
                                @JsonProperty("content") String content,
                                @JsonProperty("messageId") Long messageId) {
    }

    private MessageCodec() {
    }

    // ===== Spring AI Message → 持久化形态 =====

    public static StoredMessage toStored(Message message) {
        Object v = message.getMetadata().get(MESSAGE_ID_KEY);
        Long messageId = v instanceof Number n ? n.longValue() : null;
        return new StoredMessage(toDbRole(message), message.getText(), messageId);
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

    /** USER 行回填轮次键 metadata（跨 saveAll 重建的存活载体）；AI 行键由仓储组装时推导。 */
    public static Message toSpringMessage(String role, String content, Long messageId) {
        return switch (role) {
            case "USER" -> messageId == null ? new UserMessage(content)
                    : UserMessage.builder().text(content).metadata(Map.of(MESSAGE_ID_KEY, messageId)).build();
            case "AI", "AGENT" -> new AssistantMessage(content);
            case "SYSTEM" -> new SystemMessage(content);
            default -> throw new IllegalArgumentException("未知会话消息 role: " + role);
        };
    }
}
