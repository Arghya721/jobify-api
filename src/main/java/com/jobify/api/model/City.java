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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cities")
@Data
@NoArgsConstructor
@Tag(name = "City", description = "City entity")
public class City implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "country_id")
    private Long countryId;

    private BigDecimal lat;

    private BigDecimal lon;

    private Long population;

    private String name;

    @Column(name = "ascii_name")
    private String asciiName;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
