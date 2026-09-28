package com.zmh.atlantic.coffee.order;

import com.zmh.atlantic.coffee.member.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单状态模拟推进器（详细设计 §2.4，决策 #46）：MVP 无门店端，
 * 支付 30s 后开始制作、制作 3 分钟出餐；READY→COMPLETED 由用户端"确认取餐"核销。
 * 批量条件 UPDATE 不走 transit() 乐观锁路径的理由与边界见 §2.4；
 * 顺带处理：超时未支付（30 分钟）自动取消并归还优惠券。
 */
@Component
@RequiredArgsConstructor
public class OrderSimulator {

    private final OrderMapper orderMapper;
    private final UserCouponMapper userCouponMapper;

    @Scheduled(fixedDelay = 10000)
    public void simulateStoreProgress() {
        LocalDateTime now = LocalDateTime.now();
        orderMapper.transitByStatus("PAID_TODO", "MAKING", now.minusSeconds(30));    // 支付 30s 后开始制作
        orderMapper.transitByStatus("MAKING", "READY", now.minusSeconds(180));       // 制作 3 分钟出餐
        int cancelled = orderMapper.cancelExpired(now.minusMinutes(30));             // 超时未支付自动取消
        if (cancelled > 0) {
            userCouponMapper.restoreForTimeoutCancelled();                           // 归还所用优惠券
        }
    }
}
