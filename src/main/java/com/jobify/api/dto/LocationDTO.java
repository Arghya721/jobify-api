package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationDTO {
    private CityDTO city;
    private RegionDTO region;
    private CountryDTO country;

    @JsonProperty("is_remote")
    private Boolean isRemote;
}
