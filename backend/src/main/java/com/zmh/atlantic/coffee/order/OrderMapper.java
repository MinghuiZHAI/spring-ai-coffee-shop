package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 模拟推进器/超时取消专用：条件批量 UPDATE，幂等且与 transit() 无共享写路径（详细设计 §2.4）。 */
    @Update("UPDATE orders SET status = #{to}, updated_at = NOW() " +
            "WHERE status = #{from} AND updated_at <= #{deadline}")
    int transitByStatus(@Param("from") String from, @Param("to") String to,
                        @Param("deadline") LocalDateTime deadline);

    /** 超时未支付自动取消（30 分钟，详细设计 v1.4 §2.4 补充）。 */
    @Update("UPDATE orders SET status = 'CANCELLED', cancel_reason = '超时未支付自动取消', updated_at = NOW() " +
            "WHERE status = 'PENDING_PAYMENT' AND created_at <= #{deadline}")
    int cancelExpired(@Param("deadline") LocalDateTime deadline);
}
