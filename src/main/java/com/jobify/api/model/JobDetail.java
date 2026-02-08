package com.jobify.api.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "job_details")
@Data
@NoArgsConstructor
@Tag(name = "JobDetail", description = "JobDetail entity")
public class JobDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "job_id")
    private Long jobId;

    @jakarta.persistence.OneToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.MapsId
    @jakarta.persistence.JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "experience_max")
    private Integer experienceMax;

    @Column(name = "experience_min")
    private Integer experienceMin;

    @Column(name = "job_posted_at")
    private OffsetDateTime jobPostedAt;

    @CreationTimestamp
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "raw_description", columnDefinition = "text")
    private String rawDescription;

    @Column(name = "experience_raw", columnDefinition = "text")
    private String experienceRaw;
}
