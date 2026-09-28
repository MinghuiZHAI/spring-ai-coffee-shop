package com.zmh.atlantic.coffee.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** JWT 签发/校验单元测试（无 Spring 上下文）。 */
class JwtTokenServiceTest {

    private final JwtTokenService service = new JwtTokenService(
            new JwtProperties("test-secret-0123456789abcdef-32bytes!", Duration.ofMinutes(5), Duration.ofHours(1)));

    @Test
    void access_token_签发后可解析出身份与角色() {
        var issued = service.issueAccess(42L, "USER");
        var claims = service.parse(issued.token());
        assertAll(
                () -> assertEquals("42", claims.getSubject()),
                () -> assertEquals("USER", claims.get("role", String.class)),
                () -> assertEquals("access", claims.get("type", String.class)));
    }

    @Test
    void refresh_与access_type可区分() {
        var access = service.issueAccess(1L, "USER");
        var refresh = service.issueRefresh(1L, "USER");
        assertAll(
                () -> assertEquals("access", service.parse(access.token()).get("type", String.class)),
                () -> assertEquals("refresh", service.parse(refresh.token()).get("type", String.class)),
                () -> assertNotEquals(access.jti(), refresh.jti()));
    }

    @Test
    void 同一用户两次签发_jti不同() {
        var a = service.issueAccess(1L, "USER");
        var b = service.issueAccess(1L, "USER");
        assertNotEquals(a.jti(), b.jti());
    }

    @Test
    void 篡改的token_校验抛JwtException() {
        var issued = service.issueAccess(1L, "USER");
        assertThrows(JwtException.class, () -> service.parse(issued.token() + "x"));
    }

    @Test
    void 错误密钥签发的token_校验抛JwtException() {
        var other = new JwtTokenService(
                new JwtProperties("another-secret-0123456789abcdef-32bytes", Duration.ofMinutes(5), Duration.ofHours(1)));
        var forged = other.issueAccess(1L, "ADMIN");
        assertThrows(JwtException.class, () -> service.parse(forged.token()));
    }
}
