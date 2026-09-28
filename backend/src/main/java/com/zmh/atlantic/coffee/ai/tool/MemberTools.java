package com.zmh.atlantic.coffee.ai.tool;

import com.zmh.atlantic.coffee.ai.log.ToolCallLogger;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.CouponBrief;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.PointItemBrief;
import com.zmh.atlantic.coffee.ai.tool.ToolDtos.PointSummary;
import com.zmh.atlantic.coffee.member.CouponService;
import com.zmh.atlantic.coffee.member.PointService;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.CouponView;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointItem;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会员域只读工具 ×2（详细设计 §3.3）：积分/券查询，包装 PointService/CouponService。
 */
@Component
@RequiredArgsConstructor
public class MemberTools {

    private final PointService pointService;
    private final CouponService couponService;
    private final ToolCallLogger toolCallLogger;

    @Tool(description = "查询当前登录用户的积分余额与近期流水")
    public PointSummary queryPoints(ToolContext toolContext) {
        return toolCallLogger.record(toolContext, "queryPoints", ToolCallLogger.params(), () -> {
            long userId = currentUserId(toolContext);
            var view = pointService.summary(userId);
            List<PointItemBrief> records = view.records().stream()
                    .map(r -> new PointItemBrief(r.createdAt(), r.changeValue(), r.type()))
                    .toList();
            return new PointSummary(view.balance(), records);
        });
    }

    @Tool(description = "查询当前登录用户的优惠券列表，返回券名、门槛、面额、有效期、状态")
    public List<CouponBrief> queryCoupons(
            ToolContext toolContext,
            @ToolParam(required = false, description = "可选：UNUSED 未使用 / USED 已使用 / EXPIRED 已过期，默认查未使用") String status) {
        return toolCallLogger.record(toolContext, "queryCoupons", ToolCallLogger.params("status", status), () -> {
            long userId = currentUserId(toolContext);
            String normalized = status == null || status.isBlank() ? "UNUSED" : status.trim().toUpperCase();
            List<CouponView> views = couponService.list(userId, normalized);
            return views.stream()
                    .map(c -> new CouponBrief(c.name(), c.thresholdAmount(), c.discountAmount(),
                            c.expireAt(), c.status()))
                    .toList();
        });
    }

    private long currentUserId(ToolContext toolContext) {
        Object v = toolContext.getContext().get("userId");
        if (v == null) {
            throw new com.zmh.atlantic.coffee.common.exception.BizException(40301, "缺少用户上下文");
        }
        return ((Number) v).longValue();
    }
}
