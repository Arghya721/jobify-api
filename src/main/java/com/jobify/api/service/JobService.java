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

    public JobService(JobRepository jobRepository, @Qualifier("caffeineCacheManager") CacheManager cacheManager) {
        this.jobRepository = jobRepository;
        this.cacheManager = cacheManager;
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
            // Cache Miss: Run DB Search using SLICE (No Count Query)
            // We use the Slice returning method logic (queryBy)
            org.springframework.data.domain.Slice<Job> jobSlice = jobRepository
                    .queryBy(JobSpecification.createSpecification(criteria), pageable);

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
            for (Long id : jobIds) {
                if (dtoMap.containsKey(id)) {
                    // Convert to Summary DTO (exclude details) for List View
                    JobDTO fullDto = dtoMap.get(id);
                    orderedDTOs.add(convertToSummaryDTO(fullDto));
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

    private JobSummaryDTO convertToSummaryDTO(JobDTO fullDto) {
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

        // Skip details and locations
        return summary;
    }

    private String generateSearchKey(JobSearchCriteria criteria, Pageable pageable) {
        String sortString = pageable.getSort().isSorted() ? pageable.getSort().toString().replace(": ", "")
                : "UNSORTED";
        return "search:" +
                criteria.hashCode() +
                "_p:" + pageable.getPageNumber() +
                "_s:" + pageable.getPageSize() +
                "_o:" + sortString;
    }
}
