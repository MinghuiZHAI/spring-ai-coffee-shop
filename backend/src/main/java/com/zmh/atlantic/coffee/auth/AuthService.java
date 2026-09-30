package com.zmh.atlantic.coffee.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.LoginRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.RefreshRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.RegisterRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.TokenResponse;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.UserInfo;
import com.zmh.atlantic.coffee.common.exception.BizException;
import com.zmh.atlantic.coffee.common.web.ResultCode;
import com.zmh.atlantic.coffee.user.User;
import com.zmh.atlantic.coffee.user.UserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 注册/登录/刷新/登出（技术栈 #21）：
 * access 无状态；refresh 7d + Redis 白名单（auth:refresh:{userId}:{jti}）实现可吊销，
 * 刷新时旋转（旧 jti 删除、签发新对），登出即删白名单。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";

    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    public TokenResponse register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (exists > 0) {
            throw new BizException(ResultCode.PARAM_INVALID, "手机号已注册");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole("USER");
        user.setMemberLevel(1);
        user.setStatus(1);
        userMapper.insert(user);
        return issue(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.phone()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_INVALID, "手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已禁用");
        }
        return issue(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = parseRefresh(request.refreshToken());
        Long userId = Long.valueOf(claims.getSubject());
        String jti = claims.getId();
        String key = REFRESH_KEY_PREFIX + userId + ":" + jti;
        if (!Boolean.TRUE.equals(redis.hasKey(key))) {
            throw new BizException(ResultCode.TOKEN_INVALID, "refreshToken 已失效，请重新登录");
        }
        User user = requireActiveUser(userId);
        redis.delete(key);                       // 旋转：旧 refresh 作废
        return issue(user);
    }

    /** 幂等登出：token 无法解析也直接返回成功（白名单键不存在时本就无操作）。 */
    public void logout(String refreshToken) {
        try {
            Claims claims = parseRefresh(refreshToken);
            redis.delete(REFRESH_KEY_PREFIX + claims.getSubject() + ":" + claims.getId());
        } catch (BizException ignored) {
            // 已过期/已失效的 token：登出目标状态已达成
        }
    }

    private Claims parseRefresh(String refreshToken) {
        Claims claims = jwtTokenService.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(ResultCode.TOKEN_INVALID, "token 类型不正确");
        }
        return claims;
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号不可用");
        }
        return user;
    }

    private TokenResponse issue(User user) {
        var access = jwtTokenService.issueAccess(user.getId(), user.getRole());
        var refresh = jwtTokenService.issueRefresh(user.getId(), user.getRole());
        redis.opsForValue().set(REFRESH_KEY_PREFIX + user.getId() + ":" + refresh.jti(), "1",
                jwtProperties.refreshTtl());
        return new TokenResponse(access.token(), refresh.token(), jwtProperties.accessTtl().toSeconds(),
                UserInfo.from(user));
    }
}
