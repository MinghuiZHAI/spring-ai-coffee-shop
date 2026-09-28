package com.zmh.atlantic.coffee.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 规格选项字典：温度/糖度/冰量（详细设计 §1.2 spec_option）。 */
@Data
@TableName("spec_option")
public class SpecOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String specGroup;

    private String optionName;

    private BigDecimal priceDelta;

    private Integer sort;

    private Integer status;
}
