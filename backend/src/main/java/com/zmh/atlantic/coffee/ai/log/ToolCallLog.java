package com.zmh.atlantic.coffee.ai.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工具调用日志（详细设计 §1.3 表 16）：只读 @Tool 工具执行记录，
 * "工具调用成功率 ≥95%"指标数据源；同步落库（技术栈 #35）。
 */
@Data
@TableName("tool_call_log")
public class ToolCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    /** 冗余 session_id，便于按会话统计。 */
    private Long sessionId;

    private String toolName;

    private String params;

    /** 超长结果截断存储。 */
    private String result;

    private Integer success;

    private Integer durationMs;

    private String errorMsg;

    private LocalDateTime createdAt;
}
