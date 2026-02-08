package com.jobify.api.repository;

import com.jobify.api.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

        // Slice avoids the count query!
        // Renamed to queryBy to avoid conflict with JpaSpecificationExecutor.findAll
        Slice<Job> queryBy(Specification<Job> spec, Pageable pageable);

        @Query("SELECT j FROM Job j " +
                        "LEFT JOIN FETCH j.company " +
                        "LEFT JOIN FETCH j.jobDetail " +
                        "LEFT JOIN FETCH j.locations l " +
                        "LEFT JOIN FETCH l.city " +
                        "LEFT JOIN FETCH l.region " +
                        "LEFT JOIN FETCH l.country " +
                        "WHERE j.id = :id")
        Optional<Job> findByIdWithDetails(@Param("id") Long id);

        @Query("SELECT j FROM Job j " +
                        "LEFT JOIN FETCH j.company " +
                        "LEFT JOIN FETCH j.jobDetail " +
                        "LEFT JOIN FETCH j.locations l " +
                        "LEFT JOIN FETCH l.city " +
                        "LEFT JOIN FETCH l.region " +
                        "LEFT JOIN FETCH l.country " +
                        "WHERE j.id IN :ids")
        List<Job> findAllByIdWithDetails(@Param("ids") List<Long> ids);
}
