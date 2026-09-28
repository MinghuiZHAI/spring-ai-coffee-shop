package com.zmh.atlantic.coffee.ai.tool;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具返回结构（模型可见的 JSON 载荷）：字段名即模型阅读口径，
 * 保持扁平、少嵌套；金额/时间原样字符串化交给模型组织话术。
 */
public final class ToolDtos {

    private ToolDtos() {
    }

    public record OrderBrief(String orderNo, String status, BigDecimal payAmount,
                             String itemSummary, LocalDateTime createdAt) {
    }

    public record PickupInfo(String orderNo, String status, String pickupCode,
                             LocalDateTime expectedFinishTime) {
    }

    public record RefundProgress(String orderNo, String refundNo, String refundStatus,
                                 BigDecimal amount, LocalDateTime applyAt, LocalDateTime finishedAt) {
    }

    public record PointSummary(Integer balance, List<PointItemBrief> recentRecords) {
    }

    public record PointItemBrief(LocalDateTime time, Integer change, String type) {
    }

    public record CouponBrief(String name, BigDecimal thresholdAmount, BigDecimal discountAmount,
                              LocalDateTime expireAt, String status) {
    }

    /** 推荐候选：推荐规则引擎（技术栈 §12.2）在工具内筛出，模型只组织话术。 */
    public record DrinkCandidate(String name, BigDecimal price, List<String> matchedTags,
                                 boolean activityHit) {
    }

    public record StoreBrief(String name, String address, String businessHours, String phone) {
    }
}
