package com.zmh.atlantic.coffee.order;

import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.CursorPage;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.ApplyRefundRequest;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.CancelOrderRequest;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.CreateOrderRequest;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderBrief;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderCreatedResponse;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.OrderDetail;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.PayResponse;
import com.zmh.atlantic.coffee.order.dto.OrderDtos.RefundView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单接口（总体设计 §5.2 订单域：创建/列表/详情/支付/取消/退款）。 */
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderCreatedResponse> create(@RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(UserContext.requireUserId(), request));
    }

    @GetMapping
    public Result<CursorPage<OrderBrief>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(orderService.list(UserContext.requireUserId(), status, cursor, limit));
    }

    @GetMapping("/{id}")
    public Result<OrderDetail> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(UserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/pay")
    public Result<PayResponse> pay(@PathVariable Long id) {
        return Result.ok(orderService.pay(UserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/pickup")
    public Result<PayResponse> pickup(@PathVariable Long id) {
        return Result.ok(orderService.pickup(UserContext.requireUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id,
                               @RequestBody(required = false) CancelOrderRequest request) {
        orderService.cancel(UserContext.requireUserId(), id, request == null ? null : request.reason());
        return Result.ok();
    }

    @PostMapping("/{id}/refunds")
    public Result<RefundView> applyRefund(@PathVariable Long id,
                                          @RequestBody(required = false) ApplyRefundRequest request) {
        return Result.ok(orderService.applyRefund(UserContext.requireUserId(), id,
                request == null ? null : request.reason()));
    }
}
