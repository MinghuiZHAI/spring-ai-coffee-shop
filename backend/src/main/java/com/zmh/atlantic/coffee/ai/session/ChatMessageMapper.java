package com.zmh.atlantic.coffee.ai.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 游标倒查最近窗口（idx_session_id_id 专用索引），调用方反转为时间正序。 */
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<ChatMessage> findLatest(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    @Select("SELECT id FROM chat_message WHERE session_id = #{sessionId} AND role = 'AI' " +
            "ORDER BY id DESC LIMIT 1")
    Long findLatestAiId(@Param("sessionId") Long sessionId);

    @Select("SELECT DISTINCT session_id FROM chat_message")
    List<Long> selectDistinctSessionIds();

    /** saveAll 快照替换的第 1 步：物理 DELETE（表无逻辑删除字段，§1.1）。 */
    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySession(@Param("sessionId") Long sessionId);

    /** saveAll 快照替换的第 2 步：批量插入本次窗口全量消息（最多 20 条）。 */
    @Insert("<script>INSERT INTO chat_message (session_id, message_id, role, content) VALUES " +
            "<foreach collection='messages' item='m' separator=','>" +
            "(#{m.sessionId}, #{m.messageId}, #{m.role}, #{m.content})" +
            "</foreach></script>")
    int insertBatch(@Param("messages") List<ChatMessage> messages);
}
