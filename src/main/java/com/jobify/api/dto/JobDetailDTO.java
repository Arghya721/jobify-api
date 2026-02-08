package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class JobDetailDTO {
    @JsonProperty("raw_description")
    private String rawDescription;

    @JsonProperty("experience_min")
    private Integer experienceMin;

    @JsonProperty("experience_max")
    private Integer experienceMax;

    @JsonProperty("experience_raw")
    private String experienceRaw;

    @JsonProperty("job_posted_at")
    private OffsetDateTime jobPostedAt;
}
