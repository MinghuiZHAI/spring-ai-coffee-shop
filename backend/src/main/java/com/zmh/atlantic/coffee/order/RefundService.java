package com.zmh.atlantic.coffee.order;

import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 退款单创建：仅生成 APPLYING 退款单，到账节奏由 RefundScheduler 两跳驱动（决策 #47）。
 * AI 不发起退款——发起渠道恒为 USER_PAGE（PRD 决策 #12）。
 */
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RefundMapper refundMapper;
    private final StringRedisTemplate redis;

    @Transactional
    public void createFullRefund(Order order) {
        Refund refund = new Refund();
        refund.setRefundNo("RF" + LocalDate.now().format(DATE) + String.format("%06d", nextSeq()));
        refund.setOrderId(order.getId());
        refund.setUserId(order.getUserId());
        refund.setAmount(order.getPayAmount());
        refund.setReason("取消订单自动全额退款");
        refund.setStatus("APPLYING");
        refund.setApplyChannel("USER_PAGE");
        refund.setApplyAt(LocalDateTime.now());
        refundMapper.insert(refund);
    }

    private Long nextSeq() {
        String date = LocalDate.now().format(DATE);
        String key = "seq:refund:" + date;
        Long seq = redis.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redis.expire(key, Duration.ofDays(2));
        }
        return seq;
    }
}
