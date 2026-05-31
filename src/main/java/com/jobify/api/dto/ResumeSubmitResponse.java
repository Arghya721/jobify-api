package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeSubmitResponse {

    @JsonProperty("upload_id")
    private Long uploadId;

    private String status;
}
