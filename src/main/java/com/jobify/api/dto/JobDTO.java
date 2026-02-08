package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDTO {
    private Long id;
    private String title;
    private CompanyDTO company;

    @JsonProperty("job_url")
    private String jobUrl;

    @JsonProperty("location_name")
    private String locationName;

    private JobDetailDTO details;

    private List<LocationDTO> locations;

    @JsonProperty("is_active")
    private Boolean isActive;
}
