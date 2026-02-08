package com.jobify.api.repository;

import com.jobify.api.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    @Query("SELECT r FROM Region r WHERE r.country.iso2 = :iso2 ORDER BY r.name ASC")
    List<Region> findAllByCountryIso2OrderByNameAsc(@Param("iso2") String iso2);
}
