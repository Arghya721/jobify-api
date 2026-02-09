package com.jobify.api.controller;

import com.jobify.api.dto.CityDTO;
import com.jobify.api.dto.CountryDTO;
import com.jobify.api.dto.RegionDTO;
import com.jobify.api.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/geo")
@Tag(name = "Geo Reference", description = "APIs for Location Reference Data")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

    @Operation(summary = "Get All Countries", description = "Returns a list of all countries sorted by name.")
    @GetMapping("/countries")
    public ResponseEntity<Map<String, List<CountryDTO>>> getCountries() {
        List<CountryDTO> countries = geoService.getCountries();
        Map<String, List<CountryDTO>> response = new HashMap<>();
        response.put("data", countries);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Regions by Country ISO2", description = "Returns regions/states for a given country ISO2 code.")
    @GetMapping("/countries/{iso2}/regions")
    public ResponseEntity<Map<String, List<RegionDTO>>> getRegions(@PathVariable String iso2) {
        List<RegionDTO> regions = geoService.getRegions(iso2);
        Map<String, List<RegionDTO>> response = new HashMap<>();
        response.put("data", regions);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Cities by Region ID", description = "Returns cities for a given region ID.")
    @GetMapping("/regions/{id}/cities")
    public ResponseEntity<Map<String, List<CityDTO>>> getCities(@PathVariable Long id) {
        List<CityDTO> cities = geoService.getCities(id);
        Map<String, List<CityDTO>> response = new HashMap<>();
        response.put("data", cities);
        return ResponseEntity.ok(response);
    }
}
