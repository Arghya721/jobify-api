package com.jobify.api.service;

import com.jobify.api.dto.SavedFilterRequest;
import com.jobify.api.dto.SavedFilterResponse;
import com.jobify.api.model.SavedFilter;
import com.jobify.api.model.User;
import com.jobify.api.repository.SavedFilterRepository;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedFilterService {

    private final SavedFilterRepository savedFilterRepository;
    private final UserRepository userRepository;

    @Value("${app.max-saved-filters:3}")
    private int maxSavedFilters;

    // ─────────────────── List ───────────────────

    @Transactional(readOnly = true)
    public List<SavedFilterResponse> getFilters(String email) {
        User user = resolveUser(email);
        return savedFilterRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────── Create ───────────────────

    @Transactional
    public SavedFilterResponse createFilter(String email, SavedFilterRequest request) {
        User user = resolveUser(email);

        long current = savedFilterRepository.countByUserId(user.getId());
        if (current >= maxSavedFilters) {
            throw new RuntimeException(
                    "Maximum saved filter limit reached (" + maxSavedFilters + "). " +
                    "Please delete an existing filter before saving a new one."
            );
        }

        SavedFilter entity = SavedFilter.builder()
                .user(user)
                .name(request.getName())
                .filters(request.getFilters())
                .build();

        return toResponse(savedFilterRepository.save(entity));
    }

    // ─────────────────── Update ───────────────────

    @Transactional
    public SavedFilterResponse updateFilter(String email, Long filterId, SavedFilterRequest request) {
        User user = resolveUser(email);

        SavedFilter entity = savedFilterRepository
                .findByIdAndUserId(filterId, user.getId())
                .orElseThrow(() -> new RuntimeException("Saved filter not found or access denied."));

        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName());
        }
        if (request.getFilters() != null) {
            entity.setFilters(request.getFilters());
        }

        return toResponse(savedFilterRepository.save(entity));
    }

    // ─────────────────── Delete ───────────────────

    @Transactional
    public void deleteFilter(String email, Long filterId) {
        User user = resolveUser(email);

        SavedFilter entity = savedFilterRepository
                .findByIdAndUserId(filterId, user.getId())
                .orElseThrow(() -> new RuntimeException("Saved filter not found or access denied."));

        savedFilterRepository.delete(entity);
    }

    // ─────────────────── Helpers ───────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private SavedFilterResponse toResponse(SavedFilter entity) {
        return SavedFilterResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .filters(entity.getFilters())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
