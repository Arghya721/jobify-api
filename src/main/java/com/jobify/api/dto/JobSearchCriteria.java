package com.jobify.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class JobSearchCriteria {
    private String q;
    private Long companyId;
    private String source;
    private String country;
    private String city;
    private String region;
    private Boolean remote;
    private Boolean isActive = true;
    private OffsetDateTime since;
    private java.util.List<String> descriptionTags;
}
