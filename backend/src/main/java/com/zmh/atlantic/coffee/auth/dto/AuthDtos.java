package com.zmh.atlantic.coffee.auth.dto;

import com.zmh.atlantic.coffee.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 认证相关请求/响应结构（总体设计 §5.2 认证域契约）。 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间") String password,
            @NotBlank(message = "昵称不能为空") String nickname) {
    }

    public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long accessExpiresIn, UserInfo userInfo) {
    }

    /**
     * 用户信息（登录/刷新响应与 GET /api/user/profile 共用，01 v1.3 追加字段口径）。
     * avatarUrl 空串 = 默认波浪徽章；gender UNKNOWN/MALE/FEMALE；luckyDay 展示用自由文本。
     */
    public record UserInfo(Long userId, String phone, String nickname, String role,
                           Integer memberLevel, String avatarUrl, String gender, String luckyDay) {

        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getPhone(), user.getNickname(), user.getRole(),
                    user.getMemberLevel(), user.getAvatarUrl(), user.getGender(), user.getLuckyDay());
        }
    }
}
