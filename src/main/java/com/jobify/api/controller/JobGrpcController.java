package com.jobify.api.controller;

import com.jobify.api.dto.*;
import com.jobify.api.grpc.*;
import com.jobify.api.service.JobService;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.OffsetDateTime;

@GrpcService
public class JobGrpcController extends JobServiceGrpc.JobServiceImplBase {

    private final JobService jobService;

    public JobGrpcController(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void getJobs(GetJobsRequest request, StreamObserver<GetJobsResponse> responseObserver) {
        try {
            JobSearchCriteria criteria = new JobSearchCriteria();
            if (request.hasQ())
                criteria.setQ(request.getQ());
            if (request.hasCompanyId())
                criteria.setCompanyId(request.getCompanyId());
            if (request.hasSource())
                criteria.setSource(request.getSource());
            if (request.hasCountry())
                criteria.setCountry(request.getCountry());
            if (request.hasCity())
                criteria.setCity(request.getCity());
            if (request.hasRegion())
                criteria.setRegion(request.getRegion());
            if (request.hasRemote())
                criteria.setRemote(request.getRemote());

            if (request.hasIsActive()) {
                criteria.setIsActive(request.getIsActive());
            } else {
                criteria.setIsActive(true); // Default matching REST API
            }

            if (request.hasSince())
                criteria.setSince(OffsetDateTime.parse(request.getSince()));
            if (request.getDescriptionTagsCount() > 0) {
                criteria.setDescriptionTags(request.getDescriptionTagsList());
            }
            if (request.hasExperienceMin())
                criteria.setExperienceMin(request.getExperienceMin());
            if (request.hasExperienceMax())
                criteria.setExperienceMax(request.getExperienceMax());

            int page = request.hasPage() ? request.getPage() : 1;
            int limit = request.hasLimit() ? request.getLimit() : 10;
            String sort = request.hasSort() ? request.getSort() : "desc";

            if (limit > 100)
                limit = 100;

            Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            criteria.setSortDirection(direction);

            // Use Sort.unsorted() because sorting is manually handled in JobSpecification
            // via CriteriaBuilder COALESCE
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.unsorted());

            JobResponse jobResponse = jobService.getJobs(criteria, pageable);

            GetJobsResponse.Builder responseBuilder = GetJobsResponse.newBuilder();
            if (jobResponse.getPagination() != null) {
                responseBuilder.setPagination(PaginationMessage.newBuilder()
                        .setPage(jobResponse.getPagination().getPage())
                        .setLimit(jobResponse.getPagination().getLimit())
                        .build());
            }

            if (jobResponse.getData() != null) {
                for (JobSummaryDTO summary : jobResponse.getData()) {
                    JobSummaryMessage.Builder summaryBuilder = JobSummaryMessage.newBuilder();

                    if (summary.getId() != null)
                        summaryBuilder.setId(summary.getId());
                    if (summary.getTitle() != null)
                        summaryBuilder.setTitle(summary.getTitle());

                    if (summary.getCompany() != null) {
                        CompanyMessage.Builder companyBuilder = CompanyMessage.newBuilder();
                        if (summary.getCompany().getId() != null)
                            companyBuilder.setId(summary.getCompany().getId());
                        if (summary.getCompany().getName() != null)
                            companyBuilder.setName(summary.getCompany().getName());
                        if (summary.getCompany().getSource() != null)
                            companyBuilder.setSource(summary.getCompany().getSource());
                        summaryBuilder.setCompany(companyBuilder.build());
                    }

                    if (summary.getJobUrl() != null)
                        summaryBuilder.setJobUrl(summary.getJobUrl());
                    if (summary.getLocationName() != null)
                        summaryBuilder.setLocationName(summary.getLocationName());
                    if (summary.getIsActive() != null)
                        summaryBuilder.setIsActive(summary.getIsActive());
                    if (summary.getCreatedAt() != null)
                        summaryBuilder.setCreatedAt(summary.getCreatedAt());
                    if (summary.getExperienceRaw() != null)
                        summaryBuilder.setExperienceRaw(summary.getExperienceRaw());
                    if (summary.getIsRemote() != null)
                        summaryBuilder.setIsRemote(summary.getIsRemote());

                    if (summary.getMatchedTags() != null && !summary.getMatchedTags().isEmpty()) {
                        summaryBuilder.addAllMatchedTags(summary.getMatchedTags());
                    }

                    responseBuilder.addData(summaryBuilder.build());
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getJobById(GetJobByIdRequest request, StreamObserver<JobMessage> responseObserver) {
        try {
            JobDTO jobDTO = jobService.getJobById(request.getId());

            JobMessage.Builder jobBuilder = JobMessage.newBuilder();
            if (jobDTO.getId() != null)
                jobBuilder.setId(jobDTO.getId());
            if (jobDTO.getTitle() != null)
                jobBuilder.setTitle(jobDTO.getTitle());

            if (jobDTO.getCompany() != null) {
                CompanyMessage.Builder companyBuilder = CompanyMessage.newBuilder();
                if (jobDTO.getCompany().getId() != null)
                    companyBuilder.setId(jobDTO.getCompany().getId());
                if (jobDTO.getCompany().getName() != null)
                    companyBuilder.setName(jobDTO.getCompany().getName());
                if (jobDTO.getCompany().getSource() != null)
                    companyBuilder.setSource(jobDTO.getCompany().getSource());
                jobBuilder.setCompany(companyBuilder.build());
            }

            if (jobDTO.getJobUrl() != null)
                jobBuilder.setJobUrl(jobDTO.getJobUrl());
            if (jobDTO.getLocationName() != null)
                jobBuilder.setLocationName(jobDTO.getLocationName());
            if (jobDTO.getIsActive() != null)
                jobBuilder.setIsActive(jobDTO.getIsActive());

            if (jobDTO.getDetails() != null) {
                JobDetailMessage.Builder detailBuilder = JobDetailMessage.newBuilder();
                if (jobDTO.getDetails().getRawDescription() != null) {
                    detailBuilder.setRawDescription(jobDTO.getDetails().getRawDescription());
                }
                if (jobDTO.getDetails().getExperienceMin() != null) {
                    detailBuilder.setExperienceMin(jobDTO.getDetails().getExperienceMin());
                }
                if (jobDTO.getDetails().getExperienceMax() != null) {
                    detailBuilder.setExperienceMax(jobDTO.getDetails().getExperienceMax());
                }
                if (jobDTO.getDetails().getExperienceRaw() != null) {
                    detailBuilder.setExperienceRaw(jobDTO.getDetails().getExperienceRaw());
                }
                if (jobDTO.getDetails().getDescriptionHtml() != null) {
                    detailBuilder.setDescriptionHtml(jobDTO.getDetails().getDescriptionHtml());
                }
                if (jobDTO.getDetails().getTags() != null) {
                    detailBuilder.addAllTags(jobDTO.getDetails().getTags());
                }
                if (jobDTO.getDetails().getJobPostedAt() != null
                        && jobDTO.getDetails().getJobPostedAt().getYear() > 1970) {
                    detailBuilder.setJobPostedAt(jobDTO.getDetails().getJobPostedAt().toString());
                } else if (jobDTO.getCreatedAt() != null && jobDTO.getCreatedAt().getYear() > 1970) {
                    detailBuilder.setJobPostedAt(jobDTO.getCreatedAt().toString());
                }
                jobBuilder.setDetails(detailBuilder.build());
            }

            if (jobDTO.getLocations() != null) {
                for (LocationDTO loc : jobDTO.getLocations()) {
                    LocationMessage.Builder locBuilder = LocationMessage.newBuilder();

                    if (loc.getCity() != null) {
                        CityMessage.Builder cityBuilder = CityMessage.newBuilder();
                        if (loc.getCity().getId() != null)
                            cityBuilder.setId(loc.getCity().getId());
                        if (loc.getCity().getName() != null)
                            cityBuilder.setName(loc.getCity().getName());
                        if (loc.getCity().getLatitude() != null)
                            cityBuilder.setLatitude(loc.getCity().getLatitude());
                        if (loc.getCity().getLongitude() != null)
                            cityBuilder.setLongitude(loc.getCity().getLongitude());
                        if (loc.getCity().getPopulation() != null)
                            cityBuilder.setPopulation(loc.getCity().getPopulation());
                        locBuilder.setCity(cityBuilder.build());
                    }
                    if (loc.getRegion() != null) {
                        RegionMessage.Builder regionBuilder = RegionMessage.newBuilder();
                        if (loc.getRegion().getId() != null)
                            regionBuilder.setId(loc.getRegion().getId());
                        if (loc.getRegion().getName() != null)
                            regionBuilder.setName(loc.getRegion().getName());
                        if (loc.getRegion().getCode() != null)
                            regionBuilder.setCode(loc.getRegion().getCode());
                        locBuilder.setRegion(regionBuilder.build());
                    }
                    if (loc.getCountry() != null) {
                        CountryMessage.Builder countryBuilder = CountryMessage.newBuilder();
                        if (loc.getCountry().getId() != null)
                            countryBuilder.setId(loc.getCountry().getId());
                        if (loc.getCountry().getName() != null)
                            countryBuilder.setName(loc.getCountry().getName());
                        if (loc.getCountry().getIso2() != null)
                            countryBuilder.setIso2(loc.getCountry().getIso2());
                        if (loc.getCountry().getIso3() != null)
                            countryBuilder.setIso3(loc.getCountry().getIso3());
                        locBuilder.setCountry(countryBuilder.build());
                    }
                    if (loc.getIsRemote() != null) {
                        locBuilder.setIsRemote(loc.getIsRemote());
                    }

                    jobBuilder.addLocations(locBuilder.build());
                }
            }

            responseObserver.onNext(jobBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
