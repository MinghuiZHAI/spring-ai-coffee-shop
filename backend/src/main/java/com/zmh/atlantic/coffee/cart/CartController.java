package com.zmh.atlantic.coffee.cart;

import com.zmh.atlantic.coffee.cart.dto.CartDtos.AddCartRequest;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.CartView;
import com.zmh.atlantic.coffee.cart.dto.CartDtos.UpdateCartRequest;
import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<CartView> view() {
        return Result.ok(cartService.view(UserContext.requireUserId()));
    }

    @PostMapping("/items")
    public Result<CartView> add(@Valid @RequestBody AddCartRequest request) {
        return Result.ok(cartService.add(UserContext.requireUserId(), request));
    }

    @PutMapping("/items/{id}")
    public Result<CartView> update(@PathVariable Long id, @Valid @RequestBody UpdateCartRequest request) {
        return Result.ok(cartService.updateQuantity(UserContext.requireUserId(), id, request));
    }

    @DeleteMapping("/items/{id}")
    public Result<CartView> remove(@PathVariable Long id) {
        return Result.ok(cartService.remove(UserContext.requireUserId(), id));
    }
}
