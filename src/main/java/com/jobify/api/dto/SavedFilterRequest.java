package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SavedFilterRequest {

    /** Human-readable name for this filter preset, e.g. "Remote React Jobs" */
    private String name;

    /** The raw filter object — keys match the job-search query params */
    private Map<String, Object> filters;
}
