package com.jobify.api.repository;

import com.jobify.api.dto.JobSearchCriteria;
import com.jobify.api.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> createSpecification(JobSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Optimization for N+1: Fetch ToOne relationships
            // Check if query is for counting to avoid fetching in count query
            // REMOVED: Eager fetching causes Double Join issues and is redundant
            // because JobService hydrates details via cache or separate bulk fetch.
            // if (query.getResultType() != Long.class && query.getResultType() !=
            // long.class) {
            // root.fetch("company", JoinType.LEFT);
            // root.fetch("jobDetail", JoinType.LEFT);
            // }

            // Join with Company
            if (criteria.getCompanyId() != null) {
                predicates.add(cb.equal(root.get("companyId"), criteria.getCompanyId()));
            }

            // Search in title
            if (StringUtils.hasText(criteria.getQ())) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + criteria.getQ().toLowerCase() + "%"));
            }

            // Filter by source (jobSource column)
            if (StringUtils.hasText(criteria.getSource())) {
                predicates.add(cb.equal(cb.lower(root.get("jobSource")), criteria.getSource().toLowerCase()));
            }

            // Locations filtering
            if (StringUtils.hasText(criteria.getCity()) || StringUtils.hasText(criteria.getRegion())
                    || StringUtils.hasText(criteria.getCountry()) || criteria.getRemote() != null) {
                Join<Job, JobLocation> locations = root.join("locations", JoinType.INNER);

                if (StringUtils.hasText(criteria.getCity())) {
                    Join<JobLocation, City> cityJoin = locations.join("city", JoinType.INNER);
                    predicates.add(cb.equal(cb.lower(cityJoin.get("name")), criteria.getCity().toLowerCase()));
                }

                if (StringUtils.hasText(criteria.getRegion())) {
                    Join<JobLocation, Region> regionJoin = locations.join("region", JoinType.INNER);
                    predicates.add(cb.equal(cb.lower(regionJoin.get("name")), criteria.getRegion().toLowerCase()));
                }

                if (StringUtils.hasText(criteria.getCountry())) {
                    Join<JobLocation, Country> countryJoin = locations.join("country", JoinType.INNER);
                    predicates.add(cb.equal(cb.lower(countryJoin.get("iso2")), criteria.getCountry().toLowerCase()));
                }

                if (criteria.getRemote() != null) {
                    predicates.add(cb.equal(locations.get("isRemote"), criteria.getRemote()));
                }
            }

            if (criteria.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), criteria.getIsActive()));
            }

            if (criteria.getSince() != null) {
                predicates.add(cb.greaterThan(root.get("createdAt"), criteria.getSince()));
            }

            if (criteria.getDescriptionTags() != null && !criteria.getDescriptionTags().isEmpty()) {
                List<Predicate> tagPredicates = new ArrayList<>();

                // Optimization: Reuse existing JOIN/FETCH if available to avoid double-joining
                Join<Job, JobDetail> jobDetailJoin = null;

                // Check existing fetches (from line 25)
                for (jakarta.persistence.criteria.Fetch<Job, ?> fetch : root.getFetches()) {
                    if (fetch.getAttribute().getName().equals("jobDetail")) {
                        @SuppressWarnings("unchecked")
                        Join<Job, JobDetail> castedJoin = (Join<Job, JobDetail>) fetch;
                        jobDetailJoin = castedJoin;
                        break;
                    }
                }

                // If no fetch found (e.g. count query or strictly filtering), create a join
                if (jobDetailJoin == null) {
                    for (Join<Job, ?> join : root.getJoins()) {
                        if (join.getAttribute().getName().equals("jobDetail")) {
                            @SuppressWarnings("unchecked")
                            Join<Job, JobDetail> castedJoin = (Join<Job, JobDetail>) join;
                            jobDetailJoin = castedJoin;
                            break;
                        }
                    }
                }

                if (jobDetailJoin == null) {
                    jobDetailJoin = root.join("jobDetail", JoinType.LEFT);
                }

                for (String tag : criteria.getDescriptionTags()) {
                    if (StringUtils.hasText(tag)) {
                        tagPredicates.add(
                                cb.like(cb.lower(jobDetailJoin.get("rawDescription")), "%" + tag.toLowerCase() + "%"));
                    }
                }
                if (!tagPredicates.isEmpty()) {
                    predicates.add(cb.or(tagPredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
