package com.zmh.atlantic.coffee.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.AddCartRequest;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.CartItemView;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.CartView;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.UpdateCartRequest;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.product.Product;
import com.zmh.atlantic.coffee.product.ProductMapper;
import com.zmh.atlantic.coffee.product.SpecOption;
import com.zmh.atlantic.coffee.product.SpecOptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车：规格以 JSON 快照固化，同商品同规格合并数量（详细设计 v1.0 §0 补充⑥）；
 * 快照与差价在加入时校验规格字典后计算，下单时整体固化进订单明细。
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final SpecOptionMapper specOptionMapper;
    private final ObjectMapper objectMapper;

    public CartView add(Long userId, AddCartRequest request) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "数量至少为 1");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null || product.getStatus() != 1) {
            throw new BizException(ResultCode.NOT_FOUND, "商品不存在或已下架");
        }
        Map<String, String> specs = request.specs() == null ? Map.of() : request.specs();
        BigDecimal delta = BigDecimal.ZERO;
        for (var entry : specs.entrySet()) {
            SpecOption option = specOptionMapper.selectOne(new LambdaQueryWrapper<SpecOption>()
                    .eq(SpecOption::getSpecGroup, entry.getKey())
                    .eq(SpecOption::getOptionName, entry.getValue())
                    .eq(SpecOption::getStatus, 1));
            if (option == null) {
                throw new BizException(ResultCode.PARAM_INVALID,
                        "规格选项不存在：" + entry.getKey() + "=" + entry.getValue());
            }
            delta = delta.add(option.getPriceDelta());
        }
        String snapshot = toJson(specs);
        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, request.productId())
                .eq(CartItem::getSpecSnapshot, snapshot));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(request.productId());
            item.setSpecSnapshot(snapshot);
            item.setSpecPriceDelta(delta);
            item.setQuantity(request.quantity());
            cartItemMapper.insert(item);
        }
        return view(userId);
    }

    public CartView updateQuantity(Long userId, Long itemId, UpdateCartRequest request) {
        if (request.quantity() == null || request.quantity() < 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "数量至少为 1");
        }
        CartItem item = requireOwn(userId, itemId);
        item.setQuantity(request.quantity());
        cartItemMapper.updateById(item);
        return view(userId);
    }

    public CartView remove(Long userId, Long itemId) {
        cartItemMapper.deleteById(requireOwn(userId, itemId).getId());
        return view(userId);
    }

    public CartView view(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getId));
        Map<Long, Product> products = items.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(items.stream().map(CartItem::getProductId).distinct().toList())
                        .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemView> views = new java.util.ArrayList<>();
        for (CartItem item : items) {
            Product product = products.get(item.getProductId());
            BigDecimal unitPrice = (product == null ? BigDecimal.ZERO : product.getBasePrice())
                    .add(item.getSpecPriceDelta());
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            views.add(new CartItemView(item.getId(), item.getProductId(),
                    product != null ? product.getName() : "（已下架商品）",
                    item.getSpecSnapshot(), unitPrice, item.getQuantity()));
        }
        return new CartView(views, total);
    }

    private CartItem requireOwn(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "购物车项不存在");
        }
        return item;
    }

    private String toJson(Map<String, String> specs) {
        try {
            return objectMapper.writeValueAsString(specs);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.PARAM_INVALID, "规格格式不正确");
        }
    }
}
