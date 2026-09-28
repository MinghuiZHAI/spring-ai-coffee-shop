package com.zmh.atlantic.coffee.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal basePrice;

    private String imageUrl;

    /** 口味标签，逗号分隔（规则化推荐筛选用，技术栈 §12.2）。 */
    private String tags;

    /** 1 上架 0 下架。 */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
