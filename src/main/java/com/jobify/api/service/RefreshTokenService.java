package com.jobify.api.service;

import com.jobify.api.model.RefreshToken;
import com.jobify.api.model.User;
import com.jobify.api.repository.RefreshTokenRepository;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.auth.jwt-expiration-ms:900000}")
    private long jwtExpirationMs;

    // Refresh token active for 30 days
    private static final long REFRESH_EXPIRATION_DAYS = 30;

    // Redis key prefix for revoked refresh-token IDs
    private static final String REVOKED_RID_PREFIX = "jwt:revoked:rid:";

    @Transactional
    public RefreshToken createRefreshToken(Long userId, String deviceInfo, String ipAddress, String location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(REFRESH_EXPIRATION_DAYS, ChronoUnit.DAYS))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .location(location)
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }

        // Update last used time
        token.setLastUsedAt(Instant.now());
        return refreshTokenRepository.save(token);
    }

    public List<RefreshToken> getActiveSessions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return refreshTokenRepository.findByUser(user).stream()
                .filter(token -> token.getExpiryDate().compareTo(Instant.now()) >= 0)
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, Long tokenId) {
        RefreshToken token = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Ensure the token belongs to the requesting user
        if (!token.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to revoke this session");
        }

        // ── Immediate JWT invalidation ──────────────────────────────────────────
        // Any JWT carrying "rid": tokenId is now invalid. We mark the refresh
        // token ID as revoked in Redis for the full JWT lifetime so that even a
        // live access token is rejected by JwtAuthFilter within milliseconds.
        redisTemplate.opsForValue().set(
                REVOKED_RID_PREFIX + tokenId,
                "1",
                jwtExpirationMs,
                TimeUnit.MILLISECONDS
        );

        refreshTokenRepository.delete(token);
    }

    /**
     * Called from JwtAuthFilter to check if the refresh token that issued this
     * JWT has been revoked. Returns true if the session was revoked.
     */
    public boolean isSessionRevoked(Long refreshTokenId) {
        if (refreshTokenId == null) return false;
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(REVOKED_RID_PREFIX + refreshTokenId)
        );
    }
}
