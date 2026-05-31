package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
public class ResumeUploadResponse {

    private Long id;

    @JsonProperty("file_name")
    private String fileName;

    private String status;

    @JsonProperty("jobs_query")
    private Map<String, Object> jobsQuery;

    private String error;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
