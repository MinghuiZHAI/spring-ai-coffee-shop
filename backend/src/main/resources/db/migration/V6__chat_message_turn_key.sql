-- #66：chat_message 落轮次相关键。saveAll 快照替换（#52）下自增 id 每轮重建，
-- 历史消息按 message_id 关联 tool_call_log（该表 message_id 同源：AiSessionHandler.route()
-- 生成，toolContext 传入同步写）。同轮 USER/AI 共享一键；AI 行由 saveAll 组装时从
-- 紧邻 USER 行推导，自身无需持久化。
ALTER TABLE chat_message
    ADD COLUMN message_id BIGINT UNSIGNED NULL COMMENT '轮次相关键（MessageIdGenerator，同轮 USER/AI 共享；tool_call_log.message_id 同源，#66）' AFTER session_id,
    ADD KEY idx_session_message (session_id, message_id);

-- #66：工具摘要的唯一事实源收敛为 tool_call_log，chat_message 冗余列废弃
-- （实测 28 条 AI 消息仅会话最后一轮回填可幸存——下一轮 saveAll 重建行即抹除）。
ALTER TABLE chat_message DROP COLUMN tool_calls;
