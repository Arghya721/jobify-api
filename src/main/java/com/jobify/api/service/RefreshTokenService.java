package com.jobify.api.service;

import com.jobify.api.model.RefreshToken;
import com.jobify.api.model.User;
import com.jobify.api.repository.RefreshTokenRepository;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    // Refresh token active for 30 days
    private static final long REFRESH_EXPIRATION_DAYS = 30;

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
        // Fetch tokens and filter out expired ones lazily or strictly
        return refreshTokenRepository.findByUser(user).stream()
                .filter(token -> token.getExpiryDate().compareTo(Instant.now()) >= 0)
                .toList();
    }

    @Transactional
    public void revokeSession(Long userId, Long tokenId) {
        RefreshToken token = refreshTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        // Ensure the token belongs to the requesting user before deleting
        if (!token.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to revoke this session");
        }
        
        refreshTokenRepository.delete(token);
    }
}
