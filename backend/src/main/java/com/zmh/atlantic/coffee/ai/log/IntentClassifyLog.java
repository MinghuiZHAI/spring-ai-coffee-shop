package com.zmh.atlantic.coffee.ai.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 意图分类日志（详细设计 §1.3 表 18）：与 tool_call_log 分表、独立口径（决策 #43）。
 * 每次分类（无论成功或降级）都落库，统计 PRD"意图识别准确率 ≥90%"覆盖真实失败路径。
 */
@Data
@TableName("intent_classify_log")
public class IntentClassifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    /** 最终采用意图（含降级后）。 */
    private String intent;

    private BigDecimal confidence;

    private Integer isDegraded;

    /** PARSE_FAILED/TIMEOUT/INVALID_ENUM/RETRY_EXHAUSTED。 */
    private String degradeReason;

    private Integer durationMs;

    private LocalDateTime createdAt;
}
