package com.zmh.atlantic.coffee.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer access token → 写入 SecurityContext 与 UserContext。
 * 无效/过期 token 按匿名继续（由授权规则决定 401），错误不在此处响应；
 * 请求结束 finally 清理上下文，避免线程复用串号。
 *
 * <p>认证持久化（SSE 异步窗口 401 的修复）：Spring Security 6.5 的
 * SecurityContextHolderFilter 向 holder 装入"延迟 Supplier"，仅对 getContext()
 * 拿到的实例做 setAuthentication 变异，在 SseEmitter 异步窗口会被重解析丢失——
 * 这里按官方模式整体 setContext 并经 SecurityContextRepository.saveContext
 * 持久化，授权环节（含流结束后的 ASYNC dispatch）经同一 repository 读到本请求的认证。</p>
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final SecurityContextRepository securityContextRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   SecurityContextRepository securityContextRepository) {
        this.jwtTokenService = jwtTokenService;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * ASYNC dispatch 也要认证：SSE（SseEmitter）完成后的再分发默认会带着空 SecurityContext
     * 重跑授权规则，触发"向已提交响应写 401"的异常噪音；重新解析 token（头仍在）即可闭合。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (log.isDebugEnabled()) {
                log.debug("JWT filter in: dispatch={} uri={} hasHeader={}", request.getDispatcherType(),
                        request.getRequestURI(), header != null);
            }
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    var claims = jwtTokenService.parse(header.substring(7));
                    if ("access".equals(claims.get("type", String.class))) {
                        long userId = Long.parseLong(claims.getSubject());
                        UserContext.set(userId);
                        var authentication = new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                        securityContextRepository.saveContext(context, request, response);
                    }
                } catch (JwtException | IllegalArgumentException e) {
                    // 无效 token：保持匿名，由 SecurityFilterChain 的授权规则触发 401 入口点
                    log.warn("JWT 校验失败: {}", String.valueOf(e.getMessage()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
