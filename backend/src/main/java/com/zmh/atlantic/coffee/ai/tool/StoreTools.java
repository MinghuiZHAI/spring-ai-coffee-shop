package com.zmh.atlantic.coffee.ai.tool;

import com.zmh.atlantic.coffee.ai.log.ToolCallLogger;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.StoreBrief;
import com.zmh.atlantic.coffee.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 门店域只读工具 ×1（详细设计 §3.3）：FaqAgent 核实营业信息用。
 */
@Component
@RequiredArgsConstructor
public class StoreTools {

    private final StoreService storeService;
    private final ToolCallLogger toolCallLogger;

    @Tool(description = "按关键词查询门店（名称/地址模糊匹配），返回地址、营业时间、电话")
    public List<StoreBrief> queryStores(
            ToolContext toolContext,
            @ToolParam(required = false, description = "门店名或地址关键词，不传查全部门店") String keyword) {
        return toolCallLogger.record(toolContext, "queryStores", ToolCallLogger.params("keyword", keyword), () -> {
            currentUserId(toolContext);
            return storeService.list(keyword).stores().stream()
                    .map(s -> new StoreBrief(s.name(), s.address(), s.businessHours(), s.phone()))
                    .toList();
        });
    }

    private void currentUserId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("userId");
        if (v == null) {
            throw new com.zmh.atlantic.coffee.common.exception.BizException(40301, "缺少用户上下文");
        }
    }
}
