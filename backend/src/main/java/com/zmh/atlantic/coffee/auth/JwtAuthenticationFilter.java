package com.zmh.atlantic.coffee.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer access token → 写入 SecurityContext 与 UserContext。
 * 无效/过期 token 按匿名继续（由授权规则决定 401），错误不在此处响应；
 * 请求结束 finally 清理上下文，避免线程复用串号。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    var claims = jwtTokenService.parse(header.substring(7));
                    if ("access".equals(claims.get("type", String.class))) {
                        long userId = Long.parseLong(claims.getSubject());
                        UserContext.set(userId);
                        var authentication = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (JwtException | IllegalArgumentException ignored) {
                    // 无效 token：保持匿名，由 SecurityFilterChain 的授权规则触发 401 入口点
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
