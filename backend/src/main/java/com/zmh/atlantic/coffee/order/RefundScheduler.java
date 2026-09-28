package com.zmh.atlantic.coffee.order;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 退款延迟到账调度（详细设计 §2.3，技术栈 #31，决策 #47）：
 * 两跳节奏 T+30s 申请中→已通过、T+60s→已到账；表即队列，条件更新天然幂等，重启自动续跑。
 * AI 客服的"退款进度查询"工具可观察到中间态（演示价值）。
 */
@Component
@RequiredArgsConstructor
public class RefundScheduler {

    private final RefundMapper refundMapper;

    @Scheduled(fixedDelay = 5000)
    public void advanceRefunds() {
        LocalDateTime now = LocalDateTime.now();
        refundMapper.approveDue(now.minusSeconds(30));   // 申请中 → 已通过（退款中）
        refundMapper.successDue(now.minusSeconds(60));   // 已通过 → 已到账
        refundMapper.syncRefundedOrders();               // 同步订单终态：退款中 → 已退款
    }
}
