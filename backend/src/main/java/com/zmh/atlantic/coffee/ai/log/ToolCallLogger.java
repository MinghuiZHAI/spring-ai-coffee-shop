package com.zmh.atlantic.coffee.ai.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @Tool 工具执行统一包装（技术栈 #35 同步落库）：计时、结果/异常序列化、写 tool_call_log。
 * tool_call_log.message_id 取本轮"轮次相关键"（AiSessionHandler 生成）——决策 #52 的
 * saveAll 快照替换语义下 chat_message.id 每轮重建，跨轮外键本就不成立，此处保证按轮统计口径。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallLogger {

    private static final int RESULT_MAX = 2000;
    private static final int ERROR_MAX = 500;

    private final ToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper;

    public <T> T record(ToolContext toolContext, String toolName, Map<String, Object> params,
                        Supplier<T> action) {
        long start = System.currentTimeMillis();
        T result = null;
        RuntimeException error = null;
        try {
            result = action.get();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            write(toolContext, toolName, params, result, error, (int) (System.currentTimeMillis() - start));
        }
    }

    /** 参数快照构造：过滤 null 值（Map.of 不接受 null），kv 依次为 key1,value1,key2,value2… */
    public static Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }

    @SneakyThrows
    private void write(ToolContext toolContext, String toolName, Map<String, Object> params,
                       Object result, RuntimeException error, int durationMs) {
        try {
            ToolCallLog row = new ToolCallLog();
            row.setMessageId(longOf(toolContext, "messageId"));
            row.setSessionId(longOf(toolContext, "sessionId"));
            row.setToolName(toolName);
            row.setParams(params == null ? null : truncate(objectMapper.writeValueAsString(params)));
            row.setResult(result == null ? null : truncate(objectMapper.writeValueAsString(result)));
            row.setSuccess(error == null ? 1 : 0);
            row.setDurationMs(durationMs);
            row.setErrorMsg(error == null ? null : truncate(String.valueOf(error.getMessage())));
            toolCallLogMapper.insert(row);
        } catch (Exception logError) {
            // 日志失败不干扰工具结果返回
            log.warn("tool_call_log 写入失败: tool={}", toolName, logError);
        }
    }

    private Long longOf(ToolContext toolContext, String key) {
        Object v = toolContext.getContext().get(key);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= RESULT_MAX ? s : s.substring(0, RESULT_MAX);
    }
}
