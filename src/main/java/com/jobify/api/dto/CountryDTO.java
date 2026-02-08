package com.jobify.api.dto;

import lombok.Data;

@Data
public class CountryDTO {
    private Long id;
    private String name;
    private String iso2;
    private String iso3;
}
