package com.jobify.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobResponse {
    private List<JobSummaryDTO> data;
    private PaginationDTO pagination;
}
