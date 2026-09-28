package com.zmh.atlantic.coffee.cart;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 购物车项：规格以快照字符串固化，同商品同规格由服务层合并数量。 */
@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    /** 规格快照 JSON：{"TEMPERATURE":"冰","SWEETNESS":"半糖"}。 */
    private String specSnapshot;

    /** 快照差价合计（加价规格之和，可为 0）。 */
    private BigDecimal specPriceDelta;

    private Integer quantity;
}
