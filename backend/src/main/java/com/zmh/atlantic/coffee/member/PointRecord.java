package com.zmh.atlantic.coffee.member;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 积分流水（审计源；余额 = user.points，由 PointService 同步维护）。 */
@Data
@TableName("point_record")
public class PointRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 正 = 获得，负 = 抵扣。 */
    private Integer changeValue;

    private String type;

    private Long relatedOrderId;

    private Integer balanceAfter;

    private String remark;

    private LocalDateTime createdAt;
}
