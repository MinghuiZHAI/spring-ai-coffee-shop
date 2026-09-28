package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.ClaimRequest;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.CouponView;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointItem;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointsView;
import com.zmh.atlantic.coffee.user.User;
import com.zmh.atlantic.coffee.user.UserMapper;
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

    private final UserMapper userMapper;
    private final PointRecordMapper pointRecordMapper;
    private final CouponService couponService;

    @GetMapping("/points")
    public Result<PointsView> points() {
        Long userId = UserContext.requireUserId();
        User user = userMapper.selectById(userId);
        List<PointItem> records = pointRecordMapper.selectList(new LambdaQueryWrapper<PointRecord>()
                        .eq(PointRecord::getUserId, userId).orderByDesc(PointRecord::getId).last("LIMIT 20"))
                .stream().map(r -> new PointItem(r.getCreatedAt(), r.getChangeValue(), r.getType(), r.getRelatedOrderId()))
                .toList();
        return Result.ok(new PointsView(user != null && user.getPoints() != null ? user.getPoints() : 0, records));
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
