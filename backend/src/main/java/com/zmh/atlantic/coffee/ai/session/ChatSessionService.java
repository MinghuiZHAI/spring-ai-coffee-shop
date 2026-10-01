package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.ai.log.ToolCallLog;
import com.zmh.atlantic.coffee.ai.log.ToolCallLogMapper;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * chat_session / chat_message 管理层（总体设计 §2.1）：会话创建、历史游标分页、
 * close 幂等；消息内容本身由 ai.memory 仓储的 saveAll 独占写入（决策 #52），
 * 本层只维护会话元数据（title/updated_at）与消息读取。
 * 工具摘要读侧（决策 #66）：按 chat_message.message_id（轮次键）关联 tool_call_log，
 * 不再依赖已废弃的 chat_message.tool_calls 冗余列。
 */
@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ToolCallLogMapper toolCallLogMapper;

    /** 每次调用都创建新会话（总体设计 §5.2：MVP 选简单方案，前端缓存 sessionId 复用）。 */
    public SessionView create(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setStatus("AI_SERVING");
        session.setTitle("");
        chatSessionMapper.insert(session);
        return toView(session);
    }

    public ChatSession getOwned(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    /**
     * 我的会话列表（updated_at 倒序）。游标 = 上一页末行会话 id：
     * 先定位游标行，再按 (updated_at, id) 复合条件向后翻页，避免 updated_at 变动导致漏读/重读。
     */
    public CursorPage<SessionView> list(Long userId, Long cursorId, int limit) {
        int capped = Math.min(limit, 50);
        ChatSession cursorRow = cursorId == null ? null : chatSessionMapper.selectById(cursorId);
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId);
        if (cursorRow != null) {
            wrapper.and(w -> w.lt(ChatSession::getUpdatedAt, cursorRow.getUpdatedAt())
                    .or(o -> o.eq(ChatSession::getUpdatedAt, cursorRow.getUpdatedAt())
                            .lt(ChatSession::getId, cursorId)));
        }
        List<ChatSession> sessions = chatSessionMapper.selectList(wrapper
                .orderByDesc(ChatSession::getUpdatedAt)
                .orderByDesc(ChatSession::getId)
                .last("LIMIT " + capped));
        List<SessionView> views = sessions.stream().map(this::toView).toList();
        Long nextCursor = sessions.size() == capped && !sessions.isEmpty()
                ? sessions.get(sessions.size() - 1).getId() : null;
        return new CursorPage<>(views, nextCursor);
    }

    /** 用户主动关闭（决策 #42 双保险之一）：对已 ENDED 会话幂等返回成功，不做状态变更。 */
    public void close(Long sessionId, Long userId) {
        getOwned(sessionId, userId);
        chatSessionMapper.close(sessionId);
    }

    /** 历史消息（游标分页，含 role 四值与工具调用摘要，§5.3 历史消息项模型）。 */
    public CursorPage<MessageView> messages(Long sessionId, Long cursor, int limit) {
        int capped = Math.min(limit, 50);
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .lt(cursor != null, ChatMessage::getId, cursor)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + capped));
        Map<Long, List<ToolCallItem>> toolsByTurn = toolsByTurnKey(rows);
        List<MessageView> views = rows.stream()
                .map(m -> toMessageView(m,
                        m.getMessageId() == null ? List.of() : toolsByTurn.getOrDefault(m.getMessageId(), List.of())))
                .toList();
        Long nextCursor = rows.size() == capped && !rows.isEmpty()
                ? rows.get(rows.size() - 1).getId() : null;
        return new CursorPage<>(views, nextCursor);
    }

    // ===== 供 AiSessionHandler 调用的元数据维护 =====

    public void touch(Long sessionId) {
        chatSessionMapper.touch(sessionId);
    }

    /** 首条消息前 20 字截断做会话标题【补充】。 */
    public void initTitleIfBlank(Long sessionId, String title, String firstMessage) {
        if ((title != null && !title.isBlank()) || firstMessage == null || firstMessage.isBlank()) {
            return;
        }
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        String stripped = firstMessage.strip();
        update.setTitle(stripped.substring(0, Math.min(stripped.length(), 20)));
        chatSessionMapper.updateById(update);
    }

    // ===== 视图结构（总体设计 §5.3 历史消息项模型） =====

    public record SessionView(Long id, String title, String status,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record ToolCallItem(String toolCallId, String tool, boolean success, Integer durationMs) {
    }

    public record MessageView(Long id, Long sessionId, String role, String content,
                              List<ToolCallItem> toolCalls, LocalDateTime createdAt) {
    }

    // ===== 内部 =====

    private SessionView toView(ChatSession session) {
        return new SessionView(session.getId(), session.getTitle(), session.getStatus(),
                session.getCreatedAt(), session.getUpdatedAt());
    }

    /** 按轮次键批量取工具日志（决策 #66）：一次 IN 查询，内存按 message_id 分组，避免逐消息 N+1。 */
    private Map<Long, List<ToolCallItem>> toolsByTurnKey(List<ChatMessage> rows) {
        List<Long> turnKeys = rows.stream()
                .map(ChatMessage::getMessageId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (turnKeys.isEmpty()) {
            return Map.of();
        }
        return toolCallLogMapper.selectList(new LambdaQueryWrapper<ToolCallLog>()
                        .in(ToolCallLog::getMessageId, turnKeys))
                .stream()
                .collect(Collectors.groupingBy(ToolCallLog::getMessageId,
                        Collectors.mapping(t -> new ToolCallItem(
                                String.valueOf(t.getId()),                 // 模型原始 toolCallId 未入日志表，以日志行 id 代号（前端仅作 key）
                                t.getToolName(),
                                t.getSuccess() != null && t.getSuccess() == 1,
                                t.getDurationMs()), Collectors.toList())));
    }

    private MessageView toMessageView(ChatMessage message, List<ToolCallItem> toolCalls) {
        return new MessageView(message.getId(), message.getSessionId(), message.getRole(),
                message.getContent(), toolCalls, message.getCreatedAt());
    }
}
