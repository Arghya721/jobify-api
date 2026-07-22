package com.jobify.api.service;

import com.jobify.api.model.User;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;

    /**
     * Marks the first-login feature tour as seen. Idempotent — the original
     * completion timestamp is kept if the user triggers this again.
     */
    @Transactional
    public void markCompleted(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        if (user.getOnboardingCompletedAt() == null) {
            user.setOnboardingCompletedAt(OffsetDateTime.now());
            userRepository.save(user);
        }
    }

    @Transactional(readOnly = true)
    public boolean isCompleted(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getOnboardingCompletedAt() != null)
                .orElse(false);
    }
}
