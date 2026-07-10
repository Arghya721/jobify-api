package com.jobify.api.service;

import com.jobify.api.dto.*;
import com.jobify.api.model.*;
import com.jobify.api.repository.JobRepository;
import com.jobify.api.repository.JobSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final CacheManager cacheManager;
    private final DescriptionFormatterService descriptionFormatterService;

    public JobService(JobRepository jobRepository, @Qualifier("caffeineCacheManager") CacheManager cacheManager,
            DescriptionFormatterService descriptionFormatterService) {
        this.jobRepository = jobRepository;
        this.cacheManager = cacheManager;
        this.descriptionFormatterService = descriptionFormatterService;
    }

    @Transactional(readOnly = true)
    public JobResponse getJobs(JobSearchCriteria criteria, Pageable pageable) {
        // 1. Generate Cache Key (Include 'slice' to differentiate if needed, or reuse)
        String cacheKey = generateSearchKey(criteria, pageable);

        // 2. Check "job_search_ids" cache
        Cache searchCache = cacheManager.getCache("job_search_ids");
        CachedJobSearch cachedResult = searchCache != null ? searchCache.get(cacheKey, CachedJobSearch.class) : null;

        List<Long> jobIds;

        if (cachedResult == null) {
            // Cache Miss: Run DB Search using built-in findAll
            org.springframework.data.domain.Page<Job> jobSlice = jobRepository
                    .findAll(JobSpecification.createSpecification(criteria), pageable);

            jobIds = jobSlice.getContent().stream().map(Job::getId).collect(Collectors.toList());

            // Slice optimization: We don't calculate total elements or pages anymore.

            // Store IDs + Pagination in cache (Structure of CachedJobSearch needs update or
            // we store dummy values)
            if (searchCache != null) {
                CachedJobSearch newCacheEntry = new CachedJobSearch(jobIds);
                searchCache.put(cacheKey, newCacheEntry);
            }
        } else {
            // Cache Hit: Retrieve from cached object
            jobIds = cachedResult.getJobIds();
            // totalElements = cachedResult.getTotalElements(); // Unused
            // totalPages = cachedResult.getTotalPages(); // Unused
        }

        // 3. Hydrate Jobs from "jobs" cache or DB
        Cache jobsCache = cacheManager.getCache("jobs");
        List<JobDTO> finalJobDTOs = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        if (jobIds != null) {
            for (Long id : jobIds) {
                JobDTO cachedJob = jobsCache != null ? jobsCache.get(id, JobDTO.class) : null;
                if (cachedJob != null) {
                    finalJobDTOs.add(cachedJob);
                } else {
                    missingIds.add(id);
                }
            }
        }

        // 4. Bulk Fetch Missing
        if (!missingIds.isEmpty()) {
            List<Job> fetchedJobs = jobRepository.findAllByIdWithDetails(missingIds);
            for (Job job : fetchedJobs) {
                JobDTO dto = mapToDTO(job, true); // Full DTO
                if (jobsCache != null) {
                    jobsCache.put(job.getId(), dto);
                }
                finalJobDTOs.add(dto);
            }
        }

        // Sort explicitly to match the order of 'jobIds' because bulk fetch ordering is
        // not guaranteed
        Map<Long, JobDTO> dtoMap = finalJobDTOs.stream()
                .collect(Collectors.toMap(JobDTO::getId, dto -> dto, (a, b) -> a));
        List<JobSummaryDTO> orderedDTOs = new ArrayList<>();
        if (jobIds != null) {
            List<String> searchedTags = criteria.getDescriptionTags();
            for (Long id : jobIds) {
                if (dtoMap.containsKey(id)) {
                    // Convert to Summary DTO (exclude details) for List View
                    JobDTO fullDto = dtoMap.get(id);
                    orderedDTOs.add(convertToSummaryDTO(fullDto, searchedTags));
                }
            }
        }

        PaginationDTO paginationDTO = new PaginationDTO(
                pageable.getPageNumber() + 1,
                pageable.getPageSize());

        JobResponse response = new JobResponse();
        response.setData(orderedDTOs);
        response.setPagination(paginationDTO);

        return response;
    }

    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(value = "jobs", key = "#id", cacheManager = "caffeineCacheManager")
    public JobDTO getJobById(Long id) {
        Job job = jobRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        return mapToDTO(job, true); // true = include details and locations
    }

    private JobDTO mapToDTO(Job job, boolean includeDetails) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setJobUrl(job.getJobUrl());
        dto.setLocationName(job.getLocationName());
        dto.setIsActive(job.getIsActive());
        dto.setCreatedAt(job.getCreatedAt());

        if (job.getCompany() != null) {
            CompanyDTO companyDTO = new CompanyDTO();
            companyDTO.setId(job.getCompany().getId());
            companyDTO.setName(job.getCompany().getName());
            companyDTO.setSource(job.getCompany().getSource());
            dto.setCompany(companyDTO);
        }

        if (includeDetails) {
            if (job.getJobDetail() != null) {
                JobDetail detail = job.getJobDetail();
                JobDetailDTO detailDTO = new JobDetailDTO();
                detailDTO.setRawDescription(detail.getRawDescription());
                detailDTO.setExperienceMin(detail.getExperienceMin());
                detailDTO.setExperienceMax(detail.getExperienceMax());
                detailDTO.setExperienceRaw(detail.getExperienceRaw());
                detailDTO.setJobPostedAt(detail.getJobPostedAt());
                detailDTO.setDescriptionHtml(
                        descriptionFormatterService.toHtml(job.getJobSource(), job.getMetadata()));
                detailDTO.setTags(detail.getTags());
                dto.setDetails(detailDTO);
            }

            if (job.getLocations() != null) {
                List<LocationDTO> locationDTOs = job.getLocations().stream()
                        .map(this::mapLocationToDTO)
                        .collect(Collectors.toList());
                dto.setLocations(locationDTOs);
            } else {
                dto.setLocations(Collections.emptyList());
            }
        }

        return dto;
    }

    private LocationDTO mapLocationToDTO(JobLocation location) {
        LocationDTO dto = new LocationDTO();
        dto.setIsRemote(location.getIsRemote());

        if (location.getCity() != null) {
            CityDTO cityDTO = new CityDTO();
            cityDTO.setId(location.getCity().getId());
            cityDTO.setName(location.getCity().getName());
            cityDTO.setLatitude(location.getCity().getLat());
            cityDTO.setLongitude(location.getCity().getLon());
            cityDTO.setPopulation(location.getCity().getPopulation());
            dto.setCity(cityDTO);
        }

        if (location.getRegion() != null) {
            RegionDTO regionDTO = new RegionDTO();
            regionDTO.setId(location.getRegion().getId());
            regionDTO.setName(location.getRegion().getName());
            regionDTO.setCode(location.getRegion().getCode());
            dto.setRegion(regionDTO);
        }

        if (location.getCountry() != null) {
            CountryDTO countryDTO = new CountryDTO();
            countryDTO.setId(location.getCountry().getId());
            countryDTO.setName(location.getCountry().getName());
            countryDTO.setIso2(location.getCountry().getIso2());
            countryDTO.setIso3(location.getCountry().getIso3());
            dto.setCountry(countryDTO);
        }

        return dto;
    }

    private JobSummaryDTO convertToSummaryDTO(JobDTO fullDto, List<String> searchedTags) {
        JobSummaryDTO summary = new JobSummaryDTO();
        summary.setId(fullDto.getId());
        summary.setTitle(fullDto.getTitle());
        summary.setJobUrl(fullDto.getJobUrl());
        summary.setLocationName(fullDto.getLocationName());
        summary.setIsActive(fullDto.getIsActive());

        // Ensure Company is set properly
        if (fullDto.getCompany() != null) {
            CompanyDTO c = new CompanyDTO();
            c.setId(fullDto.getCompany().getId());
            c.setName(fullDto.getCompany().getName());
            c.setSource(fullDto.getCompany().getSource());
            summary.setCompany(c);
        }

        // Map the posted date and experience from job details
        // Fallback to createdAt if jobPostedAt is null
        boolean dateSet = false;
        if (fullDto.getDetails() != null) {
            if (fullDto.getDetails().getJobPostedAt() != null
                    && fullDto.getDetails().getJobPostedAt().getYear() > 1970) {
                summary.setCreatedAt(fullDto.getDetails().getJobPostedAt().toString());
                dateSet = true;
            }
            if (fullDto.getDetails().getExperienceRaw() != null) {
                summary.setExperienceRaw(fullDto.getDetails().getExperienceRaw());
            }
        }

        // If details was null or jobPostedAt was invalid, fallback to Job createdAt
        if (!dateSet && fullDto.getCreatedAt() != null && fullDto.getCreatedAt().getYear() > 1970) {
            summary.setCreatedAt(fullDto.getCreatedAt().toString());
        }

        // Derive is_remote from locations
        if (fullDto.getLocations() != null) {
            boolean isRemote = fullDto.getLocations().stream()
                    .anyMatch(loc -> Boolean.TRUE.equals(loc.getIsRemote()));
            summary.setIsRemote(isRemote);
        } else {
            summary.setIsRemote(false);
        }

        // Calculate matched tags
        if (searchedTags != null && !searchedTags.isEmpty()) {
            List<String> matchedTags = new ArrayList<>();
            String rawDescription = null;
            if (fullDto.getDetails() != null && fullDto.getDetails().getRawDescription() != null) {
                rawDescription = fullDto.getDetails().getRawDescription().toLowerCase();
            }
            if (rawDescription != null) {
                for (String tag : searchedTags) {
                    if (rawDescription.contains(tag.toLowerCase())) {
                        matchedTags.add(tag);
                    }
                }
            }
            if (!matchedTags.isEmpty()) {
                summary.setMatchedTags(matchedTags);
            }
        }

        return summary;
    }

    private String generateSearchKey(JobSearchCriteria criteria, Pageable pageable) {
        String sortString = pageable.getSort().isSorted() ? pageable.getSort().toString().replace(": ", "")
                : "UNSORTED";
        String tags = criteria.getDescriptionTags() != null
                ? String.join(",", criteria.getDescriptionTags())
                : "";
        return "search:" +
                "q:" + (criteria.getQ() != null ? criteria.getQ().toLowerCase().trim() : "") +
                "_co:" + criteria.getCompanyId() +
                "_src:" + criteria.getSource() +
                "_cy:" + criteria.getCity() +
                "_rg:" + criteria.getRegion() +
                "_ct:" + criteria.getCountry() +
                "_rm:" + criteria.getRemote() +
                "_ia:" + criteria.getIsActive() +
                "_si:" + criteria.getSince() +
                "_tg:" + tags +
                "_sd:" + criteria.getSortDirection() +
                "_xn:" + criteria.getExperienceMin() +
                "_xx:" + criteria.getExperienceMax() +
                "_p:" + pageable.getPageNumber() +
                "_s:" + pageable.getPageSize() +
                "_o:" + sortString;
    }
}
