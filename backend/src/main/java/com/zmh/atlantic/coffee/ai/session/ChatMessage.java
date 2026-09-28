package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息（详细设计 §1.3 表 15）。role 四值：USER/AI/AGENT(预留)/SYSTEM。
 * 唯一写入方是 ai.memory.MySqlRedisChatMemoryRepository.saveAll（advisor 链内快照替换，
 * 决策 #52）——会话层不直接 insert 消息，仅对本列 tool_calls 做收尾 UPDATE。
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private String role;

    private String content;

    /** 工具调用摘要 JSON：[{"toolCallId":"...","tool":"...","success":true,"durationMs":35}]。 */
    private String toolCalls;

    private LocalDateTime createdAt;
}
