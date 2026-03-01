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
                jakarta.persistence.criteria.Expression<java.time.OffsetDateTime> createdAtExpr = root.get("createdAt");
                predicates.add(cb.greaterThan(createdAtExpr, criteria.getSince()));
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
                        if (cb instanceof org.hibernate.query.criteria.HibernateCriteriaBuilder) {
                            org.hibernate.query.criteria.HibernateCriteriaBuilder hcb = (org.hibernate.query.criteria.HibernateCriteriaBuilder) cb;
                            tagPredicates.add(hcb.ilike(jobDetailJoin.get("rawDescription"), "%" + tag + "%"));
                        } else {
                            tagPredicates.add(
                                    cb.like(cb.lower(jobDetailJoin.get("rawDescription")),
                                            "%" + tag.toLowerCase() + "%"));
                        }
                    }
                }

                // AND all the tags to match ALL! Match all narrow down the results, forcing
                // PostgreSQL to use the index!
                if (!tagPredicates.isEmpty()) {
                    predicates.add(cb.and(tagPredicates.toArray(new Predicate[0])));
                }
            }

            // Custom Sorting: COALESCE(jobDetail.jobPostedAt, root.createdAt)
            if (criteria.getSortDirection() != null && query.getResultType() != Long.class
                    && query.getResultType() != long.class) {
                Join<Job, JobDetail> sortJobDetailJoin = null;

                for (Join<Job, ?> join : root.getJoins()) {
                    if (join.getAttribute().getName().equals("jobDetail")) {
                        @SuppressWarnings("unchecked")
                        Join<Job, JobDetail> castedJoin = (Join<Job, JobDetail>) join;
                        sortJobDetailJoin = castedJoin;
                        break;
                    }
                }

                if (sortJobDetailJoin == null) {
                    sortJobDetailJoin = root.join("jobDetail", JoinType.LEFT);
                }

                jakarta.persistence.criteria.Expression<java.time.OffsetDateTime> postedAt = sortJobDetailJoin
                        .get("jobPostedAt");
                jakarta.persistence.criteria.Expression<java.time.OffsetDateTime> createdAt = root.get("createdAt");
                java.time.OffsetDateTime minDate = java.time.OffsetDateTime.parse("1970-01-01T00:00:00Z");
                jakarta.persistence.criteria.Expression<java.time.OffsetDateTime> effectiveDate = cb.<java.time.OffsetDateTime>selectCase()
                        .when(cb.isNull(postedAt), createdAt)
                        .when(cb.lessThan(postedAt, minDate), createdAt)
                        .otherwise(postedAt);

                // PERFORMANCE FIX: PostgreSQL planner trap "Limit + Order By + ILIKE"
                // This forces PG to evaluate the intensive Text Index Filter FIRST instead of a
                // full backward sequential scan
                // by making the sort key opaque to the planner.
                jakarta.persistence.criteria.Expression<java.time.OffsetDateTime> opaqueSortKey = cb.function(
                        "COALESCE", java.time.OffsetDateTime.class, effectiveDate,
                        cb.nullLiteral(java.time.OffsetDateTime.class));

                if (criteria.getSortDirection().isAscending()) {
                    query.orderBy(cb.asc(opaqueSortKey));
                } else {
                    query.orderBy(cb.desc(opaqueSortKey));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
