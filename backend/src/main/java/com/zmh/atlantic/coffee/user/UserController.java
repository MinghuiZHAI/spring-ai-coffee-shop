package com.zmh.atlantic.coffee.user;

import com.zmh.atlantic.coffee.auth.UserContext;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.UserInfo;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.Result;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料（01 v1.3 追加 / 04 v1.6 §8，M1-6 批次 1）：
 * 身份一律取 UserContext——接口不接受任何 userId 入参，"修改他人资料"在
 * 接口层面不存在（越权防线闭合，普通用户/ADMIN 均只能操作本人资料）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/profile")
    public Result<UserInfo> profile() {
        return Result.ok(UserInfo.from(requireUser()));
    }

    /** 部分更新：null 字段不修改；gender 枚举校验（UNKNOWN/MALE/FEMALE）。 */
    @PutMapping("/profile")
    public Result<UserInfo> update(@Valid @RequestBody UpdateProfileRequest request) {
        User user = requireUser();
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getLuckyDay() != null) {
            user.setLuckyDay(request.getLuckyDay());
        }
        userMapper.updateById(user);
        return Result.ok(UserInfo.from(user));
    }

    private User requireUser() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /** 资料修改请求（白名单字段；phone/role/points 不可经此接口变更）。 */
    @Data
    public static class UpdateProfileRequest {

        @Size(max = 50, message = "昵称最长 50 字")
        private String nickname;

        @Size(max = 255, message = "头像 URL 最长 255 字")
        private String avatarUrl;

        @Pattern(regexp = "UNKNOWN|MALE|FEMALE", message = "gender 取值需为 UNKNOWN/MALE/FEMALE")
        private String gender;

        @Size(max = 20, message = "幸运日最长 20 字")
        private String luckyDay;
    }
}
