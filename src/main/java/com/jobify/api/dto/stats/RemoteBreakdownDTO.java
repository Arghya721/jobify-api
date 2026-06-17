package com.jobify.api.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoteBreakdownDTO {
    private long remote;
    private long onsite;
    private long total;
}
