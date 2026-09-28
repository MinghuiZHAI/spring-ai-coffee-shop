package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointItem;
import com.zmh.atlantic.coffee.member.dto.MemberDtos.PointsView;
import com.zmh.atlantic.coffee.user.User;
import com.zmh.atlantic.coffee.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 积分服务：支付成功后发放（实付 1 元 = 10 分），余额与流水同事务维护。 */
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserMapper userMapper;
    private final PointRecordMapper pointRecordMapper;

    @Transactional
    public void earn(Long userId, int reward, Long orderId) {
        if (reward <= 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int balance = (user.getPoints() == null ? 0 : user.getPoints()) + reward;
        user.setPoints(balance);
        userMapper.updateById(user);
        PointRecord record = new PointRecord();
        record.setUserId(userId);
        record.setChangeValue(reward);
        record.setType("EARN");
        record.setRelatedOrderId(orderId);
        record.setBalanceAfter(balance);
        record.setRemark("下单获得");
        pointRecordMapper.insert(record);
    }

    /** 余额 + 近期流水（/api/user/points 与 AI 会员工具 queryPoints 共用同一口径）。 */
    public PointsView summary(Long userId) {
        User user = userMapper.selectById(userId);
        List<PointItem> records = pointRecordMapper.selectList(new LambdaQueryWrapper<PointRecord>()
                        .eq(PointRecord::getUserId, userId).orderByDesc(PointRecord::getId).last("LIMIT 20"))
                .stream().map(r -> new PointItem(r.getCreatedAt(), r.getChangeValue(), r.getType(), r.getRelatedOrderId()))
                .toList();
        return new PointsView(user != null && user.getPoints() != null ? user.getPoints() : 0, records);
    }
}
