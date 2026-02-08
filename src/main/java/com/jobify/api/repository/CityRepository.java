package com.jobify.api.repository;

import com.jobify.api.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findAllByRegionIdOrderByNameAsc(Long regionId);
}
