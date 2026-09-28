package com.zmh.atlantic.coffee.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.List;

/**
 * 安全配置（技术栈 #22）：三条路由规则对应 PRD 三角色；
 * /api/agent/** 为 M3 坐席接口占位，MVP 无路由挂载。
 * 401/403 以 JSON（code=40101/40301）返回，与统一响应体对齐——SSE 建立前的错误走标准 HTTP 状态码。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /** 未使用 UserDetailsService 体系（用户来自 user 表 + JWT claims），提供空实现避免 Boot 自动装配。 */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        return new JwtAuthenticationFilter(jwtTokenService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/agent/**").hasRole("AGENT")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "AGENT")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler(this::writeForbidden))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40101,\"message\":\"token 缺失或过期\",\"data\":null}");
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":40301,\"message\":\"无权限\",\"data\":null}");
    }
}
