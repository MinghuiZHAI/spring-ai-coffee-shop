package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /** 会话活跃心跳（总体设计 §3.2 第 10 步：会话层维护会话元数据）。 */
    @Update("UPDATE chat_session SET updated_at = NOW() WHERE id = #{id}")
    int touch(@Param("id") Long id);

    /** 超时扫描置 ENDED（总体设计 v1.3 修正 4，决策 #42 双保险之一）：按状态区分时长。 */
    @Update("UPDATE chat_session SET status = 'ENDED', updated_at = NOW() " +
            "WHERE status = #{status} AND updated_at <= #{deadline}")
    int markExpired(@Param("status") String status, @Param("deadline") LocalDateTime deadline);

    /** 用户主动关闭：条件更新天然幂等，ENDED 会话再次调用返回 0（不重复变更）。 */
    @Update("UPDATE chat_session SET status = 'ENDED', updated_at = NOW() " +
            "WHERE id = #{id} AND status <> 'ENDED'")
    int close(@Param("id") Long id);
}
