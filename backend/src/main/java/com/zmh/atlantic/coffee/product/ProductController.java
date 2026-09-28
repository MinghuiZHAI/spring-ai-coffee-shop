package com.zmh.atlantic.coffee.product;

import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.MenuResponse;
import com.zmh.atlantic.coffee.product.dto.ProductDtos.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ProductController {

    private final ProductQueryService productQueryService;

    @GetMapping("/menu")
    public Result<MenuResponse> menu() {
        return Result.ok(productQueryService.menu());
    }

    @GetMapping("/products/{id}")
    public Result<ProductDetail> detail(@PathVariable Long id) {
        return Result.ok(productQueryService.detail(id));
    }
}
