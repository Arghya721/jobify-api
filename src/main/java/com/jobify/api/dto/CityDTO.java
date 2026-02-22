package com.jobify.api.dto;

import lombok.Data;

@Data
public class CityDTO {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private Long population;
}
