package com.zmh.atlantic.coffee.store;

import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.store.dto.StoreDtos.StoreListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/stores")
    public Result<StoreListResponse> list(@RequestParam(required = false) String keyword) {
        return Result.ok(storeService.list(keyword));
    }
}
