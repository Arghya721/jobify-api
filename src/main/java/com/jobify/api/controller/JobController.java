package com.jobify.api.controller;

import com.jobify.api.dto.JobResponse;
import com.jobify.api.dto.JobSearchCriteria;
import com.jobify.api.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Jobs", description = "Job management APIs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "Get jobs", description = "Retrieve a list of jobs with filtering and pagination")
    @GetMapping("/jobs")
    public org.springframework.http.ResponseEntity<JobResponse> getJobs(
            @RequestParam(required = false) String q,
            @RequestParam(name = "company_id", required = false) Long companyId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(name = "is_active", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
            @RequestParam(name = "description_tags", required = false) java.util.List<String> descriptionTags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "desc") String sort) { // 'sort' only controls direction now

        JobSearchCriteria criteria = new JobSearchCriteria();
        criteria.setQ(q);
        criteria.setCompanyId(companyId);
        criteria.setSource(source);
        criteria.setCountry(country);
        criteria.setCity(city);
        criteria.setRegion(region);
        criteria.setRemote(remote);
        criteria.setIsActive(isActive);
        criteria.setSince(since);
        criteria.setDescriptionTags(descriptionTags);

        // Limit max 100
        if (limit > 100) {
            limit = 100;
        }

        // Page is 1-based in API, but 0-based in Spring Data
        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Always sort by job_posted_at (aliased via JobDetail join)
        // This replaces the old "created_at" default.
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, "jobDetail.jobPostedAt"));

        return org.springframework.http.ResponseEntity.ok(jobService.getJobs(criteria, pageable));
    }

    @Operation(summary = "Get job by ID", description = "Retrieve a single job with details and locations")
    @GetMapping("/jobs/{id}")
    public org.springframework.http.ResponseEntity<com.jobify.api.dto.JobDTO> getJobById(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(jobService.getJobById(id));
    }
}
