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

    /** 积分余额（V3 迁移补齐；point_record 为审计源）。 */
    private Integer points;

    /** 头像 URL（V4；空=默认波浪徽章）。 */
    private String avatarUrl;

    /** UNKNOWN/MALE/FEMALE（V4，应用层校验）。 */
    private String gender;

    /** 幸运日（V4，展示用自由文本）。 */
    private String luckyDay;

    /** 1 正常 0 禁用。 */
    private Integer status;

    @TableLogic
    private Integer deleted;
}
