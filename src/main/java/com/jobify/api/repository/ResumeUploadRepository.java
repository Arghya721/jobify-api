package com.jobify.api.repository;

import com.jobify.api.model.ResumeUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeUploadRepository extends JpaRepository<ResumeUpload, Long> {

    List<ResumeUpload> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    Optional<ResumeUpload> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(r) FROM ResumeUpload r WHERE r.user.id = :userId AND r.status IN ('pending', 'processing')")
    long countActiveByUserId(Long userId);
}
