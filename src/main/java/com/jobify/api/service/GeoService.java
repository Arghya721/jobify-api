package com.jobify.api.service;

import com.jobify.api.dto.CityDTO;
import com.jobify.api.dto.CountryDTO;
import com.jobify.api.dto.RegionDTO;
import com.jobify.api.model.City;
import com.jobify.api.model.Country;
import com.jobify.api.model.Region;
import com.jobify.api.repository.CityRepository;
import com.jobify.api.repository.CountryRepository;
import com.jobify.api.repository.RegionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GeoService {

    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;

    public GeoService(CountryRepository countryRepository, RegionRepository regionRepository,
            CityRepository cityRepository) {
        this.countryRepository = countryRepository;
        this.regionRepository = regionRepository;
        this.cityRepository = cityRepository;
    }

    // Normal Caching (Redis) - Default CacheManager
    @Cacheable(value = "geo_countries")
    public List<CountryDTO> getCountries() {
        return countryRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Local LRU Caching (Caffeine)
    @Cacheable(value = "geo_regions", key = "#iso2", cacheManager = "caffeineCacheManager")
    public List<RegionDTO> getRegions(String iso2) {
        return regionRepository.findAllByCountryIso2OrderByNameAsc(iso2).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Local LRU Caching (Caffeine)
    @Cacheable(value = "geo_cities", key = "#regionId", cacheManager = "caffeineCacheManager")
    public List<CityDTO> getCities(Long regionId) {
        return cityRepository.findAllByRegionIdOrderByNameAsc(regionId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CountryDTO mapToDTO(Country country) {
        CountryDTO dto = new CountryDTO();
        dto.setId(country.getId()); // Assuming CountryDTO has ID now based on request
        dto.setName(country.getName());
        dto.setIso2(country.getIso2());
        dto.setIso3(country.getIso3());
        return dto;
    }

    private RegionDTO mapToDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getId()); // Assuming RegionDTO has ID
        dto.setName(region.getName());
        dto.setCode(region.getCode());
        return dto;
    }

    private CityDTO mapToDTO(City city) {
        CityDTO dto = new CityDTO();
        dto.setId(city.getId()); // Assuming CityDTO has ID
        dto.setName(city.getName());
        dto.setLatitude(city.getLat());
        dto.setLongitude(city.getLon());
        dto.setPopulation(city.getPopulation());
        return dto;
    }
}
