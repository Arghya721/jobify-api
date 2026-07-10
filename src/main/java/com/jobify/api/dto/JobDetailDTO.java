package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class JobDetailDTO {
    @JsonProperty("raw_description")
    private String rawDescription;

    @JsonProperty("description_html")
    private String descriptionHtml;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("experience_min")
    private Integer experienceMin;

    @JsonProperty("experience_max")
    private Integer experienceMax;

    @JsonProperty("experience_raw")
    private String experienceRaw;

    @JsonProperty("job_posted_at")
    private OffsetDateTime jobPostedAt;
}
