package com.zmh.atlantic.coffee.auth;

import com.zmh.atlantic.coffee.auth.dto.AuthDtos.LoginRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.RefreshRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.RegisterRequest;
import com.zmh.atlantic.coffee.auth.dto.AuthDtos.TokenResponse;
import com.zmh.atlantic.coffee.common.web.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口（公开，总体设计 §5.2 认证域）。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return Result.ok();
    }
}
