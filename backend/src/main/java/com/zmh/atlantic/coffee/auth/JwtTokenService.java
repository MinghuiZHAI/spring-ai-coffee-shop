package com.zmh.atlantic.coffee.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 双 token 签发与校验（技术栈 #21）：
 * access 2h 无状态；refresh 7d 由 Redis 白名单（auth:refresh:{userId}:{jti}）实现可吊销，
 * 白名单的存取在 AuthService（Redis 读写属于认证流程，不属于加解密职责）。
 */
@Service
public class JwtTokenService {

    public record IssuedToken(String token, String jti, Instant expiresAt) {
    }

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(JwtProperties properties) {
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = properties.accessTtl();
        this.refreshTtl = properties.refreshTtl();
    }

    public IssuedToken issueAccess(Long userId, String role) {
        return issue(userId, role, "access", accessTtl);
    }

    public IssuedToken issueRefresh(Long userId, String role) {
        return issue(userId, role, "refresh", refreshTtl);
    }

    private IssuedToken issue(Long userId, String role, String type, Duration ttl) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", type)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
        return new IssuedToken(token, jti, now.plus(ttl));
    }

    /** 校验签名与有效期，失败抛 io.jsonwebtoken.JwtException。 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
