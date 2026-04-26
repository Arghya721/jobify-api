package com.jobify.api.repository;

import com.jobify.api.model.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {

    List<SavedFilter> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    Optional<SavedFilter> findByIdAndUserId(Long id, Long userId);
}
