package com.zmh.atlantic.coffee.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 购物车视图/请求结构（总体设计 §5.2 购物车域契约）。 */
public final class CartDtos {

    private CartDtos() {
    }

    /** specs 的 key 为规格组枚举名（TEMPERATURE/SWEETNESS/ICE），value 为选项名。 */
    public record AddCartRequest(Long productId, Map<String, String> specs, Integer quantity) {
    }

    public record UpdateCartRequest(Integer quantity) {
    }

    public record CartItemView(Long id, Long productId, String productName, String specSnapshot,
                               BigDecimal unitPrice, Integer quantity) {
    }

    public record CartView(List<CartItemView> items, BigDecimal totalAmount) {
    }
}
