package com.jobify.api.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingVelocityDTO {
    private long thisWeek;
    private long lastWeek;
    private long changePercent;
}
