package com.jobify.api.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@Tag(name = "Job", description = "Job entity")
public class Job implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "internal_job_id")
    private Long internalJobId;

    // metadata is jsonb in DB, keeping as String for now
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "source_job_id")
    private String sourceJobId;

    @Column(name = "job_url")
    private String jobUrl;

    @Column(name = "job_source")
    private String jobSource;

    @Column(name = "notified_at")
    private String notifiedAt; // Using String as per schema "text" type for now() default which is unusual for
                               // timestamp, but sticking to schema provided

    private String title;

    @Column(name = "location_name")
    private String locationName;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "company_id", insertable = false, updatable = false)
    private Company company;

    @jakarta.persistence.OneToOne(mappedBy = "job", fetch = jakarta.persistence.FetchType.LAZY)
    private JobDetail jobDetail;

    @jakarta.persistence.OneToMany(mappedBy = "job", fetch = jakarta.persistence.FetchType.LAZY)
    private java.util.List<JobLocation> locations;
}
