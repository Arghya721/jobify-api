package com.jobify.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Maintains a Redis-backed blacklist of revoked JWT IDs (jti).
 *
 * When a session is revoked we write:
 *   key  = "jwt:blacklist:{jti}"
 *   value = "1"
 *   TTL  = remaining lifetime of the JWT
 *
 * The JwtAuthFilter checks this before accepting any token.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    /** Blacklist a jti for the given number of milliseconds. */
    public void blacklist(String jti, long ttlMs) {
        if (jti == null || ttlMs <= 0) return;
        redisTemplate.opsForValue().set(
                PREFIX + jti,
                "1",
                ttlMs,
                TimeUnit.MILLISECONDS
        );
    }

    /** Returns true if the jti is currently blacklisted. */
    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + jti));
    }
}
