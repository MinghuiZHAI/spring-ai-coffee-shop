package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息（详细设计 §1.3 表 15）。role 四值：USER/AI/AGENT(预留)/SYSTEM。
 * 唯一写入方是 ai.memory.MySqlRedisChatMemoryRepository.saveAll（advisor 链内快照替换，
 * 决策 #52）。message_id 为轮次相关键（决策 #66）：同轮 USER/AI 共享，跨 saveAll 重建
 * 由 USER 行 metadata 存活、AI 行重新推导；tool_call_log.message_id 同源，历史工具摘要
 * 按它关联（原 tool_calls 冗余列在快照替换下无法幸存，V6 废弃）。
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /** 轮次相关键（MessageIdGenerator；快照替换下不唯一标识行，仅标识轮次）。 */
    private Long messageId;

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
