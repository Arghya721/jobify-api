package com.jobify.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "resume_uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "gcs_path", nullable = false)
    private String gcsPath;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jobs_query", columnDefinition = "jsonb")
    private Map<String, Object> jobsQuery;

    @Column
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
