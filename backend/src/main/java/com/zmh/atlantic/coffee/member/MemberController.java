package com.zmh.atlantic.coffee.member;

import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.ClaimRequest;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.CouponView;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointsView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MemberController {

    private final PointService pointService;
    private final CouponService couponService;

    @GetMapping("/points")
    public Result<PointsView> points() {
        return Result.ok(pointService.summary(UserContext.requireUserId()));
    }

    @GetMapping("/coupons")
    public Result<List<CouponView>> coupons(@RequestParam(required = false) String status) {
        return Result.ok(couponService.list(UserContext.requireUserId(), status));
    }

    @PostMapping("/coupons/claim")
    public Result<CouponView> claim(@Valid @RequestBody ClaimRequest request) {
        return Result.ok(couponService.claim(UserContext.requireUserId(), request));
    }
}
