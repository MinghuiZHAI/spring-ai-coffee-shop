package com.zmh.atlantic.coffee.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmh.atlantic.coffee.ai.session.ChatMessage;
import com.zmh.atlantic.coffee.ai.session.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChatMemory 自定义仓储（技术栈 #15 / 详细设计 §3.4）：MySQL 权威 + Redis 热缓存。
 * conversationId = chat_session.id 的字符串形式。
 *
 * <p>saveAll 契约为【替换】（Spring AI 官方语义）：MessageWindowChatMemory 每轮传入
 * "历史 + 新消息"截断后的完整窗口，绝非追加——实现为"先删后插"快照替换（决策 #52），
 * chat_message 由此收敛为每会话最近 20 条的窗口快照。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MySqlRedisChatMemoryRepository implements ChatMemoryRepository {

    static final String KEY_PREFIX = "chat:memory:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final int WINDOW = 20;

    private final ChatMessageMapper chatMessageMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> findConversationIds() {
        return chatMessageMapper.selectDistinctSessionIds().stream()
                .map(String::valueOf).toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        List<String> cached = redis.opsForList().range(key, 0, WINDOW - 1);
        if (cached != null && !cached.isEmpty()) {
            return deserialize(cached);
        }
        // 缓存未命中：游标倒查最近 20 条后反转为时间正序，并回填缓存
        List<ChatMessage> rows = chatMessageMapper.findLatest(Long.valueOf(conversationId), WINDOW);
        Collections.reverse(rows);
        List<Message> messages = rows.stream()
                .map(r -> MessageCodec.toSpringMessage(r.getRole(), r.getContent()))
                .toList();
        if (!messages.isEmpty()) {
            cacheAll(key, MessageCodec.toStored(messages));
        }
        return messages;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String conversationId, List<Message> messages) {
        // 1) 物理删除该会话全部消息（chat_message 无逻辑删除字段，§1.1）
        chatMessageMapper.deleteBySession(Long.valueOf(conversationId));
        // 2) 批量插入本次窗口全量消息（含历史，最多 20 条）
        if (!messages.isEmpty()) {
            List<ChatMessage> rows = MessageCodec.toStored(messages).stream()
                    .map(s -> row(Long.valueOf(conversationId), s.role(), s.content()))
                    .toList();
            chatMessageMapper.insertBatch(rows);
        }
        // 3) 重建 Redis 缓存（先删旧列表，再写入并截断到窗口）
        cacheAll(KEY_PREFIX + conversationId, MessageCodec.toStored(messages));
        if (log.isDebugEnabled()) {
            log.debug("chat memory snapshot saved: conversationId={} messages={}", conversationId, messages.size());
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        chatMessageMapper.deleteBySession(Long.valueOf(conversationId));
        redis.delete(KEY_PREFIX + conversationId);
    }

    // ===== 内部 =====

    private ChatMessage row(Long sessionId, String role, String content) {
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        return m;
    }

    private void cacheAll(String key, List<MessageCodec.StoredMessage> stored) {
        if (stored.isEmpty()) {
            redis.delete(key);
            return;
        }
        redis.delete(key);
        redis.opsForList().rightPushAll(key, stored.stream().map(this::serialize).toList());
        redis.opsForList().trim(key, 0, WINDOW - 1);
        redis.expire(key, CACHE_TTL);
    }

    private List<Message> deserialize(List<String> cached) {
        List<Message> messages = new ArrayList<>(cached.size());
        for (String json : cached) {
            MessageCodec.StoredMessage s = read(json);
            messages.add(MessageCodec.toSpringMessage(s.role(), s.content()));
        }
        return messages;
    }

    @SneakyThrows
    private String serialize(MessageCodec.StoredMessage stored) {
        return objectMapper.writeValueAsString(stored);
    }

    @SneakyThrows
    private MessageCodec.StoredMessage read(String json) {
        return objectMapper.readValue(json, MessageCodec.StoredMessage.class);
    }
}
