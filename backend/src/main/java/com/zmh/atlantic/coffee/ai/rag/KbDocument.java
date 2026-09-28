package com.zmh.atlantic.coffee.ai.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档（详细设计 §1.3 表 17）。状态门：DRAFT→PUBLISHED（重建成功才置发布，决策 #44）。
 * 实体放在 ai.rag：M1 由 KbIndexService/KbBootstrapRunner 读取，M2 知识库管理后台（kb 包）
 * 依赖 ai.rag 复用，保持"kb → ai.rag 单向依赖"（总体设计 §2.2）。
 */
@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** MENU/ACTIVITY/MEMBER_RULE/FAQ/BRAND。 */
    private String kbType;

    /** Markdown 源文。 */
    private String content;

    private String status;

    private Integer version;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
