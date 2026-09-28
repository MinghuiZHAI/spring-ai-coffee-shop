package com.zmh.atlantic.coffee.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** JWT 配置（application.yml atlantic.jwt.*）。 */
@ConfigurationProperties(prefix = "atlantic.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
}
