package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话（详细设计 §1.3 表 14）。status 三值一次定全：
 * AI_SERVING / HUMAN_SERVING（M3 预留）/ ENDED；HUMAN_SERVING 承接坐席 agent_id MVP 恒 NULL。
 */
@Data
@TableName("chat_session")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String status;

    private Long agentId;

    /** 首条消息前 20 字截断【补充】。 */
    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
