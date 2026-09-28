package com.zmh.atlantic.coffee.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户实体：全角色共用一张表（PRD 决策 #14），role 三值 USER/ADMIN/AGENT（AGENT 为 M3 预留）。
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    /** BCrypt 摘要，不对外输出。 */
    private String password;

    private String nickname;

    private String role;

    private Integer memberLevel;

    /** 1 正常 0 禁用。 */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
