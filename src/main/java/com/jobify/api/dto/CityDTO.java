package com.jobify.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CityDTO {
    private Long id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long population;
}
