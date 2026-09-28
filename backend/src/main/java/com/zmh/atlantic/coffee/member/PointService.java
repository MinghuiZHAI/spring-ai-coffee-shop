package com.zmh.atlantic.coffee.member;

import com.zmh.atlantic.coffee.user.User;
import com.zmh.atlantic.coffee.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
