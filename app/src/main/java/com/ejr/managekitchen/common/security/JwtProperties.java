package com.ejr.managekitchen.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration JWT (app.jwt.*). La clé doit faire au moins 32 caractères (HMAC-SHA256). */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
