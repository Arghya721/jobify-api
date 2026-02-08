package com.jobify.api.model;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "job_locations")
@Data
@NoArgsConstructor
@Tag(name = "JobLocation", description = "JobLocation entity")
public class JobLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "is_remote")
    private Boolean isRemote;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "job_id", insertable = false, updatable = false)
    private Job job;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "city_id", insertable = false, updatable = false)
    private City city;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;
}
