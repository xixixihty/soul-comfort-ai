package com.hxq.soulcomfortai.util;

import com.hxq.soulcomfortai.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String DEFAULT_SECRET_WARNING = "changeme-please-use-a-strong-random-secret-key-at-least-32-chars";

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(JwtProperties jwtProperties) {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank() || DEFAULT_SECRET_WARNING.equals(secret)) {
            byte[] randomBytes = new byte[64];
            new SecureRandom().nextBytes(randomBytes);
            secret = Base64.getEncoder().encodeToString(randomBytes);
            log.warn("============================================================");
            log.warn("!!! JWT_SECRET 未设置，已生成随机密钥 !!!");
            log.warn("!!! 生产环境请务必通过环境变量 JWT_SECRET 设置固定密钥 !!!");
            log.warn("!!! 当前生成的密钥会在重启后失效（所有用户需重新登录）!!!");
            log.warn("============================================================");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.getExpiration();
    }

    public String generateToken(String userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return parseToken(token).getSubject();
    }
}