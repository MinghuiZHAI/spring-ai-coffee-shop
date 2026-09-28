package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface RefundMapper extends BaseMapper<Refund> {

    /** 第一跳：申请中 → 已通过（T+30s）。表即队列，条件更新幂等（详细设计 §2.3）。 */
    @Update("UPDATE refund SET status = 'APPROVED', updated_at = NOW() " +
            "WHERE status = 'APPLYING' AND apply_at <= #{deadline}")
    int approveDue(@Param("deadline") LocalDateTime deadline);

    /** 第二跳：已通过 → 已到账（T+60s）。 */
    @Update("UPDATE refund SET status = 'SUCCESS', finished_at = NOW(), updated_at = NOW() " +
            "WHERE status = 'APPROVED' AND apply_at <= #{deadline}")
    int successDue(@Param("deadline") LocalDateTime deadline);

    /** 退款到账后同步订单终态（已完成订单的售后路径）。 */
    @Update("UPDATE orders o JOIN refund r ON r.order_id = o.id " +
            "SET o.status = 'REFUNDED', o.updated_at = NOW() " +
            "WHERE r.status = 'SUCCESS' AND o.status = 'REFUNDING'")
    int syncRefundedOrders();
}
