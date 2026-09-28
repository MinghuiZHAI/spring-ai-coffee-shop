package com.zmh.atlantic.coffee.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单明细：商品名与规格随下单固化（快照，不随菜单变更漂移）。 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private String productName;

    private String specSnapshot;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal lineAmount;

    private LocalDateTime createdAt;
}
